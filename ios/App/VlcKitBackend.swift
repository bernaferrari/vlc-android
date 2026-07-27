//
//  VlcKitBackend.swift
//
//  Bridge from shared IosPlaybackService → MobileVLCKit.
//  Linked via SPM (see ios/project.yml). Auto-attached from App launch.
//

import AVFoundation
import Foundation
import MediaPlayer
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
    private var resumeAfterInterruption = false
    private var currentTitle = ""
    private var configuredVolume: Int32 = 100
    private var configuredRate: Float = 1

#if canImport(MobileVLCKit)
    private var player: VLCMediaPlayer?
#endif

    private override init() {
        super.init()
        let notifications = NotificationCenter.default
        notifications.addObserver(
            self,
            selector: #selector(handleAudioInterruption(_:)),
            name: AVAudioSession.interruptionNotification,
            object: nil
        )
        notifications.addObserver(
            self,
            selector: #selector(handleAudioRouteChange(_:)),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
        configureRemoteCommands()
    }

    /// Attach the Compose-owned UIView as VLCKit's drawable surface.
    func attachDrawable(view: UIView?) {
        drawableView = view
#if canImport(MobileVLCKit)
        if let view {
            player?.drawable = view
        } else {
            player?.drawable = nil
        }
#endif
    }

    func play(uri: String, title: String?) {
#if canImport(MobileVLCKit)
        activateAudioSession()
        guard let media = makeMedia(uri: uri, title: title) else {
            listener?.onError(message: "Invalid URI: \(uri)")
            return
        }
        let player = ensurePlayer()
        player.media = media
        player.play()
        publishNowPlayingInfo(isPlaying: true)
#else
        // A shipping build must not simulate success when its decoder package is
        // absent: surface an actionable error to the shared player instead.
        listener?.onError(message: "MobileVLCKit is unavailable in this build.")
#endif
    }

    func preparePaused(uri: String, title: String?, positionMs: Int64) -> Bool {
#if canImport(MobileVLCKit)
        guard let media = makeMedia(uri: uri, title: title) else { return false }
        let player = ensurePlayer()
        player.media = media
        // VLCKit honours the requested time when play is later initiated. Crucially, we do not
        // call play/pause here, so restoring the KMP queue can never produce an audio blip.
        player.time = VLCTime(number: NSNumber(value: max(0, positionMs)))
        publishNowPlayingInfo(isPlaying: false)
        return true
#else
        return false
#endif
    }

    func pause() {
#if canImport(MobileVLCKit)
        player?.pause()
        publishNowPlayingInfo(isPlaying: false)
        listener?.onPaused()
#endif
    }

    func resume() {
#if canImport(MobileVLCKit)
        activateAudioSession()
        player?.play()
        publishNowPlayingInfo(isPlaying: true)
        listener?.onPlaying()
#endif
    }

    func stop() {
#if canImport(MobileVLCKit)
        player?.stop()
#endif
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        deactivateAudioSession()
        listener?.onStopped()
    }

    func seekTo(positionMs: Int64) {
#if canImport(MobileVLCKit)
        // A live/non-seekable input must not make the shared slider pretend it moved.
        guard let player, player.isSeekable else { return }
        let length = max(0, Int64(player.media?.length.intValue ?? 0))
        let target = max(0, length > 0 ? min(positionMs, length) : positionMs)
        if length > 0 {
            player.position = Float(target) / Float(length)
        } else {
            player.time = VLCTime(number: NSNumber(value: target))
        }
        listener?.onTimeChanged(timeMs: target, lengthMs: length)
        publishNowPlayingInfo()
#endif
    }

    func setVolume(volume: Int32) {
        configuredVolume = min(200, max(0, volume))
#if canImport(MobileVLCKit)
        // VLCKit audio.volume is 0...200
        player?.audio?.volume = configuredVolume
#endif
    }

    func getVolume() -> Int32 {
#if canImport(MobileVLCKit)
        player?.audio?.volume ?? configuredVolume
#else
        configuredVolume
#endif
    }

    func setRate(rate: Float) {
        configuredRate = min(4, max(0.25, rate))
#if canImport(MobileVLCKit)
        player?.rate = configuredRate
        publishNowPlayingInfo()
#endif
    }

    func getRate() -> Float {
#if canImport(MobileVLCKit)
        player?.rate ?? configuredRate
#else
        configuredRate
#endif
    }

    func setListener(listener: VlcKitPlayerBackendListener?) {
        self.listener = listener
    }

    func dispose() {
#if canImport(MobileVLCKit)
        player?.stop()
        player?.delegate = nil
        player?.drawable = nil
        player = nil
#endif
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        deactivateAudioSession()
        listener = nil
    }

    var isVlcKitLinked: Bool {
#if canImport(MobileVLCKit)
        true
#else
        false
#endif
    }

    // MARK: - iOS audio / lock-screen integration

    private func activateAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .moviePlayback, options: [.allowAirPlay, .allowBluetoothA2DP])
            try session.setActive(true)
        } catch {
            NSLog("VLC could not activate the audio session: %@", error.localizedDescription)
        }
    }

