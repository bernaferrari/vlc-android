@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.videolan.vlc.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.cinterop.interpretObjCPointerOrNull
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UITextField
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/**
 * iOS application lock backed by a device-only Keychain item.
 *
 * The PIN never crosses into shared persistence and the Keychain item cannot
 * migrate with an iCloud/unencrypted device backup.  Face ID / Touch ID is an
 * optional fast path; cancellation always falls back to the same PIN used on
 * Android so neither platform depends on a device passcode being configured.
 */
object IosAppLockController : AppLockController {
    private const val service = "org.videolan.vlc.app-lock"
    private const val account = "pin-v1"
    private const val biometricsPreference = "org.videolan.vlc.app-lock.biometrics-enabled"

    private val mutableState = MutableStateFlow(currentState(locked = hasCredential()))
    override val state: StateFlow<AppLockState> = mutableState.asStateFlow()

    private var hostProvider: (() -> UIViewController?)? = null

    /** Called by the Compose UIKit host after it exists; no global window lookup is used. */
    fun attachHost(provider: () -> UIViewController?) {
        hostProvider = provider
    }

    fun detachHost() {
        hostProvider = null
    }

    override suspend fun enable(): Boolean {
        val first = requestPin(
            title = "Create app lock",
            message = "Choose a 4-digit PIN for VLC.",
            action = "Continue",
        ) ?: return false
        val confirmation = requestPin(
            title = "Confirm PIN",
            message = "Enter the same 4-digit PIN again.",
            action = "Enable",
        ) ?: return false
        if (first != confirmation || !storeCredential(first)) return false
        mutableState.value = currentState(locked = false, enabled = true)
        return true
    }

    override suspend fun disable(): Boolean {
        if (!hasCredential() || !authenticate()) return false
        deleteCredential()
        mutableState.value = currentState(locked = false, enabled = false)
        return true
    }

    override suspend fun unlock(): Boolean {
        if (!hasCredential()) {
            mutableState.value = currentState(locked = false, enabled = false)
            return false
        }
        if (!authenticate()) return false
        mutableState.value = currentState(locked = false, enabled = true)
        return true
    }

    override suspend fun setBiometricsEnabled(enabled: Boolean): Boolean {
        if (!hasCredential() || !biometricsAvailable()) return false
        NSUserDefaults.standardUserDefaults.setBool(enabled, biometricsPreference)
        mutableState.value = currentState(locked = mutableState.value.locked, enabled = true)
        return true
    }

    override fun lock() {
        if (mutableState.value.enabled || hasCredential()) {
            mutableState.value = currentState(locked = true, enabled = true)
        }
    }

    private suspend fun authenticate(): Boolean {
        if (biometricsEnabled() && requestBiometricUnlock()) return true
        val pin = requestPin(
            title = "Unlock VLC",
            message = "Enter your 4-digit app lock PIN.",
            action = "Unlock",
        ) ?: return false
        return credentialMatches(pin)
    }

