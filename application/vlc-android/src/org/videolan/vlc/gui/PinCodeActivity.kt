/*
 * ************************************************************************
 *  PinCodeActivity.kt
 * *************************************************************************
 * Copyright © 2023 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.videolan.resources.AndroidDevices
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.resources.util.retrieveApplication
import org.videolan.tools.KEY_SAFE_MODE_PIN
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCPinCodeScreen
import org.videolan.vlc.gui.helpers.UiTools
import java.security.MessageDigest


private const val PIN_CODE_REASON = "pin_code_reason"

/**
 * Activity allowing to setup the safe mode.
 */
class PinCodeActivity : BaseActivity() {

    private lateinit var reason: PinCodeReason
    private lateinit var model: SafeModeModel

    private var reasonText by mutableStateOf("")
    private var pinCode by mutableStateOf("")
    private var pinStep by mutableStateOf(PinStep.ENTER_EXISTING)
    private var nextButtonText by mutableStateOf("")
    private var showSuccess by mutableStateOf(false)
    private var showCancel by mutableStateOf(true)

    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView
    override val displayTitle = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra(PIN_CODE_REASON)) throw IllegalStateException("No reason given")
        reason = PinCodeReason.entries.toTypedArray()[intent.getIntExtra(PIN_CODE_REASON, 0)]
        reasonText = getString(
            when (reason) {
                PinCodeReason.FIRST_CREATION -> R.string.pin_code_reason_create
                PinCodeReason.MODIFY -> R.string.pin_code_reason_modify
                else -> R.string.pin_code_reason_check
            }
        )
        nextButtonText = getString(R.string.next)
        setResult(RESULT_CANCELED)

        val getModel by viewModels<SafeModeModel> { SafeModeModel.Factory(this, reason) }
        model = getModel
        model.step.observe(this) { handleStep(it) }

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCPinCodeScreen(
                        reasonText = reasonText,
                        title = getString(pinStep.titleRes()),
                        pin = pinCode,
                        showPinEntry = !showSuccess,
                        showSuccess = showSuccess,
                        successText = getString(R.string.pin_unlock_success),
                        showVirtualKeyboard = Settings.tvUI && !showSuccess,
                        nextText = nextButtonText,
                        cancelText = getString(R.string.cancel),
                        deleteContentDescription = getString(R.string.delete),
                        nextEnabled = showSuccess || pinCode.length == PIN_LENGTH,
                        showCancel = showCancel,
                        onPinChange = ::onPinCodeChanged,
                        onDigit = ::appendDigit,
                        onBackspace = ::deleteLastDigit,
                        onNext = ::next,
                        onCancel = ::finish,
                        successIconContent = { PinCodeIcon(R.drawable.ic_pin_lock) },
                        backspaceIconContent = { PinCodeIcon(R.drawable.ic_backspace_white) }
                    )
                }
            }
        )

        if (AndroidDevices.isTv) applyOverscanMargin(this)
    }

    private fun handleStep(step: PinStep) {
        if (step == PinStep.EXIT) {
            finish()
            return
        }
        if (reason == PinCodeReason.CHECK && step !in arrayOf(PinStep.INVALID, PinStep.ENTER_EXISTING)) {
            setResult(RESULT_OK)
            finish()
            return
        }
        if (reason == PinCodeReason.UNLOCK && step !in arrayOf(PinStep.INVALID, PinStep.ENTER_EXISTING)) {
            setResult(RESULT_OK)
            showTips()
            return
        }

        pinStep = step
        if (model.isFinalStep()) nextButtonText = getString(R.string.done)
    }

    private fun showTips() {
        UiTools.setKeyboardVisibility(window.decorView, false)
        pinCode = ""
        showSuccess = true
        showCancel = false
        nextButtonText = getString(R.string.done)
    }

    private fun onPinCodeChanged(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(PIN_LENGTH)
        val shouldSubmit = sanitized.length == PIN_LENGTH && pinCode.length != PIN_LENGTH
        pinCode = sanitized
        if (shouldSubmit) next()
    }

    private fun appendDigit(digit: String) {
        if (showSuccess || pinCode.length >= PIN_LENGTH) return
        val updated = (pinCode + digit).take(PIN_LENGTH)
        pinCode = updated
        if (updated.length == PIN_LENGTH) next()
    }

    private fun deleteLastDigit() {
        if (pinCode.isNotEmpty()) pinCode = pinCode.dropLast(1)
    }

    /**
     * Use a keyboard / remote controller to type the PIN.
     *
     * @param keyCode
     * @param event
     * @return
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> {
                appendDigit("0")
                true
            }

            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> {
                appendDigit("1")
                true
            }

            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> {
                appendDigit("2")
                true
            }

            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> {
                appendDigit("3")
                true
            }

            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> {
                appendDigit("4")
                true
            }

            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> {
                appendDigit("5")
                true
            }

            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> {
                appendDigit("6")
                true
            }

            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> {
                appendDigit("7")
                true
            }

            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> {
                appendDigit("8")
                true
            }

            KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> {
                appendDigit("9")
                true
            }

            KeyEvent.KEYCODE_DEL -> {
                deleteLastDigit()
                true
            }

            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                next()
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * Move to next step.
     */
    private fun next() {
        val currentPin = pinCode
        if (model.step.value == PinStep.RE_ENTER || model.step.value == PinStep.NO_MATCH) {
            if (model.checkMatch(currentPin)) {
                model.savePin(currentPin)
                setResult(RESULT_OK)
                finish()
            } else {
                pinCode = ""
                return
            }
        }
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, "nextStep: $currentPin", Exception("Give me a stacktrace"))
        model.nextStep(currentPin)
        pinCode = ""
    }

    companion object {
        private const val PIN_LENGTH = 4

        fun getIntent(context: Context, reason: PinCodeReason) = Intent(context, PinCodeActivity::class.java).apply {
            putExtra(PIN_CODE_REASON, reason.ordinal)
        }
    }
}

