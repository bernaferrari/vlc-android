package org.videolan.vlc.gui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {
    var permissionRequestInFlight by mutableStateOf(false)
    var notificationRequestInFlight by mutableStateOf(false)
    var scanStorages by mutableStateOf(true)
    var currentStep by mutableStateOf(OnboardingStep.WELCOME)
}

enum class OnboardingStep {
    WELCOME,
    ASK_PERMISSION,
    SCAN,
    NO_PERMISSION,
    NOTIFICATION_PERMISSION,
}
