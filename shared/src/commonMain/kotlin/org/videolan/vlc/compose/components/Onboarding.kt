package org.videolan.vlc.compose.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.icons.VlcCone
import org.videolan.vlc.compose.theme.LocalVLCMotion
import org.videolan.vlc.compose.theme.VLCMotion
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.customize
import vlc_android.shared.generated.resources.medialibrary_scan_explanation
import vlc_android.shared.generated.resources.next
import vlc_android.shared.generated.resources.notification_permission
import vlc_android.shared.generated.resources.notification_permission_explanation
import vlc_android.shared.generated.resources.onboarding_scan_title
import vlc_android.shared.generated.resources.onboarding_scanning_enable
import vlc_android.shared.generated.resources.permission
import vlc_android.shared.generated.resources.permission_ask_again
import vlc_android.shared.generated.resources.permission_expanation_allow
import vlc_android.shared.generated.resources.permission_expanation_no_allow
import vlc_android.shared.generated.resources.permission_media
import vlc_android.shared.generated.resources.permission_not_granted
import vlc_android.shared.generated.resources.skip
import vlc_android.shared.generated.resources.start
import vlc_android.shared.generated.resources.welcome_subtitle
import vlc_android.shared.generated.resources.welcome_title

/** The steps a first launch can show; hosts decide which ones apply to their platform. */
enum class VLCOnboardingStep { Welcome, MediaAccess, AccessDenied, LibraryScan, Notifications }

/**
 * First-run journey shared by every host. Each step is one idea: an expressive illustration, a
 * title, one paragraph and a single primary action pinned to the bottom. Progress is a row of
 * morphing dots; wide windows place the illustration beside the text.
 */
@Composable
fun VLCOnboardingScreen(
    step: VLCOnboardingStep,
    stepIndex: Int,
    stepCount: Int,
    scanStorages: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onGrantPermission: () -> Unit,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalVLCMotion.current
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                StepDots(current = stepIndex, count = stepCount, modifier = Modifier.weight(1f))
                AnimatedVisibility(visible = stepIndex < stepCount - 1, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = onSkip) { Text(stringResource(Res.string.skip)) }
                }
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    if (motion.reducedMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                    else (slideInHorizontally(tween(VLCMotion.DurationMedium, easing = VLCMotion.EmphasizedDecelerate)) { direction * it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally(tween(VLCMotion.DurationShort, easing = VLCMotion.EmphasizedAccelerate)) { -direction * it / 4 } + fadeOut())
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "onboarding-step",
            ) { shown ->
                StepBody(
                    step = shown,
                    scanStorages = scanStorages,
                    onScanStoragesChanged = onScanStoragesChanged,
                    onCustomizeScan = onCustomizeScan,
                )
            }
            Button(
                onClick = if (step == VLCOnboardingStep.AccessDenied) onGrantPermission else onNext,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(min = 56.dp).align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        when (step) {
                            VLCOnboardingStep.MediaAccess, VLCOnboardingStep.AccessDenied -> Res.string.permission_ask_again
                            else -> if (stepIndex >= stepCount - 1) Res.string.start else Res.string.next
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (step == VLCOnboardingStep.AccessDenied) {
                TextButton(onClick = onNext, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(Res.string.start))
                }
            }
        }
    }
}

@Composable
private fun StepBody(
    step: VLCOnboardingStep,
    scanStorages: Boolean,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        val text: @Composable () -> Unit = {
            Column(horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally) {
                val align = if (wide) TextAlign.Start else TextAlign.Center
                Text(
                    stringResource(step.title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                step.bodies.forEachIndexed { index, body ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (step == VLCOnboardingStep.LibraryScan) {
                    Spacer(Modifier.height(24.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        VLCSettingsToggleRow(
                            title = stringResource(Res.string.onboarding_scanning_enable),
                            checked = scanStorages,
                            onCheckedChange = onScanStoragesChanged,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCustomizeScan, enabled = scanStorages) {
                        Icon(MaterialSymbols.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.customize))
                    }
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { StepIllustration(step) }
                Box(Modifier.weight(1.2f).verticalScroll(rememberScrollState())) { text() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StepIllustration(step)
                Spacer(Modifier.height(40.dp))
                Box(Modifier.widthIn(max = 480.dp)) { text() }
            }
        }
    }
}

/** A soft disc and a tilted, rounded tile that settles with a spring — one hero per step. */
@Composable
private fun StepIllustration(step: VLCOnboardingStep) {
    val motion = LocalVLCMotion.current
    val tilt by animateFloatAsState(
        targetValue = if (step.ordinal % 2 == 0) -10f else 10f,
        animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "onboarding-tilt",
    )
    val warning = step == VLCOnboardingStep.AccessDenied
    val tile by animateColorAsState(
        if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        label = "onboarding-tile",
    )
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(200.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape))
        Box(Modifier.size(132.dp).rotate(tilt).background(tile, RoundedCornerShape(40.dp)))
        if (step == VLCOnboardingStep.Welcome) {
            Image(VlcCone, contentDescription = null, modifier = Modifier.size(84.dp))
        } else {
            Icon(
                step.icon,
                contentDescription = null,
                tint = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

/** The current step stretches into a pill; finished steps stay filled. */
@Composable
private fun StepDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val motion = LocalVLCMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 28.dp else 8.dp,
                animationSpec = if (motion.reducedMotion) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                label = "onboarding-dot",
            )
            val color by animateColorAsState(
                if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "onboarding-dot-color",
            )
            Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
        }
    }
}

private val VLCOnboardingStep.title
    get() = when (this) {
        VLCOnboardingStep.Welcome -> Res.string.welcome_title
        VLCOnboardingStep.MediaAccess -> Res.string.permission
        VLCOnboardingStep.AccessDenied -> Res.string.permission_not_granted
        VLCOnboardingStep.LibraryScan -> Res.string.onboarding_scan_title
        VLCOnboardingStep.Notifications -> Res.string.notification_permission
    }

private val VLCOnboardingStep.bodies
    get() = when (this) {
        VLCOnboardingStep.Welcome -> listOf(Res.string.welcome_subtitle)
        VLCOnboardingStep.MediaAccess -> listOf(Res.string.permission_media)
        VLCOnboardingStep.AccessDenied -> listOf(Res.string.permission_expanation_no_allow, Res.string.permission_expanation_allow)
        VLCOnboardingStep.LibraryScan -> listOf(Res.string.medialibrary_scan_explanation)
        VLCOnboardingStep.Notifications -> listOf(Res.string.notification_permission_explanation)
    }

private val VLCOnboardingStep.icon: MaterialIcon
    get() = when (this) {
        VLCOnboardingStep.Welcome, VLCOnboardingStep.MediaAccess -> MaterialSymbols.Filled.VideoLibrary
        VLCOnboardingStep.AccessDenied -> MaterialSymbols.Filled.Lock
        VLCOnboardingStep.LibraryScan -> MaterialSymbols.Filled.Search
        VLCOnboardingStep.Notifications -> MaterialSymbols.Filled.Notifications
    }
