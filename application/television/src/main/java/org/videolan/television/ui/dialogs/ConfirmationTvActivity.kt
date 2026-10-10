package org.videolan.television.ui.dialogs

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.videolan.television.ui.browser.BaseTvActivity
import org.videolan.vlc.R
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvConfirmationScreen
import org.videolan.vlc.compose.tv.VLCTvTheme

/**
 * Generic TV yes/no confirmation. Callers pass a title and text; the result is
 * [ACTION_ID_POSITIVE] or [ACTION_ID_NEGATIVE]. Back cancels without a result.
 */
class ConfirmationTvActivity : BaseTvActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(CONFIRMATION_DIALOG_TITLE).orEmpty()
        val text = intent.getStringExtra(CONFIRMATION_DIALOG_TEXT).orEmpty()
        val destructive = intent.getBooleanExtra(CONFIRMATION_DIALOG_DESTRUCTIVE, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        VLCTvConfirmationScreen(
                            title = title,
                            message = text,
                            positiveLabel = getString(R.string.yes),
                            negativeLabel = getString(R.string.no),
                            onPositive = { finishWithResult(ACTION_ID_POSITIVE) },
                            onNegative = { finishWithResult(ACTION_ID_NEGATIVE) },
                            symbol = if (destructive) MaterialSymbols.Filled.Delete else MaterialSymbols.Filled.Info,
                            destructive = destructive,
                        )
                    }
                }
            }
        )
    }

    companion object {
        const val CONFIRMATION_DIALOG_TITLE = "confirmation_dialog_title"
        const val CONFIRMATION_DIALOG_TEXT = "confirmation_dialog_text"
        const val CONFIRMATION_DIALOG_DESTRUCTIVE = "confirmation_dialog_destructive"
        const val ACTION_ID_POSITIVE = 1
        const val ACTION_ID_NEGATIVE = ACTION_ID_POSITIVE + 1
    }

    override fun refresh() {}

    private fun finishWithResult(resultCode: Int) {
        setResult(resultCode)
        finish()
    }
}
