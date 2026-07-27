package org.videolan.vlc.kmp

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.videolan.vlc.gui.AppLockPinActivity
import org.videolan.vlc.platform.AppLockController
import org.videolan.vlc.platform.AppLockState
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.coroutines.resume

/**
 * Android's secure, app-specific PIN vault.
 *
 * The PIN never reaches SharedPreferences. Its salted PBKDF2 verifier is encrypted with an
 * AES-GCM key in Android Keystore, so copied app data alone cannot be used for offline PIN
 * guessing. This is intentionally separate from legacy safe-mode's SHA-256 preference.
 */
internal class AndroidAppLockVault(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasCredential(): Boolean = preferences.contains(ENCRYPTED_VERIFIER)

    fun biometricsEnabled(): Boolean = preferences.getBoolean(BIOMETRICS_ENABLED, false)

    fun setBiometricsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(BIOMETRICS_ENABLED, enabled).commit()
    }

    fun store(pin: CharArray): Boolean = runCatching {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val verifier = derive(pin, salt)
        val plain = Base64.encodeToString(salt, Base64.NO_WRAP) + "." +
            Base64.encodeToString(verifier, Base64.NO_WRAP)
        preferences.edit().putString(ENCRYPTED_VERIFIER, encrypt(plain.toByteArray())).commit()
    }.getOrDefault(false)

    fun verify(pin: CharArray): Boolean = runCatching {
        val encrypted = preferences.getString(ENCRYPTED_VERIFIER, null) ?: return false
        val pieces = String(decrypt(encrypted)).split('.', limit = 2)
        if (pieces.size != 2) return false
        val salt = Base64.decode(pieces[0], Base64.NO_WRAP)
        val expected = Base64.decode(pieces[1], Base64.NO_WRAP)
        MessageDigest.isEqual(expected, derive(pin, salt))
    }.getOrDefault(false)

    fun clear() {
        preferences.edit().remove(ENCRYPTED_VERIFIER).remove(BIOMETRICS_ENABLED).commit()
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, DERIVED_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val payload = cipher.iv + cipher.doFinal(plain)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): ByteArray {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > GCM_IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_BYTES))
        }
        return cipher.doFinal(payload, GCM_IV_BYTES, payload.size - GCM_IV_BYTES)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val PREFERENCES = "vlc_app_lock_v1"
        const val ENCRYPTED_VERIFIER = "encrypted_verifier"
        const val BIOMETRICS_ENABLED = "biometrics_enabled"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vlc_app_lock_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val SALT_BYTES = 16
        const val PBKDF2_ITERATIONS = 210_000
        const val DERIVED_KEY_BITS = 256
    }
}

/** Activity-bound adapter for the common app-lock policy. */
class AndroidAppLockController(context: Context) : AppLockController {
    private val appContext = context.applicationContext
    private val vault = AndroidAppLockVault(context)
    private val mutableState = MutableStateFlow(
        currentState(enabled = vault.hasCredential(), locked = vault.hasCredential()),
    )
    override val state: StateFlow<AppLockState> = mutableState.asStateFlow()

    private var launcher: ActivityResultLauncher<Intent>? = null
    private var awaiting: ((Boolean) -> Unit)? = null
    private var attachedActivity: ComponentActivity? = null

    fun attach(activity: ComponentActivity) {
        if (attachedActivity === activity) return
        detach()
        attachedActivity = activity
        launcher = activity.activityResultRegistry.register(
            "vlc-app-lock-${System.identityHashCode(this)}",
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            awaiting?.invoke(result.resultCode == Activity.RESULT_OK)
            awaiting = null
        }
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = lock()
            override fun onDestroy(owner: LifecycleOwner) {
                detach()
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    fun detach() {
        launcher?.unregister()
        launcher = null
        attachedActivity = null
        awaiting?.invoke(false)
        awaiting = null
    }

    override suspend fun enable(): Boolean {
        if (vault.hasCredential()) return unlock()
        val configured = request(AppLockPinActivity.Mode.CREATE)
        if (configured && vault.hasCredential()) {
            mutableState.value = currentState(enabled = true, locked = false)
            return true
        }
        return false
    }

    override suspend fun disable(): Boolean {
        if (!vault.hasCredential()) {
            mutableState.value = currentState(enabled = false, locked = false)
            return true
        }
        if (!authenticate()) return false
        vault.clear()
        mutableState.value = currentState(enabled = false, locked = false)
        return true
    }

    override suspend fun unlock(): Boolean {
        if (!vault.hasCredential()) return false
        val unlocked = authenticate()
        if (unlocked) mutableState.value = currentState(enabled = true, locked = false)
        return unlocked
    }

    override suspend fun setBiometricsEnabled(enabled: Boolean): Boolean {
        if (!vault.hasCredential() || !biometricsAvailable()) return false
        vault.setBiometricsEnabled(enabled)
        mutableState.value = currentState(enabled = true, locked = mutableState.value.locked)
        return true
    }

    override fun lock() {
        if (vault.hasCredential()) {
            mutableState.value = currentState(enabled = true, locked = true)
        }
    }

    private suspend fun authenticate(): Boolean {
        if (mutableState.value.biometricsEnabled && requestBiometricUnlock()) return true
        return request(AppLockPinActivity.Mode.VERIFY)
    }

    private fun currentState(enabled: Boolean, locked: Boolean) = AppLockState(
        supported = true,
        enabled = enabled,
        locked = enabled && locked,
        biometricsAvailable = biometricsAvailable(),
        biometricsEnabled = enabled && biometricsAvailable() && vault.biometricsEnabled(),
    )

    private fun biometricsAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && biometricEnrollmentAvailable()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun biometricEnrollmentAvailable(): Boolean = appContext
        .getSystemService(BiometricManager::class.java)
        ?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun requestBiometricUnlock(): Boolean = suspendCancellableCoroutine { continuation ->
        val activity = attachedActivity
        if (activity == null || !biometricsAvailable()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val cancellation = CancellationSignal()
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Unlock VLC")
            .setDescription("Authenticate to open your VLC library.")
            .setNegativeButton("Use PIN", executor) { _, _ ->
                if (continuation.isActive) continuation.resume(false)
            }
            .build()
        continuation.invokeOnCancellation { cancellation.cancel() }
        prompt.authenticate(cancellation, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                if (continuation.isActive) continuation.resume(false)
            }
        })
    }

    private suspend fun request(mode: AppLockPinActivity.Mode): Boolean = suspendCancellableCoroutine { continuation ->
        val currentLauncher = launcher
        if (currentLauncher == null || awaiting != null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val activity = attachedActivity ?: run {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        awaiting = { result -> if (continuation.isActive) continuation.resume(result) }
        continuation.invokeOnCancellation { awaiting = null }
        currentLauncher.launch(AppLockPinActivity.intent(activity, mode))
    }
}
