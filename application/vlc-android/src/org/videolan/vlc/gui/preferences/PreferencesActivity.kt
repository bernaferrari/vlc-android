/*****************************************************************************
 * PreferencesActivity.kt
 *
 * Copyright © 2011-2026 VLC authors and VideoLAN
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
 */

package org.videolan.vlc.gui.preferences

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.videolan.tools.KEY_RESTRICT_SETTINGS
import org.videolan.tools.Settings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.compose.app.VlcSettingsScreen
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.PinCodeActivity
import org.videolan.vlc.gui.PinCodeReason
import org.videolan.vlc.gui.helpers.setVlcContent
import org.videolan.vlc.kmp.AndroidSettingsEffects
import org.videolan.vlc.kmp.EXTRA_SETTINGS_KEY
import org.videolan.vlc.kmp.VlcKmpInitializer
import org.videolan.vlc.kmp.openSettingsAt
import org.videolan.vlc.viewmodel.SettingsViewModel

/** Legacy extra: a preference key (or section id) to reveal. Prefer [EXTRA_SETTINGS_KEY]. */
const val EXTRA_PREF_END_POINT = "extra_pref_end_point"

/**
 * Standalone host for the shared settings pane, used by Android TV and external entry points.
 * Phones open settings inside the shared main shell instead (see [openSettingsAt]). All rows and
 * side effects come from the shared model and AndroidSettingsContributor; this activity only adds
 * the settings PIN gate and translates pending effects into the activity result callers expect.
 */
open class PreferencesActivity : BaseActivity() {

    override val displayTitle = false
    private lateinit var settingsViewModel: SettingsViewModel

    private val pinCodeResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) finish()
    }

    override fun getSnackAnchorView(overAudioPlayer: Boolean): View? = findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!VlcKmpInitializer.isInitialized) VlcKmpInitializer.initialize(applicationContext)
        if (savedInstanceState == null && Settings.getInstance(this).getBoolean(KEY_RESTRICT_SETTINGS, false)) {
            pinCodeResult.launch(PinCodeActivity.getIntent(this, PinCodeReason.CHECK))
        }
        settingsViewModel = VlcKoin.get().get()
        val initialKey = if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_SETTINGS_KEY) ?: intent.getStringExtra(EXTRA_PREF_END_POINT)
        } else null
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setVlcContent {
                    VlcSettingsScreen(vm = settingsViewModel, onExit = ::finish, initialKey = initialKey)
                }
            }
        )
    }

    override fun finish() {
        AndroidSettingsEffects.take()?.let { setResult(it.resultCode) }
        super.finish()
    }

    override fun onDestroy() {
        if (isFinishing) {
            settingsViewModel.onSettingsClosed()
            settingsViewModel.onCleared()
        }
        super.onDestroy()
    }

    companion object {
        /** Opens settings at the section showing [prefKey]. Kept for callers outside the settings code. */
        fun launchWithPref(activity: ComponentActivity, prefKey: String) = openSettingsAt(activity, prefKey)
    }
}
