package org.videolan.vlc.gui.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.videolan.resources.AndroidDevices

class OnboardingViewModel : ViewModel() {
    var permissionAlreadyAsked by mutableStateOf(false)
    var notificationPermissionAlreadyAsked by mutableStateOf(false)
    var scanStorages by mutableStateOf(true)
    var permissionType by mutableStateOf(PermissionType.ALL)

    var theme by mutableStateOf(if (AndroidDevices.canUseSystemNightMode()) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO)
    var currentStep by mutableStateOf(OnboardingStep.WELCOME)
}

enum class PermissionType {
    NONE, MEDIA, ALL
}

enum class OnboardingStep {
    WELCOME,
    ASK_PERMISSION,
    SCAN,
    NO_PERMISSION,
    NOTIFICATION_PERMISSION,
    THEME
}
