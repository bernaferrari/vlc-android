//
//  VlcExternalDisplayController.swift
//
//  Owns only the UIKit output window for a connected display. Playback, queue,
//  controls, and library state remain in the shared KMP product.
//

import UIKit

final class VlcExternalDisplayController: NSObject {
    static let shared = VlcExternalDisplayController()

    private var externalWindow: UIWindow?
    private var started = false

    func start() {
        guard !started else { return }
        started = true
        let center = NotificationCenter.default
        center.addObserver(
            self,
            selector: #selector(screenDidConnect(_:)),
            name: UIScreen.didConnectNotification,
            object: nil
        )
        center.addObserver(
            self,
            selector: #selector(screenDidDisconnect(_:)),
            name: UIScreen.didDisconnectNotification,
            object: nil
        )
        if let screen = UIScreen.screens.first(where: { $0 !== UIScreen.main }) {
            show(screen: screen)
        }
    }

    @objc private func screenDidConnect(_ notification: Notification) {
        let screen = notification.object as? UIScreen
            ?? UIScreen.screens.first(where: { $0 !== UIScreen.main })
        guard let screen else { return }
        show(screen: screen)
    }

    @objc private func screenDidDisconnect(_ notification: Notification) {
        hide()
    }

    private func show(screen: UIScreen) {
        guard externalWindow == nil else { return }
        let outputController = ExternalDisplayViewController()
        let window = UIWindow(frame: screen.bounds)
        window.screen = screen
        window.rootViewController = outputController
        window.isHidden = false
        externalWindow = window
        VlcKitBackend.shared.attachExternalDrawable(view: outputController.view)
    }

    private func hide() {
        VlcKitBackend.shared.attachExternalDrawable(view: nil)
        externalWindow?.isHidden = true
        externalWindow = nil
    }
}

private final class ExternalDisplayViewController: UIViewController {
    override func loadView() {
        let view = UIView()
        view.backgroundColor = .black
        self.view = view
    }
}
