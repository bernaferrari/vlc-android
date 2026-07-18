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
        IosPlaybackService.shared.setBackend(backend: VlcKitBackend.shared)
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
        IosMediaRepository.shared.upsert(
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
                lastPlayed: 0
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

/// Root: CMP shell + floating import actions.
struct RootContainer: View {
    var body: some View {
        ZStack(alignment: .topTrailing) {
            ComposeSharedRoot()
            ImportToolbar()
                .padding(.top, 56)
                .padding(.trailing, 12)
        }
    }
}

struct ImportToolbar: View {
    var body: some View {
        HStack(spacing: 8) {
            Button {
                present { MediaImporter.shared.presentDocumentPicker(from: $0) }
            } label: {
                Label("Files", systemImage: "folder")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(.ultraThinMaterial, in: Capsule())
            }
            Button {
                present { MediaImporter.shared.presentPhotosPicker(from: $0) }
            } label: {
                Label("Photos", systemImage: "photo.on.rectangle")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(.ultraThinMaterial, in: Capsule())
            }
            Button {
                MediaImporter.shared.rescanLocalFolders()
                VlcSharedApi().seedDemoLibrary()
            } label: {
                Image(systemName: "arrow.clockwise")
                    .padding(8)
                    .background(.ultraThinMaterial, in: Circle())
            }
        }
        .foregroundStyle(.primary)
    }

    private func present(_ block: @escaping (UIViewController) -> Void) {
        guard let root = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow })?
            .rootViewController else { return }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        block(top)
    }
}

/// UIKit bridge to Kotlin `MainViewController()` — full CMP library/player/settings.
struct ComposeSharedRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Ensure backend is attached before first play.
        IosPlaybackService.shared.setBackend(backend: VlcKitBackend.shared)
        let vc = MainViewControllerKt.MainViewController()
        // Prefer drawing video into the host view when VLCKit is present.
        DispatchQueue.main.async {
            VlcKitBackend.shared.attachDrawable(vc.view)
        }
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
