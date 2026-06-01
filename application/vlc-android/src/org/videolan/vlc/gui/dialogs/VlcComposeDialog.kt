package org.videolan.vlc.gui.dialogs

import androidx.appcompat.app.AppCompatDialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.videolan.libvlc.Dialog
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.tools.LOGIN_STORE
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.DialogActivity

internal interface VlcComposeDialogController {
    fun dismissFromVlc()
    fun updateProgress() = Unit
}

fun ComponentActivity.showVlcComposeDialog(vlcDialog: Dialog) {
    when (vlcDialog) {
        is Dialog.LoginDialog -> VlcLoginComposeDialog(this, vlcDialog).show()
        is Dialog.QuestionDialog -> VlcQuestionComposeDialog(this, vlcDialog).show()
        is Dialog.ProgressDialog -> VlcProgressComposeDialog(this, vlcDialog).show()
        else -> Unit
    }
}

private abstract class BaseVlcComposeDialog<T : Dialog>(
    protected val activity: ComponentActivity,
    protected val vlcDialog: T
) : VlcComposeDialogController {
    protected val dialog = AppCompatDialog(activity)
    private var dismissedFromVlc = false
    private var dismissHandled = false

    fun show() {
        vlcDialog.setContext(this)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setTitle(vlcDialog.title)
        dialog.setContentView(
            ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent { Content() }
            }
        )
        dialog.setOnDismissListener { handleDismiss() }
        dialog.show()
    }

    override fun dismissFromVlc() {
        dismissedFromVlc = true
        if (dialog.isShowing) dialog.dismiss() else handleDismiss()
    }

    protected fun dismissFromUi() {
        if (dialog.isShowing) dialog.dismiss() else handleDismiss()
    }

    private fun handleDismiss() {
        if (dismissHandled) return
        dismissHandled = true
        if (!dismissedFromVlc) vlcDialog.dismiss()
        (activity as? DialogActivity)?.finish()
    }

    @Composable
    protected abstract fun Content()
}

private class VlcLoginComposeDialog(
    activity: ComponentActivity,
    private val loginDialog: Dialog.LoginDialog
) : BaseVlcComposeDialog<Dialog.LoginDialog>(activity, loginDialog) {
    private val settings = Settings.getInstance(activity)

    @Composable
    override fun Content() {
        VLCTheme {
            val colors = VLCThemeDefaults.colors
            var login by remember { mutableStateOf(loginDialog.defaultUsername.orEmpty()) }
            var password by remember { mutableStateOf("") }
            var store by remember { mutableStateOf(settings.getBoolean(LOGIN_STORE, true)) }
            Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(text = loginDialog.text.orEmpty())
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text(activity.getString(R.string.login)) },
                        singleLine = true,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(activity.getString(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    )
                    if (loginDialog.asksStore()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Checkbox(checked = store, onCheckedChange = { store = it })
                            Text(activity.getString(R.string.store_password))
                        }
                        if (!AndroidUtil.isMarshMallowOrLater) {
                            Text(
                                text = activity.getString(R.string.encryption_warning),
                                color = colors.fontLight,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        TextButton(onClick = ::dismissFromUi) {
                            Text(activity.getString(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                loginDialog.postLogin(login.trim(), password.trim(), store)
                                settings.putSingle(LOGIN_STORE, store)
                                dismissFromUi()
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(activity.getString(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

private class VlcQuestionComposeDialog(
    activity: ComponentActivity,
    private val questionDialog: Dialog.QuestionDialog
) : BaseVlcComposeDialog<Dialog.QuestionDialog>(activity, questionDialog) {
    @Composable
    override fun Content() {
        VLCTheme {
            val colors = VLCThemeDefaults.colors
            Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(text = questionDialog.text.orEmpty())
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        TextButton(onClick = ::dismissFromUi) {
                            Text(questionDialog.cancelText?.takeIf { it.isNotEmpty() } ?: activity.getString(R.string.cancel))
                        }
                        questionDialog.action2Text?.takeIf { it.isNotEmpty() }?.let { action ->
                            TextButton(
                                onClick = {
                                    questionDialog.postAction(2)
                                    dismissFromUi()
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(action)
                            }
                        }
                        questionDialog.action1Text?.takeIf { it.isNotEmpty() }?.let { action ->
                            Button(
                                onClick = {
                                    questionDialog.postAction(1)
                                    dismissFromUi()
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(action)
                            }
                        }
                    }
                }
            }
        }
    }
}

private class VlcProgressComposeDialog(
    activity: ComponentActivity,
    private val progressDialog: Dialog.ProgressDialog
) : BaseVlcComposeDialog<Dialog.ProgressDialog>(activity, progressDialog) {
    private val progress = mutableStateOf(progressDialog.position)
    private val cancelText = mutableStateOf(progressDialog.cancelText.orEmpty())
    private val indeterminate = mutableStateOf(progressDialog.isIndeterminate)

    override fun updateProgress() {
        progress.value = progressDialog.position
        cancelText.value = progressDialog.cancelText.orEmpty()
        indeterminate.value = progressDialog.isIndeterminate
    }

    @Composable
    override fun Content() {
        VLCTheme {
            val colors = VLCThemeDefaults.colors
            Surface(color = colors.backgroundDefault, contentColor = colors.fontDefault) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(text = progressDialog.text.orEmpty())
                    if (indeterminate.value) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.value.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                    cancelText.value.takeIf { it.isNotEmpty() }?.let { text ->
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            TextButton(onClick = ::dismissFromUi) {
                                Text(text)
                            }
                        }
                    }
                }
            }
        }
    }
}