#if canImport(MobileVLCKit)
    private func ensurePlayer() -> VLCMediaPlayer {
        if let player { return player }
        let player = VLCMediaPlayer()
        player.delegate = self
        if let drawableView {
            player.drawable = drawableView
        }
        player.audio?.volume = configuredVolume
        player.rate = configuredRate
        self.player = player
        return player
    }

    private func makeMedia(uri: String, title: String?) -> VLCMedia? {
        let url: URL?
        if uri.hasPrefix("/") {
            url = URL(fileURLWithPath: uri)
        } else {
            url = URL(string: uri) ?? URL(fileURLWithPath: uri)
        }
        guard let url else { return nil }
        currentTitle = title?.isEmpty == false ? title! : url.deletingPathExtension().lastPathComponent
        let media = VLCMedia(url: url)
        if let title, !title.isEmpty {
            media.addOption(":meta-title=\(title)")
        }
        // Prefer hardware decode when available.
        media.addOption(":avcodec-hw=any")
        return media
    }
#endif

    private func deactivateAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // A route may already have deactivated the session; never turn a normal stop into an
            // apparent playback failure.
            NSLog("VLC could not deactivate the audio session: %@", error.localizedDescription)
        }
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
            self.resume()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
            self.pause()
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
#if canImport(MobileVLCKit)
            self.player?.isPlaying == true ? self.pause() : self.resume()
            return .success
#else
            return .commandFailed
#endif
        }
        center.stopCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
            self.stop()
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            guard let self, self.listener != nil else { return .commandFailed }
            IosPlaybackService.companion.shared.next()
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            guard let self, self.listener != nil else { return .commandFailed }
            IosPlaybackService.companion.shared.previous()
            return .success
        }
        center.skipForwardCommand.preferredIntervals = [15]
        center.skipForwardCommand.addTarget { [weak self] event in
            guard let self, let event = event as? MPSkipIntervalCommandEvent else { return .commandFailed }
            self.seekRelative(milliseconds: Int64(event.interval * 1_000))
            return .success
        }
        center.skipBackwardCommand.preferredIntervals = [15]
        center.skipBackwardCommand.addTarget { [weak self] event in
            guard let self, let event = event as? MPSkipIntervalCommandEvent else { return .commandFailed }
            self.seekRelative(milliseconds: -Int64(event.interval * 1_000))
            return .success
        }
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let self, let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            self.seekTo(positionMs: Int64(event.positionTime * 1_000))
            return .success
        }
        center.changePlaybackRateCommand.supportedPlaybackRates = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2]
        center.changePlaybackRateCommand.addTarget { [weak self] event in
            guard let self, let event = event as? MPChangePlaybackRateCommandEvent else { return .commandFailed }
            self.setRate(rate: event.playbackRate)
            return .success
        }
    }

    private func seekRelative(milliseconds: Int64) {
#if canImport(MobileVLCKit)
        let current = Int64(player?.time.intValue ?? 0)
        seekTo(positionMs: max(0, current + milliseconds))
#endif
    }

    private func publishNowPlayingInfo(isPlaying: Bool? = nil) {
#if canImport(MobileVLCKit)
        guard let player, player.media != nil else { return }
        let length = max(0, Int64(player.media?.length.intValue ?? 0))
        let time = max(0, Int64(player.time.intValue))
        let playing = isPlaying ?? player.isPlaying
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPMediaItemPropertyTitle] = currentTitle
        if length > 0 {
            info[MPMediaItemPropertyPlaybackDuration] = Double(length) / 1_000
        }
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = Double(time) / 1_000
        info[MPNowPlayingInfoPropertyPlaybackRate] = playing ? player.rate : 0
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = player.rate
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
#endif
    }

    @objc private func handleAudioInterruption(_ notification: Notification) {
#if canImport(MobileVLCKit)
        guard let rawType = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return }
        switch type {
        case .began:
            resumeAfterInterruption = player?.isPlaying == true
            if resumeAfterInterruption { player?.pause() }
        case .ended:
            let rawOptions = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            let options = AVAudioSession.InterruptionOptions(rawValue: rawOptions)
            if resumeAfterInterruption && options.contains(.shouldResume) {
                activateAudioSession()
                player?.play()
            }
            resumeAfterInterruption = false
        @unknown default:
            break
        }
#endif
    }

    @objc private func handleAudioRouteChange(_ notification: Notification) {
#if canImport(MobileVLCKit)
        guard let rawReason = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: rawReason) else { return }
        // Never blast sound through the speaker after headphones / an external route disappear.
        if reason == .oldDeviceUnavailable && player?.isPlaying == true {
            player?.pause()
        }
#endif
    }
}

#if canImport(MobileVLCKit)
extension VlcKitBackend: VLCMediaPlayerDelegate {
    func mediaPlayerStateChanged(_ aNotification: Notification) {
        guard let player else { return }
        switch player.state {
        case .playing:
            publishNowPlayingInfo(isPlaying: true)
            listener?.onPlaying()
        case .paused:
            publishNowPlayingInfo(isPlaying: false)
            listener?.onPaused()
        case .stopped:
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            listener?.onStopped()
        case .ended:
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            listener?.onEnded()
        case .error: listener?.onError(message: "VLCKit error")
        default: break
        }
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard let player else { return }
        let time = Int64(player.time.intValue)
        let length = Int64(player.media?.length.intValue ?? 0)
        listener?.onTimeChanged(timeMs: time, lengthMs: length)
        publishNowPlayingInfo()
    }
}
#endif
