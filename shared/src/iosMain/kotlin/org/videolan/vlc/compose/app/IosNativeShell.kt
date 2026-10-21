@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package org.videolan.vlc.compose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.videolan.tools.VlcSettings
import org.videolan.vlc.app.VlcKoin
import org.videolan.vlc.compose.components.LocalShellBottomInset
import org.videolan.vlc.compose.components.LocalNativeBarHeight
import org.videolan.vlc.compose.components.LocalUseNativeNavigation
import org.videolan.vlc.compose.icons.Icon
import org.videolan.vlc.compose.icons.MaterialSymbols
import org.videolan.vlc.compose.player.PlayerSurface
import org.videolan.vlc.compose.theme.VLCAppTheme
import org.videolan.vlc.compose.theme.VLCThemeAppearance
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.compose.theme.resolveVLCThemePreference
import org.videolan.vlc.model.MediaFolder
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.PlaylistInfo
import org.videolan.vlc.platform.IosAppLockController
import org.videolan.vlc.viewmodel.AudioListViewModel
import org.videolan.vlc.viewmodel.BrowserViewModel
import org.videolan.vlc.viewmodel.MainTab
import org.videolan.vlc.viewmodel.MoreHubViewModel
import org.videolan.vlc.viewmodel.PlayerViewModel
import org.videolan.vlc.viewmodel.PlaylistsViewModel
import org.videolan.vlc.viewmodel.SettingsViewModel
import org.videolan.vlc.viewmodel.VideoListViewModel
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UINavigationItemBackButtonDisplayModeMinimal
import platform.UIKit.colorWithDynamicProvider
import platform.UIKit.modalInPresentation
import platform.UIKit.tabBarItem
import platform.UIKit.UITabBarController
import platform.UIKit.UITabBarItem
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.navigationItem
import platform.darwin.NSObject
import vlc_android.shared.generated.resources.Res
import vlc_android.shared.generated.resources.audio
import vlc_android.shared.generated.resources.browse
import vlc_android.shared.generated.resources.more
import vlc_android.shared.generated.resources.playlists
import vlc_android.shared.generated.resources.video

/**
 * iOS-native chrome around the shared screens, following JetBrains' Liquid Glass guidance: UIKit
 * owns the tab bar, one navigation stack per tab, the back gesture and the "now playing" bottom
 * accessory, so iOS 26+ renders all of them in Liquid Glass with system behaviour. Compose still
 * draws every screen through [ShellScreenContent], exactly as the Android shell does.
 */
internal class IosNativeShell(private val playerSurface: PlayerSurface) {
    private val tabs = UITabBarController()
    private val scope = MainScope()
    private val callbacks = IosShellHostCallbacks { topMost() }
    private val koin get() = VlcKoin.get()
    private val hostPlayer: PlayerViewModel = koin.get()
    private val hostSettings: SettingsViewModel = koin.get()
    private val navigationPolicies = mutableListOf<NavigationBarPolicy>()
    private var playerController: UIViewController? = null
    private var lockController: UIViewController? = null
    // The accessory needs iOS 26 and a Swift host that can weak-link UITabAccessory.
    private val supportsBottomAccessory = isAtLeastIOS(26) && IosTabAccessoryBridge.handler != null

    private val tabOrder = listOf(MainTab.VIDEO, MainTab.AUDIO, MainTab.PLAYLISTS, MainTab.BROWSER, MainTab.MORE)

    fun build(): UIViewController {
        tabs.setViewControllers(tabOrder.map(::tabStack), animated = false)
        IosAppLockController.attachHost { topMost() }
        followTheme()
        if (supportsBottomAccessory) installNowPlayingAccessory()
        guardWithAppLock()
        return tabs
    }

    private fun tabStack(tab: MainTab): UINavigationController {
        val holder = StackHolder()
        val root = screen(tab.toVlcShellRoute(), holder)
        // Pushed screens show only the glass chevron; the previous title lives in its long-press menu.
        root.navigationItem.backButtonDisplayMode = UINavigationItemBackButtonDisplayModeMinimal
        val stack = UINavigationController(rootViewController = root)
        holder.stack = stack
        val policy = NavigationBarPolicy()
        navigationPolicies += policy
        stack.delegate = policy
        stack.setNavigationBarHidden(true, animated = false)
        stack.tabBarItem = UITabBarItem(
            title = tab.fallbackTitle,
            image = UIImage.systemImageNamed(tab.symbol),
            selectedImage = UIImage.systemImageNamed(tab.selectedSymbol),
        )
        scope.launch { stack.tabBarItem.title = getString(tab.titleRes) }
        return stack
    }

