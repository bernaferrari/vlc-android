//
//  AppDelegate.swift
//  VLC-iOS
//
//  Hosts the shared Compose Multiplatform shell (same VlcSharedApp as Android)
//  with real MobileVLCKit decode + Files/Photos library import.
//

import SwiftUI
import UIKit
import CarPlay
import VLCShared

@main
struct VLCiOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootContainer()
                .ignoresSafeArea()
                // SwiftUI scene delivery is the reliable path for document hand-off on modern
                // iPadOS multi-window apps; UIKit delegate delivery remains as a fallback.
                .onOpenURL { _ = importAndPlayIncomingMedia($0) }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosKoinBootstrap.shared.start()
        // Real decode when MobileVLCKit SPM product is linked.
        IosPlaybackService.companion.shared.setBackend(backend: VlcKitBackend.shared)
        IosRendererController.shared.setBackend(backend: VlcKitBackend.shared)
        // The same native package owns LAN discovery and folder parsing; the
        // Compose browser receives only portable entries through its repository.
        IosNetworkBrowserController.shared.setBackend(backend: VlcKitNetworkBrowser.shared)
        // Wait for persisted privacy settings before reading a durable queue.
        // In particular, a prior private session must never be restored during
        // the small window before DataStore hydration completes.
        IosKoinBootstrap.shared.whenPreferencesReady {
            _ = IosPlaybackService.companion.shared.restoreSession()
        }
        IosMediaImportController.shared.setHandler(handler: IosMediaImportBridge.shared)
        IosRemoteAccessServer.shared.setHandler(handler: IosWifiSharingBridge.shared)
        MediaImporter.shared.rescanLocalFolders()
        IosKoinBootstrap.shared.whenPreferencesReady {
            IosRemoteAccessServer.shared.restorePersistedState()
        }
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        importAndPlayIncomingMedia(url)
    }

    func applicationWillResignActive(_ application: UIApplication) {
        IosPlaybackService.companion.shared.saveSession()
        // iOS does not grant a background-server mode for arbitrary local HTTP.
        // Stop explicitly so shared settings never claim a suspended listener is live.
        IosRemoteAccessServer.shared.setEnabled(enabled: false)
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        IosPlaybackService.companion.shared.saveSession()
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // The persisted switch remains enabled; resume the native listener only
        // when the app is foregrounded and Local Network access is meaningful.
        IosRemoteAccessServer.shared.restorePersistedState()
    }

    func applicationWillTerminate(_ application: UIApplication) {
        IosPlaybackService.companion.shared.saveSession()
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        if connectingSceneSession.role == .carTemplateApplication {
            let configuration = UISceneConfiguration(
                name: "VLC CarPlay",
                sessionRole: connectingSceneSession.role
            )
            configuration.delegateClass = VlcCarPlaySceneDelegate.self
            return configuration
        }
        if connectingSceneSession.role == .windowExternalDisplay {
            let configuration = UISceneConfiguration(
                name: "VLC External Display",
                sessionRole: connectingSceneSession.role
            )
            configuration.delegateClass = VlcExternalDisplaySceneDelegate.self
            return configuration
        }
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
}

@MainActor
@discardableResult
private func importAndPlayIncomingMedia(_ url: URL) -> Bool {
    guard let media = MediaImporter.shared.importIncomingURL(url) else { return false }
    // “Open in VLC” means play now, while the permanent Documents copy also appears in the
    // shared library and survives the source app's security-scoped grant.
    // Go through the shared controller so the imported item receives the same history and
    // cross-platform session bookkeeping as a selection made in the Compose library.
    PlaybackController.companion.get().play(item: media, queue: [media])
    return true
}

/// Root: the shared Compose shell owns all permanent product chrome.
struct RootContainer: View {
    var body: some View {
        ComposeSharedRoot()
    }
}

/// UIKit bridge to Kotlin `MainViewController()` — full CMP library/player/settings.
struct ComposeSharedRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // The shared player route supplies the exact Compose-owned drawable.
        IosPlaybackService.companion.shared.setBackend(backend: VlcKitBackend.shared)
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
