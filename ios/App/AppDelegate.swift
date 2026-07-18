//
//  AppDelegate.swift
//  VLC-iOS
//
//  SwiftUI shell consuming VLCShared. Wire VLCKit by implementing VlcKitPlayerBackend
//  and calling IosPlaybackService.shared.setBackend(...).
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
                .task {
                    await appState.initialize()
                }
        }
    }
}

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
        // Optional: attach VLCKit when linked — see VlcKitBackend.swift
        // IosPlaybackService.shared.setBackend(backend: VlcKitBackend())
        platformInfo = api.platformInfo()
        isInitialized = api.isInitialized()
        status = isInitialized ? "KMP ready" : "KMP not started"
        await refreshLibrary()
    }

    func refreshLibrary() async {
        mediaCount = Int(api.getMediaCount(type: .all))
        // listMediaTitles is async in Kotlin; from Swift use async bridge
        do {
            titles = try await api.listMediaTitles(type: .all, limit: 50) as? [String] ?? []
        } catch {
            // Fallback: count only
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
        status = "Demo library loaded (decode needs VLCKit backend)"
    }

    func playFirst() async {
        let ok = api.playFirst(type: .all)
        status = ok ? "Play requested" : "Nothing to play — seed library first"
    }

    func pause() { api.pause() }
    func resume() { api.resume() }
    func stop() { api.stop() }
}

struct ContentView: View {
    @EnvironmentObject var appState: VLCAppState

    var body: some View {
        NavigationStack {
            List {
                Section("KMP") {
                    LabeledContent("Platform", value: appState.platformInfo)
                    LabeledContent("Initialized", value: appState.isInitialized ? "Yes" : "No")
                    LabeledContent("Status", value: appState.status)
                }
                Section("Library (\(appState.mediaCount))") {
                    if appState.titles.isEmpty {
                        Text("No media yet")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(appState.titles, id: \.self) { title in
                            Text(title)
                        }
                    }
                    Button("Load demo library") {
                        Task { await appState.seedDemoLibrary() }
                    }
                    Button("Refresh") {
                        Task { await appState.refreshLibrary() }
                    }
                }
                Section("Playback") {
                    Text("Attach VLCKit via IosPlaybackService.shared.setBackend(…)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    HStack {
                        Button("Play") { Task { await appState.playFirst() } }
                            .buttonStyle(.borderedProminent)
                        Button("Pause") { appState.pause() }
                        Button("Resume") { appState.resume() }
                        Button("Stop") { appState.stop() }
                    }
                }
                if let err = appState.lastError {
                    Section("Error") {
                        Text(err).foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("VLC")
        }
    }
}
