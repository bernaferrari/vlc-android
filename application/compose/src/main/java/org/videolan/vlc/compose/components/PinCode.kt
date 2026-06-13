package org.videolan.vlc.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

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
    successIconContent: @Composable () -> Unit = { DefaultPinIconPlaceholder() },
    backspaceIconContent: @Composable () -> Unit = { DefaultPinIconPlaceholder() }
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (showSuccess) {
                    PinUnlockSuccess(
                        successText = successText,
                        iconContent = successIconContent,
                        modifier = Modifier.align(Alignment.Center)
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
                        onNext = onNext,
                        backspaceIconContent = backspaceIconContent,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                PinActions(
                    cancelText = cancelText,
                    nextText = nextText,
                    nextEnabled = nextEnabled,
                    showCancel = showCancel,
                    onCancel = onCancel,
                    onNext = onNext,
                    modifier = Modifier.align(Alignment.BottomCenter)
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
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HiddenPinInput(
            pin = pin,
            enabled = !showVirtualKeyboard,
            title = title,
            onPinChange = onPinChange,
            onNext = onNext
        )

        Text(
            text = reasonText,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            color = colors.fontDefault,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(48.dp))

        PinBoxes(pin = pin)

        if (showVirtualKeyboard) {
            Spacer(Modifier.height(24.dp))
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
private fun HiddenPinInput(
    pin: String,
    enabled: Boolean,
    title: String,
    onPinChange: (String) -> Unit,
    onNext: () -> Unit
) {
    if (!enabled) return

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = pin,
        onValueChange = { value ->
            onPinChange(value.filter { it.isDigit() }.take(4))
        },
        singleLine = true,
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
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
            .focusRequester(focusRequester)
            .semantics {
                contentDescription = title
                password()
            }
    )
}

@Composable
private fun PinBoxes(pin: String, modifier: Modifier = Modifier) {
    val colors = VLCThemeDefaults.colors

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 8.dp
        val tileSize = pinTileSize(maxWidth, spacing)
        val pinSemantics = "${pin.length} of 4 PIN digits entered"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = pinSemantics },
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val filled = pin.length > index
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .background(
                            color = if (filled) colors.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filled) "●" else "",
                        color = colors.primary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

private fun pinTileSize(maxWidth: Dp, spacing: Dp): Dp =
    ((maxWidth - spacing * 3f - 64.dp) / 4f)
        .coerceAtMost(64.dp)
        .coerceAtLeast(48.dp)

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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEachIndexed { rowIndex, rowDigits ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDigits.forEachIndexed { columnIndex, digit ->
                    val focusModifier = if (rowIndex == 0 && columnIndex == 0) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    }
                    PinKeyboardButton(
                        text = digit,
                        onClick = { onDigit(digit) },
                        modifier = focusModifier.size(48.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PinKeyboardButton(
                text = "0",
                onClick = { onDigit("0") },
                modifier = Modifier.size(48.dp)
            )
            PinKeyboardButton(
                text = "",
                contentDescription = deleteContentDescription,
                onClick = onBackspace,
                modifier = Modifier
                    .width(104.dp)
                    .height(48.dp),
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
    iconContent: (@Composable () -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val active = focused || pressed

    Surface(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (active) colors.primaryFocus else Color(0xCC000000),
        contentColor = Color.White,
        border = if (focused) BorderStroke(1.dp, colors.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (iconContent != null) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    iconContent()
                }
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
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
            .fillMaxWidth()
            .widthIn(max = 520.dp)
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

        TextButton(
            onClick = onNext,
            enabled = nextEnabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = VLCThemeDefaults.colors.primary
            )
        ) {
            Text(nextText)
        }
    }
}

@Composable
private fun DefaultPinIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(LocalContentColor.current.copy(alpha = 0.24f))
    )
}

@Preview(name = "PIN Phone Light", showBackground = true)
@Composable
private fun VLCPinCodeScreenPreview() {
    VLCPinCodeScreen(
        reasonText = "First, let's create your PIN code",
        title = "Enter your new PIN",
        pin = "12",
        showPinEntry = true,
        showSuccess = false,
        successText = "Successfully unlocked. To re-activate the safe mode, please tap this icon in the app:",
        showVirtualKeyboard = false,
        nextText = "Next",
        cancelText = "Cancel",
        deleteContentDescription = "Delete",
        nextEnabled = false,
        showCancel = true,
        onPinChange = {},
        onDigit = {},
        onBackspace = {},
        onNext = {},
        onCancel = {}
    )
}

@Preview(name = "PIN TV Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VLCPinCodeScreenTvPreview() {
    VLCPinCodeScreen(
        reasonText = "Access restricted. Please enter your PIN code",
        title = "Enter your PIN",
        pin = "123",
        showPinEntry = true,
        showSuccess = false,
        successText = "Successfully unlocked. To re-activate the safe mode, please tap this icon in the app:",
        showVirtualKeyboard = true,
        nextText = "Next",
        cancelText = "Cancel",
        deleteContentDescription = "Delete",
        nextEnabled = false,
        showCancel = true,
        onPinChange = {},
        onDigit = {},
        onBackspace = {},
        onNext = {},
        onCancel = {}
    )
}

@Preview(name = "PIN Success", showBackground = true)
@Composable
private fun VLCPinCodeScreenSuccessPreview() {
    VLCPinCodeScreen(
        reasonText = "",
        title = "",
        pin = "",
        showPinEntry = false,
        showSuccess = true,
        successText = "Successfully unlocked. To re-activate the safe mode, please tap this icon in the app:",
        showVirtualKeyboard = false,
        nextText = "Done",
        cancelText = "Cancel",
        deleteContentDescription = "Delete",
        nextEnabled = true,
        showCancel = false,
        onPinChange = {},
        onDigit = {},
        onBackspace = {},
        onNext = {},
        onCancel = {}
    )
}
