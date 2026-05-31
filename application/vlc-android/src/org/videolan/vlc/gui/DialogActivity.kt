/*
 * ***************************************************************************
 * DialogActivity.java
 * ***************************************************************************
 * Copyright © 2016 VLC authors and VideoLAN
 * Author: Geoffrey Métais
 *
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
 * ***************************************************************************
 */

package org.videolan.vlc.gui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.Dialog
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.util.parcelable
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.compose.components.VLCExternalDeviceDialogContent
import org.videolan.vlc.gui.dialogs.NetworkServerDialog
import org.videolan.vlc.gui.helpers.MedialibraryUtils
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.util.showVlcDialog

class DialogActivity : BaseActivity() {
    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? = findViewById<View>(android.R.id.content)
    private var preventFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transparent)
        val key = intent.action
        if (key.isNullOrEmpty()) {
            finish()
            return
        }
        when (key) {
            KEY_SERVER -> setupServerDialog()
            KEY_SUBS_DL -> setupSubsDialog()
            KEY_DEVICE -> setupDeviceDialog()
            KEY_DIALOG -> {
                dialog?.run {
                    showVlcDialog(this)
                    loginDialogShown.postValue(true)
                    dialog = null
                } ?: finish()
            }
            else -> finish()
        }
    }

    private fun setupDeviceDialog() {
        val intent = intent
        val path = intent.getStringExtra(EXTRA_PATH) ?: return finish()
        val scan = intent.getBooleanExtra(EXTRA_SCAN, false)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCExternalDeviceDialogContent(
                        title = getString(R.string.device_dialog_title),
                        message = getString(R.string.device_dialog_message),
                        browseText = getString(R.string.browse_folder),
                        scanText = getString(R.string.medialibrary_scan),
                        cancelText = getString(R.string.cancel),
                        showScan = scan,
                        onBrowse = { browseDevice(path) },
                        onScan = { scanDevice(path) },
                        onCancel = ::finish
                    )
                }
            }
        )
        lifecycleScope.launch {
            delay(30_000L)
            if (!isFinishing) finish()
        }
    }

    private fun browseDevice(path: String) {
        startActivity(
            Intent(applicationContext, StartActivity::class.java)
                .putExtra(org.videolan.resources.EXTRA_PATH, path)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun scanDevice(path: String) {
        MedialibraryUtils.addDevice(path, applicationContext)
        startActivity(Intent(applicationContext, StartActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun setupServerDialog() {
        val networkServerDialog = NetworkServerDialog()
        intent.parcelable<MediaWrapper>(EXTRA_MEDIA)?.let {
            networkServerDialog.setServer(it)
        }
        networkServerDialog.show(supportFragmentManager, "fragment_edit_network")
    }

    private fun setupSubsDialog() {
        val medialist = intent.parcelable<MediaWrapper>(EXTRA_MEDIA)
        if (medialist != null)
            MediaUtils.getSubs(this, medialist)
        else
            finish()
    }

    override fun finish() {
        loginDialogShown.postValue(false)
        if (preventFinish) {
            preventFinish = false
            return
        }
        super.finish()
    }

    fun preventFinish() {
        preventFinish = true
    }

    companion object {

        var dialog : Dialog? = null
        var loginDialogShown = MutableLiveData(false)
        const val KEY_SERVER = "serverDialog"
        const val KEY_SUBS_DL = "subsdlDialog"
        const val KEY_DEVICE = "deviceDialog"
        const val KEY_DIALOG = "vlcDialog"

        const val EXTRA_MEDIA = "extra_media"
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_UUID = "extra_uuid"
        const val EXTRA_SCAN = "extra_scan"
    }
}
