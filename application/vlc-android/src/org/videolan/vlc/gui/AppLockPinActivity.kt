package org.videolan.vlc.gui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCPinCodeScreen
import org.videolan.vlc.kmp.AndroidAppLockVault

/** Native credential entry for the common app-lock policy; the PIN UI itself is shared Compose. */
class AppLockPinActivity : BaseActivity() {
    private lateinit var mode: Mode
    private lateinit var vault: AndroidAppLockVault
    private var pin by mutableStateOf("")
    private var firstPin: String? by mutableStateOf(null)
    private var error by mutableStateOf(false)

    override val displayTitle = false
    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        mode = (intent.getSerializableExtra(MODE_EXTRA) as? Mode) ?: run {
            finish(); return
        }
        vault = AndroidAppLockVault(applicationContext)
        setResult(RESULT_CANCELED)
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val creating = mode == Mode.CREATE
                VLCPinCodeScreen(
                    reasonText = getString(
                        if (creating) R.string.pin_code_reason_create else R.string.pin_code_reason_check,
                    ),
                    title = getString(
                        if (error) R.string.app_lock_invalid_pin
                        else if (firstPin == null) R.string.app_lock_enter_pin
                        else R.string.app_lock_reenter_pin,
                    ),
                    pin = pin,
                    showPinEntry = true,
                    showSuccess = false,
                    successText = "",
                    showVirtualKeyboard = false,
                    nextText = getString(if (firstPin == null && creating) R.string.next else R.string.done),
                    cancelText = getString(R.string.cancel),
                    deleteContentDescription = getString(R.string.delete),
                    nextEnabled = pin.length == PIN_LENGTH,
                    showCancel = true,
                    onPinChange = { updatePin(it) },
                    onDigit = { digit -> updatePin(pin + digit) },
                    onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                    onNext = ::submit,
                    onCancel = ::finish,
                )
            }
        })
    }

    private fun updatePin(value: String) {
        val sanitized = value.filter(Char::isDigit).take(PIN_LENGTH)
        val submitNow = sanitized.length == PIN_LENGTH && pin.length != PIN_LENGTH
        pin = sanitized
        if (submitNow) submit()
    }

    private fun submit() {
        if (pin.length != PIN_LENGTH) return
        val creating = mode == Mode.CREATE
        val initial = firstPin
        if (creating && initial == null) {
            firstPin = pin
            pin = ""
            error = false
            return
        }
        val candidate = pin.toCharArray()
        lifecycleScope.launch {
            val accepted = withContext(Dispatchers.Default) {
                if (creating) initial == pin && vault.store(candidate) else vault.verify(candidate)
            }
            candidate.fill('\u0000')
            if (accepted) {
                setResult(RESULT_OK)
                finish()
            } else {
                error = true
                pin = ""
                if (creating) firstPin = null
            }
        }
    }

    enum class Mode { CREATE, VERIFY }

    companion object {
        private const val MODE_EXTRA = "app_lock_mode"
        private const val PIN_LENGTH = 4

        fun intent(context: Context, mode: Mode): Intent =
            Intent(context, AppLockPinActivity::class.java).putExtra(MODE_EXTRA, mode)
    }
}
