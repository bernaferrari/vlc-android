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
    private var selectedEqualizerPresetId: String?
    private var videoCropMode = VideoCropMode.original

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

    /// Applies the shared Compose resize choice using MobileVLCKit's native video controls.
    /// A nil ratio resets LibVLC's forced aspect ratio; scale 0 asks it to fit its drawable.
    func setVideoOutput(aspectRatio: String?, scale: Float) {
#if canImport(MobileVLCKit)
        player?.scaleFactor = scale
        if let aspectRatio {
            aspectRatio.withCString { player?.videoAspectRatio = UnsafeMutablePointer(mutating: $0) }
        } else {
            player?.videoAspectRatio = nil
        }
#endif
    }

    func videoCrop() -> PlaybackVideoCrop {
#if canImport(MobileVLCKit)
        PlaybackVideoCrop(supported: true, mode: videoCropMode)
#else
        PlaybackVideoCrop()
#endif
    }

    func setVideoCrop(mode: VideoCropMode) {
#if canImport(MobileVLCKit)
        videoCropMode = mode
        if let geometry = mode.geometry {
            geometry.withCString { player?.videoCropGeometry = UnsafeMutablePointer(mutating: $0) }
        } else {
            player?.videoCropGeometry = nil
        }
#endif
    }

    func videoAdjust() -> PlaybackVideoAdjust {
#if canImport(MobileVLCKit)
        guard let filter = player?.adjustFilter else { return PlaybackVideoAdjust(supported: true) }
        return PlaybackVideoAdjust(
            supported: true,
            enabled: filter.enabled,
            brightness: filterFloat(filter.brightness, fallback: 1),
            contrast: filterFloat(filter.contrast, fallback: 1),
            hue: filterFloat(filter.hue, fallback: 0),
            saturation: filterFloat(filter.saturation, fallback: 1),
            gamma: filterFloat(filter.gamma, fallback: 1)
        )
#else
        return PlaybackVideoAdjust()
#endif
    }

    func setVideoAdjustEnabled(enabled: Bool) {
#if canImport(MobileVLCKit)
        player?.adjustFilter.enabled = enabled
#endif
    }

    func setVideoAdjust(parameter: VideoAdjustParameter, value: Float) {
#if canImport(MobileVLCKit)
        guard let filter = player?.adjustFilter else { return }
        let safe = min(parameter.maximum, max(parameter.minimum, value))
        switch parameter {
        case .brightness: filter.brightness.value = NSNumber(value: safe)
        case .contrast: filter.contrast.value = NSNumber(value: safe)
        case .hue: filter.hue.value = NSNumber(value: safe)
        case .saturation: filter.saturation.value = NSNumber(value: safe)
        case .gamma: filter.gamma.value = NSNumber(value: safe)
        default: break
        }
        filter.enabled = true
#endif
    }

    func resetVideoAdjust() {
#if canImport(MobileVLCKit)
        guard let filter = player?.adjustFilter else { return }
        filter.brightness.value = NSNumber(value: 1)
        filter.contrast.value = NSNumber(value: 1)
        filter.hue.value = NSNumber(value: 0)
        filter.saturation.value = NSNumber(value: 1)
        filter.gamma.value = NSNumber(value: 1)
        filter.enabled = false
#endif
    }

    func tracks() -> PlaybackTracks {
#if canImport(MobileVLCKit)
        guard let player else { return PlaybackTracks(audio: [], subtitles: []) }
        return PlaybackTracks(
            audio: makeTracks(
                names: player.audioTrackNames,
                indexes: player.audioTrackIndexes,
                selected: Int(player.currentAudioTrackIndex)
            ),
            subtitles: makeTracks(
                names: player.videoSubTitlesNames,
                indexes: player.videoSubTitlesIndexes,
                selected: Int(player.currentVideoSubTitleIndex)
            )
        )
#else
        return PlaybackTracks(audio: [], subtitles: [])
#endif
    }

    func selectAudioTrack(id: String) {
#if canImport(MobileVLCKit)
        guard let trackID = Int32(id) else { return }
        player?.currentAudioTrackIndex = trackID
#endif
    }

    func selectSubtitleTrack(id: String) {
#if canImport(MobileVLCKit)
        guard let trackID = Int32(id) else { return }
        player?.currentVideoSubTitleIndex = trackID
#endif
    }

    func delays() -> PlaybackDelays {
#if canImport(MobileVLCKit)
        return PlaybackDelays(
            audioUs: Int64(player?.currentAudioPlaybackDelay ?? 0),
            subtitleUs: Int64(player?.currentVideoSubTitleDelay ?? 0),
            supported: true
        )
#else
        return PlaybackDelays(audioUs: 0, subtitleUs: 0, supported: false)
#endif
    }

    func setAudioDelay(delayUs: Int64) {
#if canImport(MobileVLCKit)
        player?.currentAudioPlaybackDelay = Int(delayUs)
#endif
    }

    func setSubtitleDelay(delayUs: Int64) {
#if canImport(MobileVLCKit)
        player?.currentVideoSubTitleDelay = Int(delayUs)
#endif
    }

    func chapters() -> PlaybackChapters {
#if canImport(MobileVLCKit)
        guard let player else { return PlaybackChapters(entries: []) }
        let selected = Int(player.currentChapterIndex)
        let count = max(0, Int(player.numberOfChapters(forTitle: -1)))
        return PlaybackChapters(entries: (0..<count).map { index in
            PlaybackChapter(index: Int32(index), title: "Chapter \(index + 1)", positionMs: 0, selected: index == selected)
        })
#else
        return PlaybackChapters(entries: [])
#endif
    }

    func selectChapter(index: Int32) {
#if canImport(MobileVLCKit)
        player?.currentChapterIndex = index
#endif
    }

    func loadExternalSubtitle(uri: String) -> Bool {
#if canImport(MobileVLCKit)
        guard let url = URL(string: uri) ?? URL(string: uri.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "") else { return false }
        return player?.addPlaybackSlave(url, type: VLCMediaPlaybackSlaveTypeSubtitle, enforce: true) == 0
#else
        return false
#endif
    }

    func equalizer() -> PlaybackEqualizer {
#if canImport(MobileVLCKit)
        let active = player?.equalizer
        let presets = VLCAudioEqualizer.presets.compactMap { $0 as? VLCAudioEqualizer.Preset }.map {
            PlaybackEqualizerPreset(id: String($0.index), label: $0.name)
        }
        let bands = active?.bands.compactMap { $0 as? VLCAudioEqualizer.Band }.map {
            PlaybackEqualizerBand(index: Int32($0.index), label: formatFrequency($0.frequency), amplificationDb: $0.amplification)
        } ?? []
        return PlaybackEqualizer(
            supported: true,
            enabled: active != nil,
            presets: presets,
            selectedPresetId: selectedEqualizerPresetId,
            preampDb: active?.preAmplification ?? 0,
            bands: bands
        )
#else
        return PlaybackEqualizer()
#endif
    }

    func setEqualizerEnabled(enabled: Bool) {
#if canImport(MobileVLCKit)
        if enabled {
            let equalizer = player?.equalizer ?? VLCAudioEqualizer()
            player?.equalizer = equalizer
        } else {
            player?.equalizer = nil
            selectedEqualizerPresetId = nil
        }
#endif
    }

    func selectEqualizerPreset(id: String) {
#if canImport(MobileVLCKit)
        guard let index = UInt(id),
              let preset = VLCAudioEqualizer.presets.compactMap({ $0 as? VLCAudioEqualizer.Preset }).first(where: { $0.index == index }) else { return }
        player?.equalizer = VLCAudioEqualizer(preset: preset)
        selectedEqualizerPresetId = id
#endif
    }

    func setEqualizerPreamp(preampDb: Float) {
#if canImport(MobileVLCKit)
        let equalizer = player?.equalizer ?? VLCAudioEqualizer()
        equalizer.preAmplification = min(20, max(-20, preampDb))
        player?.equalizer = equalizer
        selectedEqualizerPresetId = nil
#endif
    }

    func setEqualizerBand(index: Int32, amplificationDb: Float) {
#if canImport(MobileVLCKit)
        let equalizer = player?.equalizer ?? VLCAudioEqualizer()
        guard index >= 0, Int(index) < equalizer.bands.count,
              let band = equalizer.bands[Int(index)] as? VLCAudioEqualizer.Band else { return }
        band.amplification = min(20, max(-20, amplificationDb))
        player?.equalizer = equalizer
        selectedEqualizerPresetId = nil
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
    private func makeTracks(names: NSArray, indexes: NSArray, selected: Int) -> [PlaybackTrack] {
        let labels = names.compactMap { $0 as? String }
        let ids = indexes.map { String(describing: $0) }
        return zip(labels, ids).map { label, id in
            PlaybackTrack(id: id, label: label, selected: Int(id) == selected)
        }
    }

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

    private func formatFrequency(_ frequency: Float) -> String {
        frequency >= 1_000 ? String(format: "%.1f kHz", frequency / 1_000) : String(format: "%.0f Hz", frequency)
    }

#if canImport(MobileVLCKit)
    private func filterFloat(_ parameter: VLCFilterParameter, fallback: Float) -> Float {
        (parameter.value as? NSNumber)?.floatValue ?? fallback
    }
#endif

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
