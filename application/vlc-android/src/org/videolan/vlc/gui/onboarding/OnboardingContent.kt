package org.videolan.vlc.gui.onboarding

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.R
import org.videolan.vlc.compose.theme.VLCThemeDefaults

@Composable
fun OnboardingContent(
    step: OnboardingStep,
    permissionType: PermissionType,
    scanStorages: Boolean,
    theme: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onPermissionTypeSelected: (PermissionType) -> Unit,
    onGrantPermission: () -> Unit,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
    onThemeSelected: (Int) -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = colors.onboardingBackground,
        contentColor = colors.fontDefault,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                OnboardingStepContent(
                    step = step,
                    permissionType = permissionType,
                    scanStorages = scanStorages,
                    theme = theme,
                    onPermissionTypeSelected = onPermissionTypeSelected,
                    onGrantPermission = onGrantPermission,
                    onScanStoragesChanged = onScanStoragesChanged,
                    onCustomizeScan = onCustomizeScan,
                    onThemeSelected = onThemeSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.fontDefault)
                ) {
                    Text(stringResource(R.string.skip))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(if (step == OnboardingStep.THEME) R.string.done else R.string.next))
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    permissionType: PermissionType,
    scanStorages: Boolean,
    theme: Int,
    onPermissionTypeSelected: (PermissionType) -> Unit,
    onGrantPermission: () -> Unit,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (step) {
        OnboardingStep.WELCOME -> WelcomeStep(modifier)
        OnboardingStep.ASK_PERMISSION -> PermissionStep(
            selected = permissionType,
            onSelected = onPermissionTypeSelected,
            modifier = modifier
        )
        OnboardingStep.SCAN -> ScanningStep(
            scanStorages = scanStorages,
            onScanStoragesChanged = onScanStoragesChanged,
            onCustomizeScan = onCustomizeScan,
            modifier = modifier
        )
        OnboardingStep.NO_PERMISSION -> NoPermissionStep(
            onGrantPermission = onGrantPermission,
            modifier = modifier
        )
        OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(modifier)
        OnboardingStep.THEME -> ThemeStep(
            selectedTheme = theme,
            onSelected = onThemeSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun WelcomeStep(modifier: Modifier = Modifier) {
    OnboardingCenterColumn(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ic_icon),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingTitle(text = stringResource(R.string.welcome_title))
        OnboardingBody(
            text = stringResource(R.string.welcome_subtitle),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PermissionStep(
    selected: PermissionType,
    onSelected: (PermissionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        PermissionOption(PermissionType.NONE, R.drawable.ic_perm_none, R.string.permission_onboarding_no_perm),
        PermissionOption(PermissionType.MEDIA, R.drawable.ic_perm_media, R.string.permission_onboarding_perm_media),
        PermissionOption(PermissionType.ALL, R.drawable.ic_perm_all, R.string.permission_onboarding_perm_all)
    )
    OnboardingCenterColumn(modifier = modifier) {
        OnboardingTitle(text = stringResource(R.string.permission))
        OnboardingBody(
            text = stringResource(R.string.permission_media),
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            options.forEach { option ->
                SelectableIconOption(
                    icon = option.icon,
                    contentDescription = stringResource(option.description),
                    selected = selected == option.type,
                    tintSelectedOnly = true,
                    onClick = { onSelected(option.type) }
                )
            }
        }
        Text(
            text = stringResource(permissionDescriptionRes(selected)),
            color = VLCThemeDefaults.colors.primary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
        )
    }
}

@Composable
private fun ScanningStep(
    scanStorages: Boolean,
    onScanStoragesChanged: (Boolean) -> Unit,
    onCustomizeScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCenterColumn(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ic_onboarding_scan),
            contentDescription = null,
            modifier = Modifier.size(104.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingTitle(text = stringResource(R.string.onboarding_scan_title))
        OnboardingBody(
            text = stringResource(R.string.medialibrary_scan_explanation),
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .clickable(role = Role.Switch) { onScanStoragesChanged(!scanStorages) }
        ) {
            Text(
                text = stringResource(R.string.onboarding_scanning_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = VLCThemeDefaults.colors.fontDefault,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = scanStorages,
                onCheckedChange = null
            )
        }
        Button(
            onClick = onCustomizeScan,
            enabled = scanStorages,
            colors = ButtonDefaults.buttonColors(
                containerColor = VLCThemeDefaults.colors.primary,
                contentColor = Color.White
            ),
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(stringResource(R.string.customize))
        }
    }
}

@Composable
private fun NoPermissionStep(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingCenterColumn(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ic_onboarding_no_permission),
            contentDescription = null,
            modifier = Modifier.size(104.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingTitle(text = stringResource(R.string.permission_not_granted))
        OnboardingBody(
            text = stringResource(R.string.permission_expanation_no_allow),
            modifier = Modifier.padding(top = 8.dp)
        )
        OnboardingBody(
            text = stringResource(R.string.permission_expanation_allow),
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = onGrantPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = VLCThemeDefaults.colors.primary,
                contentColor = Color.White
            ),
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(stringResource(R.string.permission_ask_again))
        }
    }
}

@Composable
private fun NotificationPermissionStep(modifier: Modifier = Modifier) {
    OnboardingCenterColumn(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ic_onboarding_notification),
            contentDescription = null,
            modifier = Modifier.size(104.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingTitle(text = stringResource(R.string.notification_permission))
        OnboardingBody(
            text = stringResource(R.string.notification_permission_explanation),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ThemeStep(
    selectedTheme: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val autoTheme = if (AndroidDevices.canUseSystemNightMode()) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    } else {
        AppCompatDelegate.MODE_NIGHT_AUTO
    }
    val options = listOf(
        ThemeOption(autoTheme, R.drawable.ic_theme_daynight, R.string.theme_auto),
        ThemeOption(AppCompatDelegate.MODE_NIGHT_NO, R.drawable.ic_theme_light, R.string.light_theme),
        ThemeOption(AppCompatDelegate.MODE_NIGHT_YES, R.drawable.ic_theme_dark, R.string.enable_black_theme)
    )
    OnboardingCenterColumn(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                SelectableIconOption(
                    icon = option.icon,
                    contentDescription = stringResource(option.contentDescription),
                    selected = selectedTheme == option.theme,
                    tintSelectedOnly = false,
                    onClick = { onSelected(option.theme) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingTitle(text = stringResource(R.string.onboarding_theme_title))
        OnboardingBody(
            text = stringResource(themeDescriptionRes(selectedTheme)),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun OnboardingCenterColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val horizontalPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 64.dp else 8.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        content()
    }
}

@Composable
private fun OnboardingTitle(text: String) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.fontDefault,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
    )
}

@Composable
private fun OnboardingBody(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = VLCThemeDefaults.colors.fontLight,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun SelectableIconOption(
    @DrawableRes icon: Int,
    contentDescription: String,
    selected: Boolean,
    tintSelectedOnly: Boolean,
    onClick: () -> Unit
) {
    val colors = VLCThemeDefaults.colors
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else null,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .scale(if (selected) 1f else 0.82f)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            colorFilter = when {
                !tintSelectedOnly -> null
                selected -> ColorFilter.tint(colors.primary)
                else -> ColorFilter.tint(Color.White)
            },
            modifier = Modifier
                .padding(16.dp)
                .size(72.dp)
        )
    }
}

@StringRes
private fun permissionDescriptionRes(permissionType: PermissionType) = when (permissionType) {
    PermissionType.NONE -> R.string.permission_onboarding_no_perm
    PermissionType.MEDIA -> R.string.permission_onboarding_perm_media
    PermissionType.ALL -> R.string.permission_onboarding_perm_all
}

@StringRes
private fun themeDescriptionRes(theme: Int): Int {
    val autoTheme = if (AndroidDevices.canUseSystemNightMode()) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    } else {
        AppCompatDelegate.MODE_NIGHT_AUTO
    }
    return when (theme) {
        AppCompatDelegate.MODE_NIGHT_NO -> R.string.light_theme
        AppCompatDelegate.MODE_NIGHT_YES -> R.string.enable_black_theme
        autoTheme -> if (AndroidDevices.canUseSystemNightMode()) R.string.daynight_system_explanation else R.string.daynight_legacy_explanation
        else -> if (AndroidDevices.canUseSystemNightMode()) R.string.daynight_system_explanation else R.string.daynight_legacy_explanation
    }
}

private data class PermissionOption(
    val type: PermissionType,
    @DrawableRes val icon: Int,
    @StringRes val description: Int
)

private data class ThemeOption(
    val theme: Int,
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int
)
