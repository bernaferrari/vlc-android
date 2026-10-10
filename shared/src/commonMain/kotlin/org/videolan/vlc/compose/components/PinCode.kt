package org.videolan.vlc.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.app.ShellStrings

/**
 * Full Compose replacement for:
 * - application/vlc-android/res/layout/pin_code_activity.xml
 *
 * PinCodeActivity keeps ownership of the SafeModeModel state machine,
 * result handling, hardware-key input, and TV overscan. This screen owns
 * only the rendered PIN boxes, phone keyboard target, TV keypad, and actions.
 */
@Composable
fun VLCPinCodeScreen(
    reasonText: String,
    title: String,
    pin: String,
    showPinEntry: Boolean,
    showSuccess: Boolean,
    successText: String,
    showVirtualKeyboard: Boolean,
    nextText: String,
    cancelText: String,
    deleteContentDescription: String,
    nextEnabled: Boolean,
    showCancel: Boolean,
    onPinChange: (String) -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    successIconContent: @Composable () -> Unit = { DefaultPinIcon(MaterialSymbols.Filled.CheckCircle) },
    backspaceIconContent: @Composable () -> Unit = { DefaultPinIcon(MaterialSymbols.Filled.Backspace) }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showSuccess) {
                    PinUnlockSuccess(
                        successText = successText,
                        iconContent = successIconContent,
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
                    )
                } else if (showPinEntry) {
                    PinEntryContent(
                        reasonText = reasonText,
                        title = title,
                        pin = pin,
                        showVirtualKeyboard = showVirtualKeyboard,
                        deleteContentDescription = deleteContentDescription,
                        onPinChange = onPinChange,
                        onDigit = onDigit,
                        onBackspace = onBackspace,
                        onNext = { if (nextEnabled) onNext() },
                        backspaceIconContent = backspaceIconContent,
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))
                PinActions(
                    cancelText = cancelText,
                    nextText = nextText,
                    nextEnabled = nextEnabled,
                    showCancel = showCancel,
                    onCancel = onCancel,
                    onNext = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PinEntryContent(
    reasonText: String,
    title: String,
    pin: String,
    showVirtualKeyboard: Boolean,
    deleteContentDescription: String,
    onPinChange: (String) -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit,
    backspaceIconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MaterialSymbols.Filled.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (reasonText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = reasonText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        if (showVirtualKeyboard) {
            PinBoxes(pin = pin)
        } else {
            PinInput(pin = pin, title = title, onPinChange = onPinChange, onNext = onNext)
        }

        if (showVirtualKeyboard) {
            Spacer(Modifier.height(32.dp))
            PinKeyboard(
                deleteContentDescription = deleteContentDescription,
                onDigit = onDigit,
                onBackspace = onBackspace,
                backspaceIconContent = backspaceIconContent
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PinInput(
    pin: String,
    title: String,
    onPinChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        interactionSource = interactionSource,
        value = pin,
        onValueChange = { value ->
            onPinChange(value.filter { it.isDigit() }.take(4))
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = if (pin.length == 4) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onDone = { onNext() },
            onNext = { onNext() }
        ),
        decorationBox = { innerTextField ->
            Box(Modifier.border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.large,
            ).padding(4.dp)) {
                Box(Modifier.size(1.dp).alpha(0.01f)) { innerTextField() }
                PinBoxes(pin = pin)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics {
                contentDescription = title
                password()
            }
    )
}

/** Four quiet dots: progress without revealing digits, and no boxes that read as text fields. */
@Composable
private fun PinBoxes(pin: String, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors
    val pinSemantics = ShellStrings.pinDigitsEntered(pin.length)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = pinSemantics },
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            val filled = pin.length > index
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .then(
                        if (filled) Modifier.background(colors.primary, CircleShape)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ),
            )
        }
    }
}

private val PinKeySize = 72.dp

@Composable
private fun PinKeyboard(
    deleteContentDescription: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    backspaceIconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstButtonFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEachIndexed { rowIndex, rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                rowDigits.forEachIndexed { columnIndex, digit ->
                    val focusModifier = if (rowIndex == 0 && columnIndex == 0) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    }
                    PinKeyboardButton(
                        text = digit,
                        onClick = { onDigit(digit) },
                        modifier = focusModifier.size(PinKeySize)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.size(PinKeySize))
            PinKeyboardButton(
                text = "0",
                onClick = { onDigit("0") },
                modifier = Modifier.size(PinKeySize)
            )
            PinKeyboardButton(
                text = "",
                contentDescription = deleteContentDescription,
                onClick = onBackspace,
                modifier = Modifier.size(PinKeySize),
                quiet = true,
                iconContent = backspaceIconContent
            )
        }
    }
}

@Composable
private fun PinKeyboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = text,
    /** Utility keys (delete) stay transparent so the digits remain the visual grid. */
    quiet: Boolean = false,
    iconContent: (@Composable () -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focusedState by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    // Focus is a D-pad/keyboard affordance; on touch screens a focused key would look pressed.
    val keyboardMode = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val focused = focusedState && keyboardMode
    val active = focused || pressed

    Surface(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        shape = CircleShape,
        color = when {
            active -> MaterialTheme.colorScheme.primaryContainer
            quiet -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = if (focused) BorderStroke(2.dp, colors.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (iconContent != null) {
                CompositionLocalProvider(LocalContentColor provides if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
                    iconContent()
                }
            } else {
                Text(
                    text = text,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal)
                )
            }
        }
    }
}

@Composable
private fun PinUnlockSuccess(
    successText: String,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VLCThemeDefaults.colors

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = successText,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.fontDefault) {
                iconContent()
            }
        }
    }
}

@Composable
private fun PinActions(
    cancelText: String,
    nextText: String,
    nextEnabled: Boolean,
    showCancel: Boolean,
    onCancel: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCancel) {
            TextButton(onClick = onCancel) {
                Text(cancelText)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = onNext, enabled = nextEnabled) {
            Text(nextText)
        }
    }
}

@Composable
private fun DefaultPinIcon(icon: MaterialIcon) = Icon(
    icon = icon,
    contentDescription = null,
)
