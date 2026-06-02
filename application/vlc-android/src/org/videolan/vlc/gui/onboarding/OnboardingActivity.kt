package org.videolan.vlc.gui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.videolan.resources.ACTIVITY_RESULT_PREFERENCES
import org.videolan.resources.AndroidDevices
import org.videolan.resources.EXTRA_FIRST_RUN
import org.videolan.resources.EXTRA_UPGRADE
import org.videolan.resources.KEY_ANIMATED
import org.videolan.resources.PREF_FIRST_RUN
import org.videolan.resources.util.startMedialibrary
import org.videolan.tools.KEY_APP_THEME
import org.videolan.tools.KEY_NAVIGATION_ID
import org.videolan.tools.KEY_MEDIALIBRARY_SCAN
import org.videolan.tools.ML_SCAN_OFF
import org.videolan.tools.ML_SCAN_ON
import org.videolan.tools.NOTIFICATION_PERMISSION_ASKED
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.MediaParsingService
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.helpers.hf.NotificationDelegate.Companion.getNotificationPermission
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate.Companion.getStoragePermission
import org.videolan.vlc.util.Permissions

const val ONBOARDING_DONE_KEY = "app_onboarding_done"

class OnboardingActivity : AppCompatActivity() {
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (AndroidDevices.canUseSystemNightMode()) enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCTheme(darkTheme = true) {
                        OnboardingContent(
                            step = viewModel.currentStep,
                            permissionType = viewModel.permissionType,
                            scanStorages = viewModel.scanStorages,
                            theme = viewModel.theme,
                            onSkip = ::onDone,
                            onNext = ::onNext,
                            onPermissionTypeSelected = { viewModel.permissionType = it },
                            onGrantPermission = {
                                viewModel.permissionAlreadyAsked = false
                                showStep(OnboardingStep.ASK_PERMISSION)
                            },
                            onScanStoragesChanged = ::setScanStorages,
                            onCustomizeScan = ::openStorageCustomization,
                            onThemeSelected = { viewModel.theme = it }
                        )
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Permissions.sAlertDialog?.dismiss()
    }

    private fun showStep(step: OnboardingStep) {
        viewModel.currentStep = step
    }

    private fun setScanStorages(enabled: Boolean) {
        Settings.getInstance(this).putSingle(KEY_MEDIALIBRARY_SCAN, if (enabled) ML_SCAN_ON else ML_SCAN_OFF)
        viewModel.scanStorages = enabled
    }

    private fun openStorageCustomization() {
        val intent = Intent(applicationContext, SecondaryActivity::class.java)
        intent.putExtra(SecondaryActivity.KEY_DESTINATION, SecondaryActivity.STORAGE_BROWSER_ONBOARDING)
        intent.putExtra(KEY_ANIMATED, true)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.no_animation)
    }

    private fun onDone() {
        setResult(RESULT_RESTART)
        Settings.getInstance(this).edit {
            putInt(PREF_FIRST_RUN, BuildConfig.VLC_VERSION_CODE)
            putBoolean(ONBOARDING_DONE_KEY, true)
            putInt(KEY_MEDIALIBRARY_SCAN, if (viewModel.scanStorages) ML_SCAN_ON else ML_SCAN_OFF)
            putInt(KEY_NAVIGATION_ID, if (viewModel.scanStorages) R.id.nav_video else R.id.nav_directories)
            putString(KEY_APP_THEME, viewModel.theme.toString())
        }
        if (!viewModel.scanStorages) MediaParsingService.preselectedStorages.clear()
        startMedialibrary(firstRun = true, upgrade = true, parse = viewModel.scanStorages)
        val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
            .putExtra(EXTRA_FIRST_RUN, true)
            .putExtra(EXTRA_UPGRADE, true)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.permissionAlreadyAsked = true
                onNext()
            }
        }
    }

    private fun askPermission() {
        lifecycleScope.launch {
            val onlyMedia = viewModel.permissionType == PermissionType.MEDIA
            viewModel.permissionAlreadyAsked = true
            if (onlyMedia && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this@OnboardingActivity,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ),
                    Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
                )
                return@launch
            }
            getStoragePermission(withDialog = false, onlyMedia = onlyMedia)
            onNext()
        }
    }

    private fun askNotificationPermission() {
        lifecycleScope.launch {
            viewModel.notificationPermissionAlreadyAsked = true
            getNotificationPermission()
            Settings.getInstance(this@OnboardingActivity).edit {
                putBoolean(NOTIFICATION_PERMISSION_ASKED, true)
            }
            onNext()
        }
    }

    private fun onNext() {
        when (viewModel.currentStep) {
            OnboardingStep.WELCOME -> {
                showStep(if (Permissions.canReadStorage(this)) OnboardingStep.SCAN else OnboardingStep.ASK_PERMISSION)
            }
            OnboardingStep.ASK_PERMISSION -> {
                if (viewModel.permissionType != PermissionType.NONE && !viewModel.permissionAlreadyAsked) {
                    askPermission()
                } else {
                    showStep(if (Permissions.canReadStorage(applicationContext)) OnboardingStep.SCAN else OnboardingStep.NO_PERMISSION)
                }
            }
            OnboardingStep.NO_PERMISSION -> {
                showStep(if (Permissions.canReadStorage(applicationContext)) OnboardingStep.SCAN else OnboardingStep.THEME)
            }
            OnboardingStep.NOTIFICATION_PERMISSION -> {
                if (!Permissions.canSendNotifications(applicationContext) && !viewModel.notificationPermissionAlreadyAsked) {
                    askNotificationPermission()
                } else {
                    showStep(OnboardingStep.THEME)
                }
            }
            OnboardingStep.SCAN -> {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && !Permissions.canSendNotifications(applicationContext)) {
                    showStep(OnboardingStep.NOTIFICATION_PERMISSION)
                } else {
                    showStep(OnboardingStep.THEME)
                }
            }
            OnboardingStep.THEME -> onDone()
        }
    }
}

fun Activity.startOnboarding() = startActivityForResult(Intent(this, OnboardingActivity::class.java), ACTIVITY_RESULT_PREFERENCES)
