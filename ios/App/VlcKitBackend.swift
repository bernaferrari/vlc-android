//
//  VlcKitBackend.swift
//
//  Template bridge from shared IosPlaybackService → MobileVLCKit.
//  Enable once MobileVLCKit is linked in the Xcode project:
//    1. SPM/CocoaPods: MobileVLCKit
//    2. Uncomment the VLCKit code paths below
//    3. On launch: IosPlaybackService.shared.setBackend(backend: VlcKitBackend())
//

import Foundation
import VLCShared

#if canImport(MobileVLCKit)
import MobileVLCKit
#endif

/// Swift implementation of the Kotlin `VlcKitPlayerBackend` interface.
final class VlcKitBackend: NSObject, VlcKitPlayerBackend {
    private var listener: VlcKitPlayerBackendListener?

#if canImport(MobileVLCKit)
    private var player: VLCMediaPlayer?
#endif

    func play(uri: String, title: String?) {
#if canImport(MobileVLCKit)
        if player == nil {
            player = VLCMediaPlayer()
            player?.delegate = self
        }
        guard let url = URL(string: uri) ?? URL(fileURLWithPath: uri) as URL? else {
            listener?.onError(message: "Invalid URI: \(uri)")
            return
        }
        let media = VLCMedia(url: url)
        if let title { media.addOption(":meta-title=\(title)") }
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
        guard let player, player.isSeekable else { return }
        let length = player.media?.length.intValue ?? 0
        if length > 0 {
            player.position = Float(positionMs) / Float(length)
        }
#endif
        listener?.onTimeChanged(timeMs: positionMs, lengthMs: 0)
    }

    func setVolume(volume: Int32) {
#if canImport(MobileVLCKit)
        // VLCKit audio.volume is 0...200
        player?.audio?.volume = volume
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
        player = nil
#endif
        listener = nil
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
