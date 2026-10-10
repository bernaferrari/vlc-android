package org.videolan.vlc.gui.onboarding

import android.os.Build
import androidx.compose.runtime.Composable
import org.videolan.vlc.compose.components.VLCOnboardingScreen
import org.videolan.vlc.compose.components.VLCOnboardingStep

/** Android host for the shared first-run journey; permissions stay owned by the activity. */
@Composable
fun OnboardingContent(
    step: OnboardingStep,
    scanStorages: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onGrantPermission: () -> Unit,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
) {
    // Notification consent only exists from Android 13; older devices finish after the scan.
    val stepCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 4 else 3
    VLCOnboardingScreen(
        step = step.shared,
        stepIndex = step.position,
        stepCount = stepCount,
        scanStorages = scanStorages,
        onSkip = onSkip,
        onNext = onNext,
        onGrantPermission = onGrantPermission,
        onScanStoragesChanged = onScanStoragesChanged,
        onCustomizeScan = onCustomizeScan,
    )
}

private val OnboardingStep.shared: VLCOnboardingStep
    get() = when (this) {
        OnboardingStep.WELCOME -> VLCOnboardingStep.Welcome
        OnboardingStep.ASK_PERMISSION -> VLCOnboardingStep.MediaAccess
        OnboardingStep.NO_PERMISSION -> VLCOnboardingStep.AccessDenied
        OnboardingStep.SCAN -> VLCOnboardingStep.LibraryScan
        OnboardingStep.NOTIFICATION_PERMISSION -> VLCOnboardingStep.Notifications
    }

private val OnboardingStep.position: Int
    get() = when (this) {
        OnboardingStep.WELCOME -> 0
        OnboardingStep.ASK_PERMISSION, OnboardingStep.NO_PERMISSION -> 1
        OnboardingStep.SCAN -> 2
        OnboardingStep.NOTIFICATION_PERMISSION -> 3
    }
