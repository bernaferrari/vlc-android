package org.videolan.vlc.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import org.videolan.libvlc.Dialog
import org.videolan.vlc.gui.dialogs.VlcComposeDialogController
import org.videolan.vlc.gui.dialogs.showVlcComposeDialog
import videolan.org.commontools.LiveEvent

private const val TAG = "DialogDelegate"

interface IDialogDelegate {
    fun observeDialogs(lco: LifecycleOwner, manager: IDialogManager)
}

interface IDialogManager {
    fun fireDialog(dialog: Dialog)
    fun dialogCanceled(dialog: Dialog?)
}

class DialogDelegate : IDialogDelegate {

    override fun observeDialogs(lco: LifecycleOwner, manager: IDialogManager) {
        dialogEvt.observe(lco) {
            when (it) {
                is Show -> manager.fireDialog(it.dialog)
                is Cancel -> manager.dialogCanceled(it.dialog)
            }
        }
    }

    companion object DialogsListener : Dialog.Callbacks {
        private val dialogEvt: LiveEvent<DialogEvt> = LiveEvent()
        var dialogCounter = 0

        override fun onProgressUpdate(dialog: Dialog.ProgressDialog) {
            (dialog.context as? VlcComposeDialogController)?.updateProgress()
        }

        override fun onDisplay(dialog: Dialog.ErrorMessage) {
            dialogEvt.value = Cancel(dialog)
        }

        override fun onDisplay(dialog: Dialog.LoginDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onDisplay(dialog: Dialog.QuestionDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onDisplay(dialog: Dialog.ProgressDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onCanceled(dialog: Dialog?) {
            (dialog?.context as? VlcComposeDialogController)?.dismissFromVlc()
            dialogEvt.value = Cancel(dialog)
        }
    }
}

@Suppress("INACCESSIBLE_TYPE")
fun ComponentActivity.showVlcDialog(dialog: Dialog) {
    DialogDelegate.dialogCounter++
    showVlcComposeDialog(dialog)
}

private sealed class DialogEvt
private class Show(val dialog: Dialog) : DialogEvt()
private class Cancel(val dialog: Dialog?) : DialogEvt()
