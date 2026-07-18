//
//  AppDelegate.swift
//  VLC-iOS
//
//  Hosts the shared Compose Multiplatform shell (same VlcSharedApp as Android).
//  Wire real decode via VlcKitBackend + IosPlaybackService.shared.setBackend.
//

import SwiftUI
import UIKit
import VLCShared

@main
struct VLCiOSApp: App {
    init() {
        // Attach VLCKit backend before Compose shell starts when the pod/SPM is linked.
        Self.attachVlcKitIfAvailable()
    }

    var body: some Scene {
        WindowGroup {
            ComposeSharedRoot()
                .ignoresSafeArea()
        }
    }

    private static func attachVlcKitIfAvailable() {
        #if canImport(MobileVLCKit)
        IosKoinBootstrap.shared.start()
        IosPlaybackService.shared.setBackend(backend: VlcKitBackend())
        #endif
    }
}

/// UIKit bridge to Kotlin `MainViewController()` — full CMP library/player/settings.
struct ComposeSharedRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Optional Swift API lab (VlcSharedApi)

@MainActor
final class VLCAppState: ObservableObject {
    @Published var platformInfo: String = ""
    @Published var mediaCount: Int = 0
    @Published var isInitialized: Bool = false
    @Published var titles: [String] = []
    @Published var status: String = "Starting…"
    @Published var lastError: String?

    private let api = VlcSharedApi()

    func initialize() async {
        IosKoinBootstrap.shared.start()
        #if canImport(MobileVLCKit)
        IosPlaybackService.shared.setBackend(backend: VlcKitBackend())
        #endif
        platformInfo = api.platformInfo()
        isInitialized = api.isInitialized()
        status = isInitialized ? "KMP ready" : "KMP not started"
        await refreshLibrary()
    }

    func refreshLibrary() async {
        mediaCount = Int(api.getMediaCount(type: .all))
        do {
            titles = try await api.listMediaTitles(type: .all, limit: 50) as? [String] ?? []
        } catch {
            titles = []
            lastError = error.localizedDescription
        }
        status = mediaCount == 0
            ? "Library empty — load demo or attach scanner"
            : "\(mediaCount) items"
    }

    func seedDemoLibrary() async {
        api.seedDemoLibrary()
        await refreshLibrary()
        status = "Demo library loaded"
    }

    func playFirst() async {
        let ok = api.playFirst(type: .all)
        status = ok ? "Play requested" : "Nothing to play — seed library first"
    }

    func pause() { api.pause() }
    func resume() { api.resume() }
    func stop() { api.stop() }
}
