//
//  AppDelegate.swift
//  VLC-iOS
//
//  KMP skeleton app consuming the VLCShared framework.
//  This is a minimal SwiftUI app that demonstrates calling into the shared
//  Kotlin module from Swift. The actual VLC iOS player will be built on top
//  of this foundation.
//

import SwiftUI
import VLCShared

@main
struct VLCiOSApp: App {
    @StateObject private var appState = VLCAppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .onAppear {
                    appState.initialize()
                }
        }
    }
}

/// App-level state bridging Kotlin/VLCShared and SwiftUI.
@MainActor
final class VLCAppState: ObservableObject {
    @Published var platformInfo: String = ""
    @Published var mediaCount: Int = 0
    @Published var isInitialized: Bool = false
    @Published var videos: [String] = []
    @Published var playlists: [String] = []

    private let api = VlcSharedApi()

    func initialize() {
        platformInfo = api.platformInfo()
        isInitialized = api.isInitialized()

        // In the full app, this is where we'd:
        // 1. Create IosVlcDataStoreFactory() and VlcPreferences(dataStore)
        // 2. Create platform-specific MediaRepository impl (VLCKit medialibrary)
        // 3. Create platform-specific PlaybackService impl (VLCKit MediaPlayer)
        // 4. startKoin with iOS modules (VlcKoin) — there is no VlcAppContainer type
        //
        // For now, just demonstrate the API surface is accessible.
        Task {
            mediaCount = await api.getMediaCount(type: MediaType.all)
        }
    }

    func playFirstVideo() {
        // Placeholder — in the full app, this calls:
        // VlcApp.shared.container.playbackService.play(item, playlist)
        // which bridges to VLCKit's VLCMediaPlayer
        print("Play action triggered — wire to VLCKit PlaybackService")
    }
}

/// Main content view — a simple dashboard showing KMP integration status.
struct ContentView: View {
    @EnvironmentObject var appState: VLCAppState

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                // KMP integration status
                VStack(alignment: .leading, spacing: 12) {
                    Text("VLC KMP")
                        .font(.largeTitle)
                        .fontWeight(.bold)

                    InfoRow(label: "Platform", value: appState.platformInfo)
                    InfoRow(label: "KMP Initialized", value: appState.isInitialized ? "Yes" : "No")
                    InfoRow(label: "Media Count", value: "\(appState.mediaCount)")
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                // Placeholder for media library
                VStack(alignment: .leading, spacing: 8) {
                    Text("Library")
                        .font(.headline)
                    Text("Media library integration pending.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text("Wire VLCKit medialibrary → MediaRepository")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                // Placeholder for playback
                VStack(alignment: .leading, spacing: 8) {
                    Text("Playback")
                        .font(.headline)
                    Text("Player integration pending.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Button("Play (placeholder)") {
                        appState.playFirstVideo()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)

                Spacer()
            }
            .padding()
            .navigationTitle("VLC")
        }
    }
}

struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}