    private suspend fun requestBiometricUnlock(): Boolean = suspendCancellableCoroutine { continuation ->
        dispatch_async(dispatch_get_main_queue()) {
            val context = LAContext()
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)) {
                continuation.resume(false)
                return@dispatch_async
            }
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "Unlock your VLC library.",
            ) { success, _ ->
                if (continuation.isActive) continuation.resume(success)
            }
        }
    }

    private suspend fun requestPin(title: String, message: String, action: String): String? =
        suspendCancellableCoroutine { continuation ->
            dispatch_async(dispatch_get_main_queue()) {
                val host = topController(hostProvider?.invoke())
                if (host == null) {
                    continuation.resume(null)
                    return@dispatch_async
                }
                val alert = UIAlertController.alertControllerWithTitle(
                    title = title,
                    message = message,
                    preferredStyle = UIAlertControllerStyleAlert,
                )
                alert.addTextFieldWithConfigurationHandler { field ->
                    field?.apply {
                        secureTextEntry = true
                        keyboardType = UIKeyboardTypeNumberPad
                    }
                }
                alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel) {
                    if (continuation.isActive) continuation.resume(null)
                })
                alert.addAction(UIAlertAction.actionWithTitle(action, UIAlertActionStyleDefault) {
                    val pin = (alert.textFields?.firstOrNull() as? UITextField)?.text
                        ?.trim()
                        ?.takeIf(::isValidPin)
                    if (continuation.isActive) continuation.resume(pin)
                })
                host.presentViewController(alert, animated = true, completion = null)
            }
        }

    private fun topController(controller: UIViewController?): UIViewController? {
        var current = controller ?: return null
        while (current.presentedViewController != null) current = current.presentedViewController!!
        return current
    }

    private fun currentState(locked: Boolean, enabled: Boolean = hasCredential()) = AppLockState(
        supported = true,
        enabled = enabled,
        locked = enabled && locked,
        biometricsAvailable = biometricsAvailable(),
        biometricsEnabled = enabled && biometricsAvailable() && biometricsEnabled(),
    )

    private fun biometricsAvailable(): Boolean = LAContext().canEvaluatePolicy(
        LAPolicyDeviceOwnerAuthenticationWithBiometrics,
        error = null,
    )

    private fun biometricsEnabled(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey(biometricsPreference)

    private fun isValidPin(value: String): Boolean = value.length == 4 && value.all(Char::isDigit)

    private fun hasCredential(): Boolean =
        withSecurityQuery(query(returnData = false)) { SecItemCopyMatching(it, null) == errSecSuccess }

    private fun credentialMatches(pin: String): Boolean = readCredential()?.let { stored ->
        // Do not use an ordinary String equality for a secret comparison.
        val candidate = pin.encodeToByteArray()
        val matches = stored.size == candidate.size && stored.indices.fold(0) { difference, index ->
            difference or (stored[index].toInt() xor candidate[index].toInt())
        } == 0
        candidate.fill(0)
        stored.fill(0)
        matches
    } ?: false

    private fun readCredential(): ByteArray? = memScoped {
        val result = alloc<CFTypeRefVar>()
        if (withSecurityQuery(query(returnData = true)) { SecItemCopyMatching(it, result.ptr) } != errSecSuccess) {
            return null
        }
        val resultRef = result.value ?: return null
        try {
            val data = interpretObjCPointerOrNull<NSData>(resultRef.rawValue) ?: return null
            val length = data.length.toInt()
            if (length <= 0) return null
            ByteArray(length).also { bytes ->
                bytes.usePinned { pinned -> platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length) }
            }
        } finally {
            CFRelease(resultRef)
        }
    }

    private fun storeCredential(pin: String): Boolean {
        deleteCredential()
        val encoded = pin.encodeToByteArray()
        val data = encoded.usePinned { pinned -> NSData.create(pinned.addressOf(0), encoded.size.convert()) }
        encoded.fill(0)
        return withSecurityQuery(credentialQuery(data)) { SecItemAdd(it, null) == errSecSuccess }
    }

    private fun deleteCredential() {
        withSecurityQuery(query(returnData = false)) { SecItemDelete(it) }
        NSUserDefaults.standardUserDefaults.removeObjectForKey(biometricsPreference)
    }

    private fun query(returnData: Boolean): NSMutableDictionary = NSMutableDictionary().apply {
        putSecurityValue(kSecClass, securityString(kSecClassGenericPassword))
        putSecurityValue(kSecAttrService, service)
        putSecurityValue(kSecAttrAccount, account)
        if (returnData) {
            putSecurityValue(kSecReturnData, true)
            putSecurityValue(kSecMatchLimit, securityString(kSecMatchLimitOne))
        }
    }

    private fun credentialQuery(data: NSData): NSMutableDictionary = query(returnData = false).apply {
        putSecurityValue(kSecAttrAccessible, securityString(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly))
        putSecurityValue(kSecValueData, data)
    }

    private fun NSMutableDictionary.putSecurityValue(key: CFStringRef?, value: Any) {
        val nsKey = key?.rawValue?.let { interpretObjCPointerOrNull<NSString>(it) } ?: return
        setObject(value, forKey = nsKey)
    }

    private fun securityString(value: CFStringRef?): NSString =
        value?.rawValue?.let { interpretObjCPointerOrNull<NSString>(it) }
            ?: error("Missing Security framework constant")

    private inline fun <T> withSecurityQuery(
        query: NSMutableDictionary,
        block: (CFDictionaryRef) -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        val dictionary = (CFBridgingRetain(query) ?: error("Unable to bridge Keychain query")) as CFDictionaryRef
        return try {
            block(dictionary)
        } finally {
            CFRelease(dictionary)
        }
    }
}