    /** One shared screen in its own controller; detail routes own their models, so state never leaks between pages. */
    private fun screen(route: VlcShellRoute, holder: StackHolder): UIViewController {
        lateinit var controller: UIViewController
        val navigator = StackNavigator(route, holder)
        controller = ComposeUIViewController {
            VLCAppTheme {
                val models = remember { newScreenModels() }
                DisposableEffect(models) { onDispose { models.clear() } }
                val title = shellRouteTitle(route, models)
                LaunchedEffect(title) {
                    controller.title = title
                    // Album and playlist heroes already show their name; keep the bar clean.
                    if (route.hasCollectionHero()) controller.navigationItem.titleView = UIView()
                }
                NativeScreenFrame(route, models, navigator)
            }
        }
        return controller
    }

    @Composable
    private fun NativeScreenFrame(route: VlcShellRoute, models: ShellScreenModels, navigator: ShellNavigator) {
        val playerState by models.player.state.collectAsState()
        val inlineMiniPlayer = !supportsBottomAccessory && playerState.hasMedia
        val safeArea = WindowInsets.safeDrawing.asPaddingValues()
        val safeBottom = safeArea.calculateBottomPadding()
        // Pages draw beneath the translucent navigation bar; headers reserve exactly its height.
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navigationBar = (safeArea.calculateTopPadding() - statusBar).coerceAtLeast(0.dp)
        val miniPlayerHeight = if (inlineMiniPlayer) 76.dp else 0.dp
        CompositionLocalProvider(
            LocalUseNativeNavigation provides true,
            LocalNativeBarHeight provides navigationBar,
            LocalShellBottomInset provides safeBottom + miniPlayerHeight,
        ) {
            // A Surface (not a bare background) so untinted text gets the theme's content colour,
            // exactly as the Android shell's Scaffold provides it.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = VLCThemeDefaults.colors.backgroundDefault,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) { Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = statusBar)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                ) {
                    ShellScreenContent(route, models, navigator, callbacks)
                }
                if (route == SettingsRoute || route is SettingsSectionRoute) {
                    val snackbar = remember { SnackbarHostState() }
                    SettingsOverlays(models.settings, snackbar)
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = safeBottom))
                }
                if (inlineMiniPlayer) {
                    // Before iOS 26 there is no tab accessory; the shared mini bar sits above the tab bar.
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = safeBottom)) {
                        MiniBar(state = playerState, onExpand = { presentPlayer(null) }, onToggle = models.player::togglePlayPause)
                    }
                }
            } }
        }
    }

    private fun newScreenModels() = ShellScreenModels(
        video = { koin.get<VideoListViewModel>() },
        audio = { koin.get<AudioListViewModel>() },
        browser = { koin.get<BrowserViewModel>() },
        playlists = { koin.get<PlaylistsViewModel>() },
        more = { koin.get<MoreHubViewModel>() },
        player = { koin.get<PlayerViewModel>() },
        settings = { koin.get<SettingsViewModel>() },
    )

    private inner class StackNavigator(
        private val current: VlcShellRoute,
        private val holder: StackHolder,
    ) : ShellNavigator {
        private fun push(route: VlcShellRoute) {
            holder.stack?.pushViewController(screen(route, holder), animated = true)
        }

        override fun openVideoContainer(folder: MediaFolder) = push(folder.toVideoContainerRoute())
        override fun openAudioEntity(item: MediaItem) {
            item.toAudioEntityRoute()?.let(::push)
        }

        override fun openBrowserFolder(folder: MediaFolder) {
            val target = folder.uri.ifBlank { folder.path }
            if (target.startsWith("otg://", ignoreCase = true)) callbacks.onRequestOtgRoot()
            val parents = (current as? BrowserFolderRoute)?.toMediaFolders().orEmpty()
            push(BrowserFolderRoute.from(parents + folder))
        }

        override fun openPlaylist(info: PlaylistInfo) = push(PlaylistDetailRoute(id = info.id, name = info.name))
        override fun openSettings() {
            scope.launch { if (hostSettings.canOpenSettings()) push(SettingsRoute) }
        }

        override fun openSettingsSection(id: String) {
            scope.launch { if (hostSettings.canOpenSection(id)) push(SettingsSectionRoute(id)) }
        }

        override fun open(route: VlcShellRoute) = push(route)
        override fun openPlayer() = presentPlayer(null)
        override fun openMediaPlayer(item: MediaItem) = presentPlayer(item)
        override fun back() {
            holder.stack?.popViewControllerAnimated(true)
        }
    }

    /** Full-screen modal player; video needs the whole display and audio keeps its own swipe-down dismiss. */
    private fun presentPlayer(requested: MediaItem?) {
        if (playerController != null) return
        lateinit var controller: UIViewController
        controller = ComposeUIViewController {
            VLCAppTheme {
                val player = remember { koin.get<PlayerViewModel>() }
                DisposableEffect(player) { onDispose { player.onCleared() } }
                ShellPlayerScreen(
                    player = player,
                    requestedMedia = requested,
                    onClose = {
                        controller.dismissViewControllerAnimated(true) { playerController = null }
                    },
                    playerSurface = playerSurface,
                    hostCallbacks = callbacks,
                )
            }
        }
        controller.modalPresentationStyle = UIModalPresentationFullScreen
        playerController = controller
        topMost().presentViewController(controller, animated = true, completion = null)
    }

    /** iOS 26+: Music-style now-playing row in the tab bar's Liquid Glass accessory. */
    private fun installNowPlayingAccessory() {
        val content = NowPlayingAccessoryView(hostPlayer, scope) { presentPlayer(null) }
        scope.launch {
            hostPlayer.state.map { it.hasMedia }.distinctUntilChanged().collect { hasMedia ->
                IosTabAccessoryBridge.handler?.setAccessory(tabs, if (hasMedia) content else null, animated = true)
            }
        }
    }

    /** Tab bar, navigation bars and native alerts follow VLC's appearance and accent. */
    private fun followTheme() {
        scope.launch {
            combine(VlcSettings.themeAppearance, VlcSettings.themeAccent) { appearance, accent ->
                resolveVLCThemePreference(appearance, accent)
            }.collect { preference ->
                tabs.overrideUserInterfaceStyle = when (preference.appearance) {
                    VLCThemeAppearance.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                    VLCThemeAppearance.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    VLCThemeAppearance.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
                }
                val light = preference.accent.primary(darkTheme = false)
                val dark = preference.accent.primary(darkTheme = true)
                tabs.view.tintColor = UIColor.colorWithDynamicProvider { traits: UITraitCollection? ->
                    val c = if (traits?.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark) dark else light
                    UIColor(red = c.red.toDouble(), green = c.green.toDouble(), blue = c.blue.toDouble(), alpha = c.alpha.toDouble())
                }
            }
        }
    }

    /** The shared app lock covers everything, including a presented player. */
    private fun guardWithAppLock() {
        scope.launch {
            hostSettings.state.map { it.appLock.supported && it.appLock.enabled && it.appLock.locked }
                .distinctUntilChanged()
                .collect { locked ->
                    if (locked && lockController == null) {
                        val gate = ComposeUIViewController { VLCAppTheme { AppLockGate(onUnlock = hostSettings::unlockAppLock) } }
                        gate.modalPresentationStyle = UIModalPresentationFullScreen
                        gate.modalInPresentation = true
                        lockController = gate
                        topMost().presentViewController(gate, animated = false, completion = null)
                    } else if (!locked) {
                        lockController?.dismissViewControllerAnimated(true, completion = null)
                        lockController = null
                    }
                }
        }
    }

    private fun topMost(): UIViewController {
        var top: UIViewController = tabs
        while (true) top = top.presentedViewController ?: return top
    }

    private class StackHolder {
        var stack: UINavigationController? = null
    }

    /** Roots draw their own editorial header; pushed screens use the native Liquid Glass bar. */
    private class NavigationBarPolicy : NSObject(), UINavigationControllerDelegateProtocol {
        @kotlinx.cinterop.ObjCSignatureOverride
        override fun navigationController(
            navigationController: UINavigationController,
            willShowViewController: UIViewController,
            animated: Boolean,
        ) {
            val isRoot = navigationController.viewControllers.firstOrNull() == willShowViewController
            navigationController.setNavigationBarHidden(isRoot, animated = animated)
        }
    }
}

