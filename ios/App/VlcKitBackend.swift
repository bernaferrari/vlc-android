//
//  VlcKitBackend.swift
//
//  Bridge from shared IosPlaybackService → MobileVLCKit.
//  Linked via SPM (see ios/project.yml). Auto-attached from App launch.
//

import Foundation
import UIKit
import VLCShared

#if canImport(MobileVLCKit)
import MobileVLCKit
#endif

/// Swift implementation of the Kotlin `VlcKitPlayerBackend` interface.
final class VlcKitBackend: NSObject, VlcKitPlayerBackend {
    static let shared = VlcKitBackend()

    private var listener: VlcKitPlayerBackendListener?
    /// Host view VLCKit draws video into (optional; audio-only still works).
    weak var drawableView: UIView?

#if canImport(MobileVLCKit)
    private var player: VLCMediaPlayer?
#endif

    private override init() {
        super.init()
    }

    /// Attach a UIView / UIViewController.view as the VLCKit drawable surface.
    func attachDrawable(_ view: UIView?) {
        drawableView = view
#if canImport(MobileVLCKit)
        if let view {
            player?.drawable = view
        }
#endif
    }

    func play(uri: String, title: String?) {
#if canImport(MobileVLCKit)
        if player == nil {
            let p = VLCMediaPlayer()
            p.delegate = self
            if let drawableView {
                p.drawable = drawableView
            }
            player = p
        }
        let url: URL?
        if uri.hasPrefix("/") {
            url = URL(fileURLWithPath: uri)
        } else {
            url = URL(string: uri) ?? URL(fileURLWithPath: uri)
        }
        guard let url else {
            listener?.onError(message: "Invalid URI: \(uri)")
            return
        }
        let media = VLCMedia(url: url)
        if let title, !title.isEmpty {
            media.addOption(":meta-title=\(title)")
        }
        // Prefer hardware decode when available
        media.addOption(":avcodec-hw=any")
        player?.media = media
        player?.play()
#else
        // Without VLCKit, report playing so shared state machine still advances in demos.
        listener?.onPlaying()
        listener?.onTimeChanged(timeMs: 0, lengthMs: 0)
#endif
    }

    func pause() {
#if canImport(MobileVLCKit)
        player?.pause()
#endif
        listener?.onPaused()
    }

    func resume() {
#if canImport(MobileVLCKit)
        player?.play()
#endif
        listener?.onPlaying()
    }

    func stop() {
#if canImport(MobileVLCKit)
        player?.stop()
#endif
        listener?.onStopped()
    }

    func seekTo(positionMs: Int64) {
#if canImport(MobileVLCKit)
        guard let player, player.isSeekable else {
            listener?.onTimeChanged(timeMs: positionMs, lengthMs: 0)
            return
        }
        let length = Int64(player.media?.length.intValue ?? 0)
        if length > 0 {
            player.position = Float(positionMs) / Float(length)
        } else {
            player.time = VLCTime(number: NSNumber(value: positionMs))
        }
        listener?.onTimeChanged(timeMs: positionMs, lengthMs: length)
#else
        listener?.onTimeChanged(timeMs: positionMs, lengthMs: 0)
#endif
    }

    func setVolume(volume: Int32) {
#if canImport(MobileVLCKit)
        // VLCKit audio.volume is 0...200
        player?.audio?.volume = Int(volume)
#endif
    }

    func setRate(rate: Float) {
#if canImport(MobileVLCKit)
        player?.rate = rate
#endif
    }

    func setListener(listener: VlcKitPlayerBackendListener?) {
        self.listener = listener
    }

    func release() {
#if canImport(MobileVLCKit)
        player?.stop()
        player?.delegate = nil
        player?.drawable = nil
        player = nil
#endif
        listener = nil
    }

    var isVlcKitLinked: Bool {
#if canImport(MobileVLCKit)
        true
#else
        false
#endif
    }
}

#if canImport(MobileVLCKit)
extension VlcKitBackend: VLCMediaPlayerDelegate {
    func mediaPlayerStateChanged(_ aNotification: Notification) {
        guard let player else { return }
        switch player.state {
        case .playing: listener?.onPlaying()
        case .paused: listener?.onPaused()
        case .stopped: listener?.onStopped()
        case .ended: listener?.onEnded()
        case .error: listener?.onError(message: "VLCKit error")
        default: break
        }
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard let player else { return }
        let time = Int64(player.time.intValue)
        let length = Int64(player.media?.length.intValue ?? 0)
        listener?.onTimeChanged(timeMs: time, lengthMs: length)
    }
}
#endif
