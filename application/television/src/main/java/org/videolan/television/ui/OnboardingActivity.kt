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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.resources.TV_MAIN_ACTIVITY
import org.videolan.resources.util.canReadStorage
import org.videolan.television.R
import org.videolan.tools.KEY_TV_ONBOARDING_DONE
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
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
                    VLCTheme(darkTheme = true) {
                        TvOnboardingContent(
                            currentPage = currentPage.intValue,
                            hasStoragePermission = hasStoragePermission.value,
                            onPrevious = { navigateToPage(currentPage.intValue - 1) },
                            onNext = { navigateToPage(currentPage.intValue + 1) },
                            onStart = ::finishOnboarding
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

@Composable
private fun TvOnboardingContent(
    currentPage: Int,
    hasStoragePermission: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStart: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.onboardingBackground)
            .padding(horizontal = 56.dp, vertical = 40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 760.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(168.dp)
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = onboardingTitle(currentPage),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = onboardingDescription(currentPage, hasStoragePermission),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            OnboardingPageIndicator(currentPage = currentPage)
            Spacer(modifier = Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (currentPage > 0) {
                    OutlinedButton(onClick = onPrevious) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.previous))
                    }
                }
                Button(
                    onClick = if (currentPage == 2) onStart else onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(text = stringResource(if (currentPage == 2) R.string.start_vlc else R.string.next))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(if (currentPage == 2) R.drawable.ic_done else R.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun onboardingTitle(pageIndex: Int) = when (pageIndex) {
    0 -> stringResource(R.string.welcome_title)
    1 -> stringResource(R.string.onboarding_scan_title)
    else -> stringResource(R.string.onboarding_all_set)
}

@Composable
private fun onboardingDescription(pageIndex: Int, hasStoragePermission: Boolean) = when (pageIndex) {
    0 -> stringResource(R.string.welcome_subtitle)
    1 -> stringResource(R.string.permission_media)
    else -> if (hasStoragePermission) {
        stringResource(R.string.onboarding_permission_given)
    } else {
        "${stringResource(R.string.permission_expanation_no_allow)}\n${stringResource(R.string.permission_expanation_allow)}"
    }
}

@Composable
private fun OnboardingPageIndicator(currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 12.dp else 9.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) VLCThemeDefaults.colors.primary else Color.White.copy(alpha = 0.35f))
            )
        }
    }
}
