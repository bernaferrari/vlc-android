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
        IosMediaImportController.shared.setHandler(handler: IosMediaImportBridge.shared)
        MediaImporter.shared.rescanLocalFolders()
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        // "Open in VLC" / Files share → copy into library
        MediaImporter.shared.rescanLocalFolders()
        IosMediaLibrary.companion.shared.upsert(
            media: MediaItem(
                id: Int64(Date().timeIntervalSince1970 * 1000),
                title: url.deletingPathExtension().lastPathComponent,
                uri: url.absoluteString,
                type: url.pathExtension.lowercased().isVideoExt ? .video : .audio,
                duration: 0,
                artist: nil,
                album: nil,
                albumArtist: nil,
                genre: nil,
                year: 0,
                trackNumber: 0,
                discNumber: 0,
                artworkUri: nil,
                width: 0,
                height: 0,
                mime: nil,
                lastModified: 0,
                size: 0,
                rating: 0,
                playedCount: 0,
                lastPlayed: 0,
                isFavorite: false,
                seen: 0,
                present: true,
                fileName: url.lastPathComponent,
                description: nil
            )
        )
        return true
    }
}

private extension String {
    var isVideoExt: Bool {
        ["mp4", "m4v", "mov", "mkv", "avi", "webm", "ts", "mpg", "mpeg", "3gp"].contains(self)
    }
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