private fun PinStep.titleRes() = when (this) {
    PinStep.ENTER_EXISTING -> R.string.safe_mode_pin
    PinStep.ENTER_NEW -> R.string.safe_mode_new_pin
    PinStep.RE_ENTER -> R.string.safe_mode_re_pin
    PinStep.NO_MATCH -> R.string.safe_mode_no_match
    PinStep.INVALID -> R.string.safe_mode_invalid_pin
    PinStep.LOGIN_SUCCESS -> R.string.safe_mode_invalid_pin
    PinStep.EXIT -> R.string.safe_mode_pin
}

@androidx.compose.runtime.Composable
private fun PinCodeIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}

class SafeModeModel(application: Application, val reason: PinCodeReason) : AndroidViewModel(application) {
    /**
     * Proceed to old pin verification and go to the next step.
     *
     * @param pin the entered pin
     */
    fun nextStep(pin: String) {
        //verify old pin
        if (step.value in arrayOf(PinStep.ENTER_EXISTING, PinStep.INVALID) && !checkValid(pin)) {
            step.postValue(PinStep.INVALID)
            return
        }
        if (reason == PinCodeReason.CHECK || reason == PinCodeReason.UNLOCK) {
            step.postValue(if (step.value == PinStep.LOGIN_SUCCESS) PinStep.EXIT else PinStep.LOGIN_SUCCESS)
            return
        }
        pins[step.value!!] = pin
        when (step.value!!) {
            PinStep.ENTER_EXISTING, PinStep.INVALID -> step.postValue(PinStep.ENTER_NEW)
            else -> step.postValue(PinStep.RE_ENTER)
        }
    }

    /**
     * Is i currently the final step.
     *
     * @return true if it is
     */
    fun isFinalStep(): Boolean {
        return step.value == PinStep.RE_ENTER
    }

    /**
     * Check if re-entered pin is the same as the entered one.
     *
     * @param pin the entered pin
     * @return true if pins match
     */
    fun checkMatch(pin: String): Boolean {
        val match = pins[PinStep.ENTER_NEW] == pin
        if (!match) step.postValue(PinStep.NO_MATCH)
        return match
    }

    /**
     * Check if the entered pin is valid.
     *
     * @param pin the entered pin
     * @return true if it's valid
     */
    private fun checkValid(pin: String) = getSha256(pin) == Settings.getInstance(getApplication()).getString(KEY_SAFE_MODE_PIN, "")

    /**
     * Save the new pin to the sharedpreferences. it's saved as a sha256 hash.
     *
     * @param pin the pin to save
     */
    fun savePin(pin: String) {
        Settings.getInstance(getApplication()).putSingle(KEY_SAFE_MODE_PIN, getSha256(pin))
    }

    /**
     * Get the sha256 hash of a string.
     *
     * @param input the input string
     * @return the sha256 hash
     */
    private fun getSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(input.toByteArray())
        return md.digest().joinToString(":") { String.format("%02x", it) }
    }

    val step = MutableLiveData(PinStep.ENTER_EXISTING)
    private val pins = HashMap<PinStep, String>()

    init {
        if (Settings.getInstance(application).getString(KEY_SAFE_MODE_PIN, "").isNullOrBlank()) {
            step.postValue(PinStep.ENTER_NEW)
        }
    }

    class Factory(private val context: Context, private val reason: PinCodeReason) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SafeModeModel(context.retrieveApplication().applicationContext as Application, reason) as T
        }
    }
}

enum class PinStep {
    ENTER_EXISTING, INVALID, ENTER_NEW, RE_ENTER, NO_MATCH, LOGIN_SUCCESS, EXIT;
}

enum class PinCodeReason {
    FIRST_CREATION, MODIFY, CHECK, UNLOCK
}
