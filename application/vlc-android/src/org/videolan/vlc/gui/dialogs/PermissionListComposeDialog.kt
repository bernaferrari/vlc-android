package org.videolan.vlc.gui.dialogs

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.videolan.resources.AndroidDevices
import org.videolan.resources.SCHEME_PACKAGE
import org.videolan.resources.util.isExternalStorageManager
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.gui.helpers.hf.StoragePermissionsDelegate.Companion.getStoragePermission
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.isTalkbackIsEnabled

/**
 * Compose-hosted permission overview bottom sheet.
 */
fun ComponentActivity.showPermissionListComposeDialog(onPermissionChanged: ((Boolean) -> Unit)? = null) {
    PermissionListComposeDialog(this, onPermissionChanged).show()
}

private class PermissionListComposeDialog(
    private val activity: ComponentActivity,
    private val onPermissionChanged: ((Boolean) -> Unit)?
) {
    private val dialog = if (Settings.showTvUi) {
        BottomSheetDialog(activity, R.style.Theme_VLC_Black_BottomSheet)
    } else {
        BottomSheetDialog(activity)
    }
    private val permissionState = mutableStateOf<PermissionListState?>(null)
    private val warningTarget = mutableStateOf<PermissionWarningTarget?>(null)
    private var initialPermissionLevel = -1
    private var permissionChanged = false
    private var rootView: ComposeView? = null
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            refreshState()
        }
    }

    fun show() {
        refreshState()
        setupContent()
        activity.lifecycle.addObserver(lifecycleObserver)
        dialog.show()
        configureBottomSheet()
    }

    private fun setupContent() {
        rootView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                permissionState.value?.let { state ->
                    PermissionListComposeDialogContent(
                        state = state,
                        warningTarget = warningTarget.value,
                        onNotificationClick = ::onNotificationClick,
                        onNoAccessClick = ::onNoAccessClick,
                        onMediaAccessClick = ::onMediaAccessClick,
                        onAllAccessClick = ::onAllAccessClick,
                        onAudioPermissionClick = ::onAudioPermissionClick,
                        onVideoPermissionClick = ::onVideoPermissionClick
                    )
                }
            }
        }
        dialog.setContentView(rootView!!)
        dialog.setOnDismissListener {
            onPermissionChanged?.invoke(permissionChanged)
            activity.lifecycle.removeObserver(lifecycleObserver)
            rootView = null
        }
    }

    private fun refreshState() {
        Permissions.emptyCache()
        val nextState = buildPermissionState()
        if (initialPermissionLevel == -1) {
            initialPermissionLevel = nextState.permissionLevel
        } else {
            permissionChanged = initialPermissionLevel != nextState.permissionLevel
        }
        permissionState.value = nextState
    }

    private fun buildPermissionState(): PermissionListState {
        val hasAllAccess = Permissions.hasAllAccess(activity)
        val hasAnyFileFineAccess = Permissions.hasAnyFileFineAccess(activity)
        val canReadStorage = Permissions.canReadStorage(activity)
        val permissionLevel = when {
            hasAllAccess -> 2
            canReadStorage -> 1
            else -> 0
        }
        return PermissionListState(
            notificationAllowed = Permissions.canSendNotifications(activity),
            hasAllAccess = hasAllAccess,
            hasAnyFileFineAccess = hasAnyFileFineAccess,
            canReadStorage = canReadStorage,
            audioPermissionGranted = Permissions.hasAudioPermission(activity),
            videoPermissionGranted = Permissions.hasVideoPermission(activity),
            showNotificationSection = Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 && !AndroidDevices.isTv,
            showMediaAccessOption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            showGranularMediaButtons = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            fileAccessText = when {
                hasAllAccess -> activity.getString(R.string.permission_onboarding_perm_all)
                hasAnyFileFineAccess -> activity.getString(R.string.permission_onboarding_perm_media)
                else -> activity.getString(R.string.permission_expanation_no_allow)
            },
            fileAccessIcon = when {
                hasAllAccess -> R.drawable.ic_perm_all
                hasAnyFileFineAccess -> R.drawable.ic_perm_media
                else -> R.drawable.ic_perm_none
            },
            warningText = when {
                hasAllAccess -> activity.getString(R.string.permission_media_warning)
                hasAnyFileFineAccess -> activity.getString(R.string.permission_all_warning)
                else -> null
            },
            permissionLevel = permissionLevel
        )
    }

    private fun onNotificationClick() {
        warningTarget.value = null
        if (Permissions.canSendNotifications(activity)) {
            Permissions.showAppSettingsPage(activity)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
            )
            Permissions.timeAsked = System.currentTimeMillis()
        }
    }

    private fun onNoAccessClick() {
        val state = permissionState.value ?: return
        if (state.hasAllAccess || state.hasAnyFileFineAccess) {
            warningTarget.value = if (state.hasAllAccess) PermissionWarningTarget.All else PermissionWarningTarget.Media
        }
    }

    private fun onAllAccessClick() {
        val state = permissionState.value ?: return
        warningTarget.value = null
        if (!state.hasAllAccess && state.hasAnyFileFineAccess) {
            warningTarget.value = PermissionWarningTarget.Media
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (!state.hasAllAccess) {
                Permissions.checkReadStoragePermission(activity, false, forceAsking = true)
            } else {
                Permissions.showAppSettingsPage(activity)
            }
        } else {
            val uri = Uri.fromParts(SCHEME_PACKAGE, activity.packageName, null)
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            activity.startActivity(intent)
        }
    }

    private fun onMediaAccessClick() {
        val state = permissionState.value ?: return
        warningTarget.value = null
        when {
            !isExternalStorageManager() && state.hasAnyFileFineAccess -> Permissions.showAppSettingsPage(activity)
            !isExternalStorageManager() && state.canReadStorage -> Permissions.showAppSettingsPage(activity)
            state.hasAllAccess -> warningTarget.value = PermissionWarningTarget.All
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ),
                    Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
                )
                Permissions.timeAsked = System.currentTimeMillis()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
                )
                Permissions.timeAsked = System.currentTimeMillis()
            }
            else -> activity.lifecycleScope.launch {
                activity.getStoragePermission(withDialog = false, onlyMedia = true)
            }
        }
    }

    private fun onAudioPermissionClick() {
        val state = permissionState.value ?: return
        warningTarget.value = null
        if (!state.hasAllAccess && !state.audioPermissionGranted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
            )
            Permissions.timeAsked = System.currentTimeMillis()
        }
    }

    private fun onVideoPermissionClick() {
        val state = permissionState.value ?: return
        warningTarget.value = null
        if (!state.hasAllAccess && !state.videoPermissionGranted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                Permissions.FINE_STORAGE_PERMISSION_REQUEST_CODE
            )
            Permissions.timeAsked = System.currentTimeMillis()
        }
    }

    private fun configureBottomSheet() {
        dialog.window?.setLayout(
            activity.resources.getDimensionPixelSize(R.dimen.default_context_width),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (AndroidDevices.isChromeBook) behavior.isDraggable = false
        }
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusable = false
        dialog.findViewById<View>(R.id.touch_outside)?.isFocusableInTouchMode = false
        rootView?.let { view ->
            if (AndroidDevices.isTv) {
                val overscan = activity.resources.getDimensionPixelSize(org.videolan.resources.R.dimen.tv_overscan_vertical)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + overscan)
            }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
            if (activity.isTalkbackIsEnabled()) view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
    }
}

