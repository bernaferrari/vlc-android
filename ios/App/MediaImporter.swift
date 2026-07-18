//
//  MediaImporter.swift
//
//  Full medialibrary intake for iOS:
//  - Files app / document picker
//  - Photos library (video + audio when available)
//  - Inbox / Documents directory rescans
//

import Foundation
import Photos
import PhotosUI
import UniformTypeIdentifiers
import UIKit
import VLCShared

@MainActor
final class MediaImporter: NSObject {
    static let shared = MediaImporter()

    private let repo = IosMediaRepository.shared
    private var nextId: Int64 = 50_000

    // MARK: - Public API

    /// Rescan app Documents + Inbox and merge into the shared library.
    func rescanLocalFolders() {
        var found: [MediaItem] = []
        let fm = FileManager.default
        let urls = [
            fm.urls(for: .documentDirectory, in: .userDomainMask).first,
            fm.urls(for: .cachesDirectory, in: .userDomainMask).first
        ].compactMap { $0 }

        for root in urls {
            guard let enumerator = fm.enumerator(
                at: root,
                includingPropertiesForKeys: [.isRegularFileKey, .fileSizeKey, .contentModificationDateKey],
                options: [.skipsHiddenFiles]
            ) else { continue }
            for case let fileURL as URL in enumerator {
                guard let item = mediaItem(fromFileURL: fileURL) else { continue }
                found.append(item)
            }
        }
        merge(found)
    }

    /// Present document picker for multi-select of audiovisual files.
    func presentDocumentPicker(from presenter: UIViewController) {
        var types: [UTType] = [.movie, .video, .audio, .mpeg4Movie, .mp3, .mpeg4Audio, .avi, .wav]
        if let mkv = UTType(filenameExtension: "mkv") { types.append(mkv) }
        if let flac = UTType(filenameExtension: "flac") { types.append(flac) }
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: types, asCopy: true)
        picker.allowsMultipleSelection = true
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    /// Present Photos picker (iOS 16+ PHPicker).
    func presentPhotosPicker(from presenter: UIViewController) {
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.filter = .any(of: [.videos, .livePhotos])
        config.selectionLimit = 0 // unlimited
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    // MARK: - Internals

    private func mediaItem(fromFileURL url: URL) -> MediaItem? {
        let ext = url.pathExtension.lowercased()
        let type: MediaType
        switch ext {
        case "mp4", "m4v", "mov", "mkv", "avi", "webm", "ts", "m2ts", "mpg", "mpeg", "3gp":
            type = .video
        case "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "aiff":
            type = .audio
        default:
            return nil
        }
        let values = try? url.resourceValues(forKeys: [.fileSizeKey, .contentModificationDateKey])
        let id = nextId
        nextId += 1
        return MediaItem(
            id: id,
            title: url.deletingPathExtension().lastPathComponent,
            uri: url.absoluteString,
            type: type,
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
            lastModified: Int64((values?.contentModificationDate?.timeIntervalSince1970 ?? 0) * 1000),
            size: Int64(values?.fileSize ?? 0),
            rating: 0,
            playedCount: 0,
            lastPlayed: 0
        )
    }

    private func merge(_ items: [MediaItem]) {
        guard !items.isEmpty else { return }
        // Prefer upsert to keep existing entries
        for item in items {
            repo.upsert(media: item)
        }
    }

    private func importSecurityScoped(_ urls: [URL]) {
        var imported: [MediaItem] = []
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        for url in urls {
            let accessed = url.startAccessingSecurityScopedResource()
            defer { if accessed { url.stopAccessingSecurityScopedResource() } }
            // Copy into Documents so library URIs remain valid after picker dismiss
            if let docs {
                let dest = docs.appendingPathComponent(url.lastPathComponent)
                try? FileManager.default.removeItem(at: dest)
                do {
                    try FileManager.default.copyItem(at: url, to: dest)
                    if let item = mediaItem(fromFileURL: dest) {
                        imported.append(item)
                    }
                } catch {
                    if let item = mediaItem(fromFileURL: url) {
                        imported.append(item)
                    }
                }
            } else if let item = mediaItem(fromFileURL: url) {
                imported.append(item)
            }
        }
        merge(imported)
    }

    private func importPhotoResults(_ results: [PHPickerResult]) {
        for result in results {
            if result.itemProvider.hasItemConformingToTypeIdentifier(UTType.movie.identifier) {
                result.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.movie.identifier) { [weak self] url, _ in
                    guard let self, let url else { return }
                    let tmp = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString + "-" + url.lastPathComponent)
                    try? FileManager.default.copyItem(at: url, to: tmp)
                    Task { @MainActor in
                        self.importSecurityScoped([tmp])
                    }
                }
            }
        }
    }
}

extension MediaImporter: UIDocumentPickerDelegate {
    nonisolated func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        Task { @MainActor in
            self.importSecurityScoped(urls)
        }
    }
}

extension MediaImporter: PHPickerViewControllerDelegate {
    nonisolated func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        Task { @MainActor in
            picker.dismiss(animated: true)
            self.importPhotoResults(results)
        }
    }
}
