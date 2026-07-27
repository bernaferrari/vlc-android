//
//  AppDelegate.swift
//  VLC-iOS
//
//  Hosts the shared Compose Multiplatform shell (same VlcSharedApp as Android)
//  with real MobileVLCKit decode + Files/Photos library import.
//

import SwiftUI
import UIKit
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
        // Prepare the prior queue without starting playback. The shared paused state lets the
        // common mini-player offer an explicit resume action after relaunch.
        _ = IosPlaybackService.companion.shared.restoreSession()
        IosMediaImportController.shared.setHandler(handler: IosMediaImportBridge.shared)
        MediaImporter.shared.rescanLocalFolders()
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
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        IosPlaybackService.companion.shared.saveSession()
    }

    func applicationWillTerminate(_ application: UIApplication) {
        IosPlaybackService.companion.shared.saveSession()
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
