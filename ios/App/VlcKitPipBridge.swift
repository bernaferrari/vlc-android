//
//  VlcKitPipBridge.swift
//
//  One VLCKit drawable serves both the Compose video surface and the iOS PiP
//  controller. This is the same public VLCPictureInPictureDrawable contract
//  used by upstream VLC iOS, not an AVPlayer or private-API workaround.
//

import UIKit
import VLCShared

#if canImport(VLCKit)
import VLCKit

final class VlcKitPipDrawable: NSObject,
    VLCDrawable,
    VLCPictureInPictureDrawable,
    VLCPictureInPictureMediaControlling,
    IosPipHandler {

    static let shared = VlcKitPipDrawable()

    private weak var backend: VlcKitBackend?
    private weak var hostView: UIView?
    private weak var videoView: UIView?
    private var pipController: VLCPictureInPictureWindowControlling?
    private var pipActive = false

    func attach(backend: VlcKitBackend) {
        self.backend = backend
    }

    func updateHostView(_ hostView: UIView?) {
        self.hostView = hostView
        guard let videoView, let hostView else { return }
        videoView.removeFromSuperview()
        videoView.frame = hostView.bounds
        videoView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        hostView.addSubview(videoView)
    }

    // MARK: VLCDrawable

    func addSubview(_ view: UIView) {
        videoView?.removeFromSuperview()
        videoView = view
        guard let hostView else { return }
        view.frame = hostView.bounds
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        hostView.addSubview(view)
    }

    func bounds() -> CGRect { hostView?.bounds ?? .zero }

    // MARK: VLCPictureInPictureDrawable

    func mediaController() -> (any VLCPictureInPictureMediaControlling)! { self }

    func pictureInPictureReady() -> (((any VLCPictureInPictureWindowControlling)?) -> Void)! {
        { [weak self] controller in
            guard let controller else { return }
            self?.pipController = controller
            controller.stateChangeEventHandler = { [weak self] active in
                self?.pipActive = active
            }
        }
    }

    // MARK: VLCPictureInPictureMediaControlling

    func play() { backend?.pipPlay() }
    func pause() { backend?.pipPause() }
    func seek(by offset: Int64, completion: (() -> Void)!) {
        backend?.pipSeek(by: offset, completion: completion ?? {})
    }
    func mediaLength() -> Int64 { backend?.pipMediaLength() ?? 0 }
    func mediaTime() -> Int64 { backend?.pipMediaTime() ?? 0 }
    func isMediaSeekable() -> Bool { backend?.pipIsSeekable() ?? false }
    func isMediaPlaying() -> Bool { backend?.pipIsPlaying() ?? false }

    // MARK: shared Kotlin PipController adapter

    var isSupported: Bool { true }
    func enter() -> Bool {
        guard let pipController else { return false }
        pipController.startPictureInPicture()
        return true
    }
    func exit() { pipController?.stopPictureInPicture() }
    func isActive() -> Bool { pipActive }
    func invalidatePlaybackState() { pipController?.invalidatePlaybackState() }
}
#endif
