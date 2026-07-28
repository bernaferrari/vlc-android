//
//  VlcExternalDisplayController.swift
//
//  Owns only the UIKit output window for a connected display. Playback, queue,
//  controls, and library state remain in the shared KMP product.
//

import UIKit

/**
 * Retains the one native output window for a UIKit external-display scene.
 * The VLCKit player is still the shared KMP player; only its drawable moves.
 */
final class VlcExternalDisplayController {
    static let shared = VlcExternalDisplayController()

    private var externalWindow: UIWindow?

    func connect(windowScene: UIWindowScene) {
        guard externalWindow == nil else { return }
        let outputController = ExternalDisplayViewController()
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = outputController
        window.isHidden = false
        externalWindow = window
        VlcKitBackend.shared.attachExternalDrawable(view: outputController.view)
    }

    func disconnect() {
        VlcKitBackend.shared.attachExternalDrawable(view: nil)
        externalWindow?.isHidden = true
        externalWindow = nil
    }
}

final class VlcExternalDisplaySceneDelegate: NSObject, UIWindowSceneDelegate {
    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        VlcExternalDisplayController.shared.connect(windowScene: windowScene)
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        VlcExternalDisplayController.shared.disconnect()
    }
}

private final class ExternalDisplayViewController: UIViewController {
    override func loadView() {
        let view = UIView()
        view.backgroundColor = .black
        self.view = view
    }
}
