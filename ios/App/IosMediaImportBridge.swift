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
        MediaImporter.shared.presentImportOptions()
    }
}
