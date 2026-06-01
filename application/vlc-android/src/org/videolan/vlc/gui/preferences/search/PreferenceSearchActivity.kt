/*
 * ************************************************************************
 *  PreferenceSearchActivity.kt
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.preferences.search

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import org.videolan.resources.AppContextProvider
import org.videolan.resources.buildPkgString
import org.videolan.tools.LocaleUtils
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.preferences.EXTRA_PREF_END_POINT
import org.videolan.vlc.viewmodels.PreferenceSearchModel
import java.util.Locale

class PreferenceSearchActivity : BaseActivity() {
    private lateinit var viewmodel: PreferenceSearchModel

    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? = findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel = ViewModelProvider(this, PreferenceSearchModel.Factory(this))[PreferenceSearchModel::class.java]
        setContentView(
                ComposeView(this).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        PreferenceSearchRoute(
                                viewmodel = viewmodel,
                                showTranslationToggle = shouldShowTranslationToggle(),
                                onClose = ::finish,
                                onPreferenceClick = ::finishWithResult
                        )
                    }
                }
        )
    }

    private fun finishWithResult(item: PreferenceItem) {
        setResult(RESULT_OK, Intent(ACTION_RESULT).apply { putExtra(EXTRA_PREF_END_POINT, item) })
        finish()
    }

    private fun shouldShowTranslationToggle(): Boolean {
        val locale: Locale = AppContextProvider.locale
                ?.takeIf { it.isNotBlank() }
                ?.let { LocaleUtils.getLocaleFromString(it) }
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales[0] else resources.configuration.locale
        return locale.language != "en"
    }

    companion object {
        val ACTION_RESULT = "search.result".buildPkgString()
    }
}