@Composable
private fun PermissionListComposeDialogContent(
    state: PermissionListState,
    warningTarget: PermissionWarningTarget?,
    onNotificationClick: () -> Unit,
    onNoAccessClick: () -> Unit,
    onMediaAccessClick: () -> Unit,
    onAllAccessClick: () -> Unit,
    onAudioPermissionClick: () -> Unit,
    onVideoPermissionClick: () -> Unit
) {
    VLCTheme {
        val colors = VLCThemeDefaults.colors
        Surface(
            color = colors.backgroundDefault,
            contentColor = colors.fontDefault,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = getString(R.string.permissions),
                    color = colors.fontDefault,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )

                if (state.showNotificationSection) {
                    SectionTitle(
                        text = getString(R.string.notification_permission),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    SimplePermissionRow(
                        text = getString(R.string.notification_permission),
                        icon = if (state.notificationAllowed) R.drawable.ic_permission_check_checked else R.drawable.ic_permission_check_unchecked,
                        onClick = onNotificationClick
                    )
                }

                SectionTitle(
                    text = getString(R.string.manage_storage_permission),
                    modifier = Modifier.padding(top = if (state.showNotificationSection) 8.dp else 24.dp)
                )
                FileAccessExplanation(state)

                PermissionOptionRow(
                    text = getString(R.string.permission_no_access),
                    selected = state.permissionLevel == 0,
                    warning = false,
                    onClick = onNoAccessClick,
                    modifier = Modifier.padding(top = 16.dp)
                )

                if (state.showMediaAccessOption) {
                    PermissionOptionRow(
                        text = getString(R.string.permission_media_title),
                        selected = state.permissionLevel == 1,
                        warning = warningTarget == PermissionWarningTarget.Media,
                        onClick = onMediaAccessClick,
                        trailing = {
                            if (state.showGranularMediaButtons) {
                                IconButton(
                                    enabled = !state.hasAllAccess && !state.audioPermissionGranted,
                                    onClick = onAudioPermissionClick
                                ) {
                                    Image(
                                        painter = painterResource(
                                            if (state.audioPermissionGranted) R.drawable.ic_permission_media_audio
                                            else R.drawable.ic_permission_media_audio_denied
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .alpha(if (!state.hasAllAccess && !state.audioPermissionGranted) 1f else 0.45f)
                                    )
                                }
                                IconButton(
                                    enabled = !state.hasAllAccess && !state.videoPermissionGranted,
                                    onClick = onVideoPermissionClick
                                ) {
                                    Image(
                                        painter = painterResource(
                                            if (state.videoPermissionGranted) R.drawable.ic_permission_media_video
                                            else R.drawable.ic_permission_media_video_denied
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .alpha(if (!state.hasAllAccess && !state.videoPermissionGranted) 1f else 0.45f)
                                    )
                                }
                            }
                        }
                    )
                }

                PermissionOptionRow(
                    text = getString(R.string.permission_all_title),
                    selected = state.permissionLevel == 2,
                    warning = warningTarget == PermissionWarningTarget.All,
                    onClick = onAllAccessClick
                )

                if (warningTarget != null && state.warningText != null) {
                    Text(
                        text = state.warningText,
                        color = colors.fontDefault,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.primary,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(start = 16.dp, end = 16.dp)
    )
}

@Composable
private fun SimplePermissionRow(
    text: String,
    icon: Int,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = text,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 24.dp)
                .weight(1f)
        )
    }
}

@Composable
private fun FileAccessExplanation(state: PermissionListState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VLCThemeDefaults.colors.backgroundDefaultDarker)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(state.fileAccessIcon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = state.fileAccessText,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun PermissionOptionRow(
    text: String,
    selected: Boolean,
    warning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = VLCThemeDefaults.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = 16.dp, top = 4.dp, end = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (warning) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 8.dp, end = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = text,
            color = colors.fontDefault,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun getString(id: Int): String {
    return androidx.compose.ui.platform.LocalContext.current.getString(id)
}

private data class PermissionListState(
    val notificationAllowed: Boolean,
    val hasAllAccess: Boolean,
    val hasAnyFileFineAccess: Boolean,
    val canReadStorage: Boolean,
    val audioPermissionGranted: Boolean,
    val videoPermissionGranted: Boolean,
    val showNotificationSection: Boolean,
    val showMediaAccessOption: Boolean,
    val showGranularMediaButtons: Boolean,
    val fileAccessText: String,
    val fileAccessIcon: Int,
    val warningText: String?,
    val permissionLevel: Int
)

private enum class PermissionWarningTarget {
    Media,
    All
}
