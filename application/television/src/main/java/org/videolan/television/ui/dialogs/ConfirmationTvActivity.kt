package org.videolan.television.ui.dialogs

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults

class ConfirmationTvActivity : BaseTvActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(CONFIRMATION_DIALOG_TITLE).orEmpty()
        val text = intent.getStringExtra(CONFIRMATION_DIALOG_TEXT).orEmpty()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        ConfirmationTvContent(
                            title = title,
                            text = text,
                            onPositive = { finishWithResult(ACTION_ID_POSITIVE) },
                            onNegative = { finishWithResult(ACTION_ID_NEGATIVE) }
                        )
                    }
                }
            }
        )
    }

    companion object {
        const val CONFIRMATION_DIALOG_TITLE = "confirmation_dialog_title"
        const val CONFIRMATION_DIALOG_TEXT = "confirmation_dialog_text"
        const val ACTION_ID_POSITIVE = 1
        const val ACTION_ID_NEGATIVE = ACTION_ID_POSITIVE + 1
    }

    override fun refresh() {}

    private fun finishWithResult(resultCode: Int) {
        setResult(resultCode)
        finish()
    }
}

@Composable
private fun ConfirmationTvContent(
    title: String,
    text: String,
    onPositive: () -> Unit,
    onNegative: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    val positiveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        positiveFocusRequester.requestFocus()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.onboardingBackground)
            .padding(horizontal = 64.dp, vertical = 48.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 720.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onPositive,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    modifier = Modifier.focusRequester(positiveFocusRequester)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.yes))
                }
                OutlinedButton(onClick = onNegative) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_small),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.no))
                }
            }
        }
    }
}
