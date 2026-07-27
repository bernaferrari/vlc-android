//
//  VlcCarPlaySceneDelegate.swift
//
//  CarPlay is an OS-owned template surface. Its catalog and playback commands
//  are intentionally supplied by the shared KMP library rather than a third
//  media implementation.
//

import CarPlay
import VLCShared

final class VlcCarPlaySceneDelegate: NSObject, CPTemplateApplicationSceneDelegate {
    private weak var interfaceController: CPInterfaceController?

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController
    ) {
        self.interfaceController = interfaceController
        showLibrary(in: interfaceController)
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnectInterfaceController interfaceController: CPInterfaceController
    ) {
        self.interfaceController = nil
    }

    private func showLibrary(in interfaceController: CPInterfaceController) {
        let items = IosVehicleMediaBridge.shared.audioItems()
        let rows = items.prefix(100).map { media -> CPListItem in
            let detail = [media.artist, media.album]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
                .joined(separator: " • ")
            let row = CPListItem(text: media.displayTitle, detailText: detail.isEmpty ? nil : detail)
            row.handler = { _, completion in
                _ = IosVehicleMediaBridge.shared.playAudioItem(id: media.id)
                completion()
            }
            return row
        }
        let section = CPListSection(items: rows, header: "Audio", sectionIndexTitle: nil)
        let template = CPListTemplate(title: "VLC", sections: [section])
        interfaceController.setRootTemplate(template, animated: false, completion: nil)
    }
}
