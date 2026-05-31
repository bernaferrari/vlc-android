/*
 * ************************************************************************
 *  RemoteAccessOnboardingActivity.kt
 * *************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
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

package org.videolan.vlc.remoteaccessserver.gui.remoteaccess.onboarding

import android.app.Activity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.coroutines.delay
import org.videolan.tools.Settings
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.remoteaccessserver.R
import org.videolan.vlc.remoteaccessserver.RemoteAccessOTP
import org.videolan.vlc.remoteaccessserver.viewmodels.RemoteAccessOnboardingViewModel
import org.videolan.vlc.util.isTalkbackIsEnabled
import java.security.SecureRandom

class RemoteAccessOnboardingActivity : AppCompatActivity() {
    private val viewModel: RemoteAccessOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        var currentPage by rememberSaveable { mutableStateOf(viewModel.currentPage) }
                        LaunchedEffect(currentPage) {
                            viewModel.currentPage = currentPage
                        }
                        RemoteAccessOnboardingScreen(
                            currentPage = currentPage,
                            onSkip = ::finish,
                            onNext = {
                                currentPage.next()?.let { currentPage = it } ?: finish()
                            }
                        )
                    }
                }
            }
        )
    }
}

enum class OnboardingPage {
    WELCOME,
    HOW,
    SSL,
    OTP,
    CONTENT;

    fun next(): OnboardingPage? = when (this) {
        WELCOME -> HOW
        HOW -> SSL
        SSL -> OTP
        OTP -> CONTENT
        CONTENT -> null
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RemoteAccessOnboardingScreen(
    currentPage: OnboardingPage,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.onboardingBackground)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
            },
            label = "remote-access-onboarding-page",
            modifier = Modifier.fillMaxSize()
        ) { page ->
            RemoteAccessOnboardingPage(page)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage != OnboardingPage.CONTENT) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip))
                }
                Spacer(Modifier.width(12.dp))
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(if (currentPage == OnboardingPage.CONTENT) R.string.done else R.string.next))
            }
        }
    }
}

@Composable
private fun RemoteAccessOnboardingPage(page: OnboardingPage) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 72.dp)
    ) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageCopy(
                    page = page,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                PageVisual(
                    page = page,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.25f))
                PageCopy(page = page)
                Spacer(Modifier.height(44.dp))
                PageVisual(page = page, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PageCopy(page: OnboardingPage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(page) {
        if ((context as? Activity)?.isTalkbackIsEnabled() == true) focusRequester.requestFocus()
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (page == OnboardingPage.WELCOME) {
            Image(
                painter = painterResource(R.drawable.ic_remote_access),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .padding(bottom = 24.dp)
            )
        }
        Text(
            text = stringResource(page.title),
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .focusRequester(focusRequester)
                .focusable()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(page.description),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp)
        )
    }
}

@Composable
private fun PageVisual(page: OnboardingPage, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (page) {
            OnboardingPage.WELCOME -> Unit
            OnboardingPage.HOW -> DeviceBrowserVisual(secure = false)
            OnboardingPage.SSL -> DeviceBrowserVisual(secure = true)
            OnboardingPage.OTP -> OtpVisual()
            OnboardingPage.CONTENT -> ContentAccessVisual()
        }
    }
}

@Composable
private fun DeviceBrowserVisual(secure: Boolean) {
    var randomData by remember { mutableStateOf(randomBinary()) }
    val transition = rememberInfiniteTransition(label = "remote-access-link")
    val linkScale by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "linkScale"
    )
    val iconProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart),
        label = "iconProgress"
    )
    val icon = when {
        secure -> R.drawable.ic_remote_access_onboarding_encryption
        iconProgress < 0.33f -> R.drawable.ic_remote_access_onboarding_play
        iconProgress < 0.66f -> R.drawable.ic_remote_access_onboarding_pause
        else -> R.drawable.ic_remote_access_onboarding_file
    }
    LaunchedEffect(secure) {
        if (!secure) return@LaunchedEffect
        while (true) {
            randomData = randomBinary()
            delay(150)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.ic_browser),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .scale(scaleX = linkScale, scaleY = 1f)
                    .background(VLCThemeDefaults.colors.primary)
            )
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopCenter)
            )
            if (secure) {
                Text(
                    text = randomData,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        DeviceIcon()
    }
}

@Composable
private fun OtpVisual() {
    val deviceOtp = remember { RemoteAccessOTP.generateCode() }
    var browserOtp by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(true) }
    var accessVisible by remember { mutableStateOf(false) }
    val linkColor = if (accessVisible && accepted) Color(0xFF9CCC65) else if (accessVisible) Color(0xFFD50000) else VLCThemeDefaults.colors.primary
    val accessAlpha by animateFloatAsState(if (accessVisible) 1f else 0f, tween(250), label = "accessAlpha")
    val linkScale by animateFloatAsState(if (browserOtp.isNotEmpty() || accessVisible) 1f else 0f, tween(500), label = "otpLink")

    LaunchedEffect(Unit) {
        while (true) {
            val source = if (accepted) deviceOtp else RemoteAccessOTP.generateCode()
            accessVisible = false
            browserOtp = ""
            delay(500)
            for (index in source.indices) {
                browserOtp = "*".repeat(index) + source[index] + " ".repeat(source.length - index - 1)
                delay(350)
            }
            browserOtp = "****"
            delay(450)
            browserOtp = ""
            accessVisible = true
            delay(1800)
            accepted = !accepted
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        OtpEndpoint(icon = R.drawable.ic_browser, label = browserOtp)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .scale(scaleX = linkScale, scaleY = 1f)
                    .background(linkColor)
            )
            Image(
                painter = painterResource(if (accepted) R.drawable.ic_remote_access_onboarding_verified else R.drawable.ic_remote_access_onboarding_denied),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopCenter)
                    .scale(accessAlpha)
            )
        }
        OtpEndpoint(icon = deviceIconRes(), label = deviceOtp)
    }
}

@Composable
private fun OtpEndpoint(icon: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            minLines = 1
        )
    }
}

@Composable
private fun ContentAccessVisual() {
    val transition = rememberInfiniteTransition(label = "content-access")
    val first by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "first"
    )
    val second by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "second"
    )
    val third by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "third"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        ContentAccessItem(R.drawable.ic_onboarding_scan, R.string.ra_onboarding_content_ml, first)
        Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
            ContentAccessItem(R.drawable.ic_folder, R.string.ra_onboarding_content_files, second)
            ContentAccessItem(R.drawable.ic_remote_access_onboarding_play, R.string.ra_onboarding_content_playback, third)
        }
    }
}

@Composable
private fun ContentAccessItem(icon: Int, label: Int, scale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(label),
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

@Composable
private fun DeviceIcon() {
    Image(
        painter = painterResource(deviceIconRes()),
        contentDescription = null,
        modifier = Modifier.size(72.dp)
    )
}

private fun deviceIconRes() = if (Settings.showTvUi) R.drawable.ic_tv else R.drawable.ic_smartphone

private fun randomBinary(): String = buildString {
    val random = SecureRandom()
    repeat(8) {
        append(if (random.nextBoolean()) "1" else "0")
    }
}

private val OnboardingPage.title: Int
    get() = when (this) {
        OnboardingPage.WELCOME -> R.string.ra_remote_access
        OnboardingPage.HOW -> R.string.ra_onboarding_how_title
        OnboardingPage.SSL -> R.string.ra_onboarding_ssl_title
        OnboardingPage.OTP -> R.string.ra_onboarding_otp_title
        OnboardingPage.CONTENT -> R.string.ra_onboarding_content_title
    }

private val OnboardingPage.description: Int
    get() = when (this) {
        OnboardingPage.WELCOME -> R.string.ra_onboarding_welcome_desc
        OnboardingPage.HOW -> R.string.ra_onboarding_how_desc
        OnboardingPage.SSL -> R.string.ra_onboarding_ssl_desc
        OnboardingPage.OTP -> R.string.ra_onboarding_otp_desc
        OnboardingPage.CONTENT -> R.string.ra_onboarding_content_desc
    }
