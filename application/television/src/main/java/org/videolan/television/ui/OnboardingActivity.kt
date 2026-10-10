/*
 * ************************************************************************
 *  OnboardingActivity.kt
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

package org.videolan.television.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.videolan.resources.TV_MAIN_ACTIVITY
import org.videolan.resources.util.canReadStorage
import org.videolan.television.R
import org.videolan.tools.KEY_TV_ONBOARDING_DONE
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.tv.VLCTvOnboardingPage
import org.videolan.vlc.compose.tv.VLCTvOnboardingScreen
import org.videolan.vlc.compose.tv.VLCTvTheme
import org.videolan.vlc.util.Permissions

class OnboardingActivity : ComponentActivity() {
    private val currentPage = mutableIntStateOf(0)
    private val hasStoragePermission = mutableStateOf(false)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasStoragePermission.value = canReadStorage(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage.intValue > 0) navigateToPage(currentPage.intValue - 1) else finish()
            }
        })
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTvTheme {
                        VLCTvOnboardingScreen(
                            pages = onboardingPages(hasStoragePermission.value),
                            currentPage = currentPage.intValue,
                            previousLabel = getString(R.string.previous),
                            nextLabel = getString(R.string.next),
                            finishLabel = getString(R.string.start_vlc),
                            onPrevious = { navigateToPage(currentPage.intValue - 1) },
                            onNext = { navigateToPage(currentPage.intValue + 1) },
                            onFinish = ::finishOnboarding
                        )
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        hasStoragePermission.value = canReadStorage(this)
    }

    private fun navigateToPage(page: Int) {
        currentPage.intValue = page.coerceIn(0, TV_ONBOARDING_LAST_PAGE)
        if (currentPage.intValue == 1 && !Permissions.canReadStorage(this)) {
            Permissions.checkReadStoragePermission(this)
        }
        hasStoragePermission.value = canReadStorage(this)
    }

    private fun onboardingPages(hasStoragePermission: Boolean) = listOf(
        VLCTvOnboardingPage(getString(R.string.welcome_title), getString(R.string.welcome_subtitle)),
        VLCTvOnboardingPage(getString(R.string.onboarding_scan_title), getString(R.string.permission_media), MaterialSymbols.Filled.Folder),
        VLCTvOnboardingPage(
            getString(R.string.onboarding_all_set),
            if (hasStoragePermission) getString(R.string.onboarding_permission_given)
            else "${getString(R.string.permission_expanation_no_allow)}\n${getString(R.string.permission_expanation_allow)}",
            if (hasStoragePermission) MaterialSymbols.Filled.CheckCircle else MaterialSymbols.Filled.Warning,
        ),
    )

    private fun finishOnboarding() {
        Settings.getInstance(this).putSingle(KEY_TV_ONBOARDING_DONE, true)
        finish()
        val intent = Intent(Intent.ACTION_VIEW).setClassName(this, TV_MAIN_ACTIVITY)
        startActivity(intent)
    }

    private companion object {
        const val TV_ONBOARDING_PAGE_COUNT = 3
        const val TV_ONBOARDING_LAST_PAGE = TV_ONBOARDING_PAGE_COUNT - 1
    }
}

