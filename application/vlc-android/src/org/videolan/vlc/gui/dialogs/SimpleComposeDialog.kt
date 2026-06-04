package org.videolan.vlc.gui.dialogs

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

fun ComponentActivity.showSimpleComposeDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val dialog = AppCompatDialog(this)
    dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VLCTheme {
                    val colors = VLCThemeDefaults.colors
                    Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 560.dp)
                                .padding(24.dp)
                        ) {
                            Text(text = title, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = message,
                                modifier = Modifier.padding(top = 16.dp),
                                color = colors.fontLight
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        dialog.dismiss()
                                    }
                                ) {
                                    Text(dismissText)
                                }
                                Button(
                                    onClick = {
                                        onConfirm()
                                        dialog.dismiss()
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(confirmText)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
    dialog.show()
}
