//
//  VlcKitNetworkBrowser.swift
//
//  Native VLCKit LAN discovery and server-folder parsing for the shared
//  Compose browser. Product state remains in common Kotlin; this file owns only
//  opaque VLCMedia lifecycle and asynchronous Objective-C callbacks.
//

import Foundation
import VLCShared

#if canImport(VLCKit)
import VLCKit
#endif

final class VlcKitNetworkBrowser: NSObject, IosNetworkBrowserBackend {
    static let shared = VlcKitNetworkBrowser()

    private var discoveryListener: IosNetworkBrowserBackendDiscoveryListener?
    private var browseListener: IosNetworkBrowserBackendBrowseListener?

#if canImport(VLCKit)
    private var discoverers: [VLCMediaDiscoverer] = []
    private var discoveryTimer: Timer?
    private var browsingMedia: VLCMedia?
    // VLCKit 4 owns parsing through a reusable parser. Keeping it here also
    // gives cancellation a real effect when the Compose screen changes.
    private let mediaParser = VLCMediaParser.shared()
#endif

    private override init() {
        super.init()
    }

    func startDiscovery(listener: IosNetworkBrowserBackendDiscoveryListener?) {
        discoveryListener = listener
#if canImport(VLCKit)
        discoveryTimer?.invalidate()
        discoverers.forEach { $0.stop() }
        discoverers.removeAll()
        guard listener != nil else { return }

        // These are the libVLC services used by VLC for iOS' upstream local
        // network browsers. Unsupported modules simply return a start error.
        for service in ["upnp", "dsm", "nfs", "bonjour"] {
            let discoverer = VLCMediaDiscoverer(name: service)
            guard discoverer.start() == 0 else { continue }
            discoverers.append(discoverer)
        }
        publishRoots()
        discoveryTimer = Timer.scheduledTimer(
            timeInterval: 1.5,
            target: self,
            selector: #selector(refreshDiscovery),
            userInfo: nil,
            repeats: true
        )
#else
        listener?.onError(message: "VLCKit is unavailable in this build.")
#endif
    }

    func browse(uri: String, listener: IosNetworkBrowserBackendBrowseListener?) {
        cancelBrowse()
        browseListener = listener
#if canImport(VLCKit)
        guard let url = URL(string: uri) else {
            listener?.onError(message: "Invalid network address.")
            return
        }
        guard let media = VLCMedia(url: url) else {
            listener?.onError(message: "Unable to open this network location.")
            return
        }
        media.delegate = self
        browsingMedia = media
        // VLCKit parses server trees asynchronously and supplies their
        // children through `subitems`, exactly as the upstream iOS browser
        // does. VLCKit 4 moved this operation from VLCMedia to VLCMediaParser.
        guard mediaParser.queue(
            media,
            options: VLCMediaParsingOptions(rawValue: 1) // VLCMediaParse
        ) == 0 else {
            finishBrowse(error: "Unable to open this network location.")
            return
        }
#else
        listener?.onError(message: "VLCKit is unavailable in this build.")
#endif
    }

    func cancelBrowse() {
#if canImport(VLCKit)
        if let browsingMedia {
            mediaParser.cancelParsing(for: browsingMedia)
        }
        browsingMedia?.delegate = nil
        browsingMedia = nil
#endif
        browseListener = nil
    }

#if canImport(VLCKit)
    @objc private func refreshDiscovery() {
        publishRoots()
    }

    private func publishRoots() {
        let discoveredEntries = discoverers.flatMap { discoverer in
            entries(in: discoverer.discoveredMedia)
        }
        discoveryListener?.onRootsChanged(entries: discoveredEntries)
    }

    private func entries(in mediaList: VLCMediaList?) -> [IosNetworkEntry] {
        guard let mediaList else { return [] }
        return (0..<mediaList.count).compactMap { index in
            guard let media = mediaList.media(at: UInt(index)) else { return nil }
            return entry(for: media)
        }
    }

    private func entry(for media: VLCMedia) -> IosNetworkEntry? {
        guard let url = media.url?.absoluteString, !url.isEmpty else { return nil }
        let title = media.metaData.title?.trimmingCharacters(in: .whitespacesAndNewlines)
        let fallback = media.url?.lastPathComponent.removingPercentEncoding
            ?? media.url?.host
            ?? url
        return IosNetworkEntry(
            title: title?.isEmpty == false ? title! : fallback,
            uri: url,
            isDirectory: media.mediaType == .directory,
            artworkUri: media.metaData.artworkURL?.absoluteString,
            durationMs: Int64(max(0, media.length.intValue)),
            size: 0
        )
    }

    private func finishBrowse(error: String? = nil) {
        let listener = browseListener
        let entries = entries(in: browsingMedia?.subitems)
        browsingMedia?.delegate = nil
        browsingMedia = nil
        browseListener = nil
        if let error {
            listener?.onError(message: error)
        } else {
            listener?.onListing(entries: entries)
        }
    }
#endif
}

#if canImport(VLCKit)
extension VlcKitNetworkBrowser: VLCMediaDelegate {
    func mediaDidFinishParsing(_ aMedia: VLCMedia) {
        guard aMedia === browsingMedia else { return }
        finishBrowse()
    }
}
#endif