private val MainTab.titleRes: StringResource
    get() = when (this) {
        MainTab.VIDEO -> Res.string.video
        MainTab.AUDIO -> Res.string.audio
        MainTab.PLAYLISTS -> Res.string.playlists
        MainTab.BROWSER -> Res.string.browse
        MainTab.MORE -> Res.string.more
    }

private val MainTab.fallbackTitle: String
    get() = name.lowercase().replaceFirstChar(Char::uppercase)

private val MainTab.symbol: String
    get() = when (this) {
        MainTab.VIDEO -> "play.rectangle.on.rectangle"
        MainTab.AUDIO -> "music.note"
        MainTab.PLAYLISTS -> "music.note.list"
        MainTab.BROWSER -> "folder"
        MainTab.MORE -> "ellipsis.circle"
    }

private val MainTab.selectedSymbol: String
    get() = when (this) {
        MainTab.VIDEO -> "play.rectangle.on.rectangle.fill"
        MainTab.AUDIO -> "music.note"
        MainTab.PLAYLISTS -> "music.note.list"
        MainTab.BROWSER -> "folder.fill"
        MainTab.MORE -> "ellipsis.circle.fill"
    }

private fun isAtLeastIOS(major: Int): Boolean =
    NSProcessInfo.processInfo.operatingSystemVersionString
        .substringAfter("Version ").substringBefore('.').trim().toIntOrNull()?.let { it >= major } ?: false
