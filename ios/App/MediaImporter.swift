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

    // Kotlin typealiases are not exported to Objective-C/Swift; use the concrete public class.
    private let repo = IosMediaLibrary.companion.shared
    private var nextId: Int64 = 50_000

    // MARK: - Public API

    /// Invoked from the shared Compose top-bar action through [IosMediaImportHandler].
    /// The menu is native because Files/Photos permissions and presentation are UIKit-owned.
    func presentImportOptions() {
        guard let presenter = activePresenter() else { return }
        let sheet = UIAlertController(title: "Add media", message: nil, preferredStyle: .actionSheet)
        sheet.addAction(UIAlertAction(title: "Files", style: .default) { [weak self] _ in
            // Let UIKit finish dismissing the sheet before presenting another controller.
            DispatchQueue.main.async {
                guard let self, let active = self.activePresenter() else { return }
                self.presentDocumentPicker(from: active)
            }
        })
        sheet.addAction(UIAlertAction(title: "Photos", style: .default) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, let active = self.activePresenter() else { return }
                self.presentPhotosPicker(from: active)
            }
        })
        sheet.addAction(UIAlertAction(title: "Refresh library", style: .default) { [weak self] _ in
            self?.rescanLocalFolders()
        })
        sheet.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        if let popover = sheet.popoverPresentationController {
            popover.sourceView = presenter.view
            popover.sourceRect = presenter.view.bounds
        }
        presenter.present(sheet, animated: true)
    }

    /// Rescan app Documents (including Inbox) and merge into the shared library.
    func rescanLocalFolders() {
        var found: [MediaItem] = []
        let fm = FileManager.default
        let urls = [fm.urls(for: .documentDirectory, in: .userDomainMask).first].compactMap { $0 }

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
        // This is a complete Documents scan, so it can safely remove stale local
        // rows while retaining user metadata and never touching streams.
        repo.reconcileLocalDocuments(media: found)
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
        guard (try? url.resourceValues(forKeys: [.isRegularFileKey]))?.isRegularFile == true else {
            return nil
        }
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
            lastPlayed: 0,
            isFavorite: false,
            seen: 0,
            present: true,
            fileName: url.lastPathComponent,
            description: nil
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
                // Preserve every user-selected file. Replacing a same-named document silently
                // loses the earlier import and its shared-library metadata.
                let dest = uniqueDestination(for: url, in: docs)
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

    private func uniqueDestination(for source: URL, in directory: URL) -> URL {
        let fm = FileManager.default
        let base = source.deletingPathExtension().lastPathComponent
        let ext = source.pathExtension
        var suffix = 1
        var destination = directory.appendingPathComponent(source.lastPathComponent)
        while fm.fileExists(atPath: destination.path) {
            suffix += 1
            let name = ext.isEmpty ? "\(base) (\(suffix))" : "\(base) (\(suffix)).\(ext)"
            destination = directory.appendingPathComponent(name)
        }
        return destination
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

    private func activePresenter() -> UIViewController? {
        guard let root = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow })?
            .rootViewController else { return nil }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        return top
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
