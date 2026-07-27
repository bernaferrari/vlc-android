//
//  IosMediaImportBridge.swift
//  VLC-iOS
//
//  Keeps UIKit picker presentation behind the shared shell's import action.
//

import UIKit
import VLCShared

final class IosMediaImportBridge: NSObject, IosMediaImportHandler {
    static let shared = IosMediaImportBridge()

    func presentMediaImport() {
        // The KMP callback is nonisolated; UIKit presentation must re-enter MainActor.
        Task { @MainActor in
            MediaImporter.shared.presentImportOptions()
        }
    }
}
