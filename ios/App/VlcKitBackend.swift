//
//  VlcKitBackend.swift
//
//  Bridge from shared IosPlaybackService → VLCKit.
//  Linked via SPM (see ios/project.yml). Auto-attached from App launch.
//

import AVFoundation
import Foundation
import MediaPlayer
import UIKit
import VLCShared

#if canImport(VLCKit)
import VLCKit
#endif

/// Swift implementation of the Kotlin `VlcKitPlayerBackend` interface.
final class VlcKitBackend: NSObject, VlcKitPlayerBackend, VlcKitRendererBackend {
    static let shared = VlcKitBackend()

    private var listener: VlcKitPlayerBackendListener?
    /// Compose-owned host view. It remains the fallback while no external display is attached.
    weak var drawableView: UIView?
    /// Native external-display host. It takes precedence without changing shared player state.
    weak var externalDrawableView: UIView?
    private var resumeAfterInterruption = false
    private var currentTitle = ""
    private var configuredVolume: Int32 = 100
    private var configuredRate: Float = 1
    private var selectedEqualizerPresetId: String?
    private var videoCropMode = VideoCropMode.original
    private var videoOutputAspectRatio: String?
    private var videoOutputScale: Float = 0
    private var externalSubtitleURL: URL?
    private var selectedRendererId: String?

#if canImport(VLCKit)
    private var player: VLCMediaPlayer?
    private var rendererDiscoverers: [VLCRendererDiscoverer] = []
    private var rendererItems: [String: VLCRendererItem] = [:]
    private let pipDrawable = VlcKitPipDrawable.shared
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
#if canImport(VLCKit)
        updateDrawable()
#endif
    }

    /// Route video to a connected physical display while retaining Compose as the fallback.
    /// This deliberately lives in the decoder island: no duplicate external-display screen or
    /// playback state is created outside the common KMP controller.
    func attachExternalDrawable(view: UIView?) {
        externalDrawableView = view
#if canImport(VLCKit)
        updateDrawable()
#endif
    }

    func play(uri: String, title: String?) {
#if canImport(VLCKit)
        activateAudioSession()
        guard let media = makeMedia(uri: uri, title: title) else {
            listener?.onError(message: "Invalid URI: \(uri)")
            return
        }
        externalSubtitleURL = nil
        let player = ensurePlayer()
        player.media = media
        player.play()
        publishNowPlayingInfo(isPlaying: true)
#else
        // A shipping build must not simulate success when its decoder package is
        // absent: surface an actionable error to the shared player instead.
        listener?.onError(message: "VLCKit is unavailable in this build.")
#endif
    }

    func preparePaused(uri: String, title: String?, positionMs: Int64) -> Bool {
#if canImport(VLCKit)
        guard let media = makeMedia(uri: uri, title: title) else { return false }
        externalSubtitleURL = nil
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
#if canImport(VLCKit)
        player?.pause()
        publishNowPlayingInfo(isPlaying: false)
        listener?.onPaused()
#endif
    }

    func resume() {
#if canImport(VLCKit)
        activateAudioSession()
        player?.play()
        publishNowPlayingInfo(isPlaying: true)
        listener?.onPlaying()
#endif
    }

    func stop() {
#if canImport(VLCKit)
        player?.stop()
#endif
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        deactivateAudioSession()
        listener?.onStopped()
    }

    func seekTo(positionMs: Int64) {
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        // VLCKit audio.volume is 0...200
        player?.audio?.volume = configuredVolume
#endif
    }

    func getVolume() -> Int32 {
#if canImport(VLCKit)
        player?.audio?.volume ?? configuredVolume
#else
        configuredVolume
#endif
    }

    func setRate(rate: Float) {
        configuredRate = min(4, max(0.25, rate))
#if canImport(VLCKit)
        player?.rate = configuredRate
        publishNowPlayingInfo()
#endif
    }

    func getRate() -> Float {
#if canImport(VLCKit)
        player?.rate ?? configuredRate
#else
        configuredRate
#endif
    }

    /// Applies the shared Compose resize choice using VLCKit's native video controls.
    /// A nil ratio resets LibVLC's forced aspect ratio; scale 0 asks it to fit its drawable.
    func setVideoOutput(aspectRatio: String?, scale: Float) {
        videoOutputAspectRatio = aspectRatio
        videoOutputScale = scale
#if canImport(VLCKit)
        applyVideoOutput(to: player)
#endif
    }

    func videoCrop() -> PlaybackVideoCrop {
#if canImport(VLCKit)
        PlaybackVideoCrop(supported: true, mode: videoCropMode)
#else
        PlaybackVideoCrop()
#endif
    }

    func setVideoCrop(mode: VideoCropMode) {
        videoCropMode = mode
#if canImport(VLCKit)
        applyVideoCrop(to: player)
#endif
    }

    func videoAdjust() -> PlaybackVideoAdjust {
#if canImport(VLCKit)
        guard let filter = player?.adjustFilter else {
            return PlaybackVideoAdjust(
                supported: true, enabled: false, brightness: 1, contrast: 1,
                hue: 0, saturation: 1, gamma: 1
            )
        }
        return PlaybackVideoAdjust(
            supported: true,
            enabled: filter.isEnabled,
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
#if canImport(VLCKit)
        player?.adjustFilter.isEnabled = enabled
#endif
    }

    func setVideoAdjust(parameter: VideoAdjustParameter, value: Float) {
#if canImport(VLCKit)
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
        filter.isEnabled = true
#endif
    }

    func resetVideoAdjust() {
#if canImport(VLCKit)
        guard let filter = player?.adjustFilter else { return }
        filter.brightness.value = NSNumber(value: 1)
        filter.contrast.value = NSNumber(value: 1)
        filter.hue.value = NSNumber(value: 0)
        filter.saturation.value = NSNumber(value: 1)
        filter.gamma.value = NSNumber(value: 1)
        filter.isEnabled = false
#endif
    }

    func tracks() -> PlaybackTracks {
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        guard let trackID = Int32(id) else { return }
        player?.currentAudioTrackIndex = trackID
#endif
    }

    func selectSubtitleTrack(id: String) {
#if canImport(VLCKit)
        guard let trackID = Int32(id) else { return }
        player?.currentVideoSubTitleIndex = trackID
#endif
    }

    func delays() -> PlaybackDelays {
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        player?.currentAudioPlaybackDelay = Int(delayUs)
#endif
    }

    func setSubtitleDelay(delayUs: Int64) {
#if canImport(VLCKit)
        player?.currentVideoSubTitleDelay = Int(delayUs)
#endif
    }

    func chapters() -> PlaybackChapters {
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        player?.currentChapterIndex = index
#endif
    }

    func loadExternalSubtitle(uri: String) -> Bool {
#if canImport(VLCKit)
        guard let url = URL(string: uri) ?? URL(string: uri.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "") else { return false }
        let loaded = player?.addPlaybackSlave(url, type: .subtitle, enforce: true) == 0
        if loaded { externalSubtitleURL = url }
        return loaded
#else
        return false
#endif
    }

    func equalizer() -> PlaybackEqualizer {
#if canImport(VLCKit)
        let active = player?.equalizer
        let presets = VLCAudioEqualizer.presets.map {
            PlaybackEqualizerPreset(id: String($0.index), label: $0.name)
        }
        let bands = active?.bands.map {
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
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        guard let index = UInt(id),
              let preset = VLCAudioEqualizer.presets.first(where: { $0.index == index }) else { return }
        player?.equalizer = VLCAudioEqualizer(preset: preset)
        selectedEqualizerPresetId = id
#endif
    }

    func setEqualizerPreamp(preampDb: Float) {
#if canImport(VLCKit)
        let equalizer = player?.equalizer ?? VLCAudioEqualizer()
        equalizer.preAmplification = min(20, max(-20, preampDb))
        player?.equalizer = equalizer
        selectedEqualizerPresetId = nil
#endif
    }

    func setEqualizerBand(index: Int32, amplificationDb: Float) {
#if canImport(VLCKit)
        let equalizer = player?.equalizer ?? VLCAudioEqualizer()
        guard index >= 0, Int(index) < equalizer.bands.count else { return }
        let band = equalizer.bands[Int(index)]
        band.amplification = min(20, max(-20, amplificationDb))
        player?.equalizer = equalizer
        selectedEqualizerPresetId = nil
#endif
    }

    func setListener(listener: VlcKitPlayerBackendListener?) {
        self.listener = listener
    }

    func dispose() {
#if canImport(VLCKit)
        player?.stop()
        player?.delegate = nil
        player?.drawable = nil
        player = nil
#endif
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        deactivateAudioSession()
        listener = nil
    }

    // MARK: - Shared renderer bridge

    func startRendererDiscovery() {
#if canImport(VLCKit)
        stopRendererDiscovery()
        guard let descriptions = VLCRendererDiscoverer.list() else { return }
        rendererDiscoverers = descriptions.compactMap { description in
            guard let discoverer = VLCRendererDiscoverer(name: description.name), discoverer.start() else {
                return nil
            }
            discoverer.delegate = self
            return discoverer
        }
#endif
    }

    func stopRendererDiscovery() {
#if canImport(VLCKit)
        rendererDiscoverers.forEach {
            $0.delegate = nil
            $0.stop()
        }
        rendererDiscoverers.removeAll()
        rendererItems.removeAll()
#endif
    }

    func renderers() -> [VlcKitRendererInfo] {
#if canImport(VLCKit)
        let items = rendererDiscoverers.flatMap(\.renderers)
        // Different libVLC discoverers can report the same physical target.
        // Keep the latest item rather than crashing on duplicate dictionary keys.
        rendererItems = [:]
        items.forEach { rendererItems[rendererId(for: $0)] = $0 }
        return items.map {
            VlcKitRendererInfo(id: rendererId(for: $0), name: $0.name, type: $0.type)
        }
#else
        []
#endif
    }

    func selectRenderer(id: String?) -> Bool {
#if canImport(VLCKit)
        let item = id.flatMap { rendererItems[$0] }
        guard id == nil || item != nil else { return false }
        // VLCKit only honours this before the first play on a player. Rebuild the
        // native player transactionally so choosing or disconnecting a renderer during
        // playback actually changes the output without losing the current item or time.
        let applied = replacePlayer(renderer: item)
        if applied { selectedRendererId = id }
        return applied
#else
        false
#endif
    }

    func currentRendererId() -> String? { selectedRendererId }

    var isVlcKitLinked: Bool {
#if canImport(VLCKit)
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

#if canImport(VLCKit)
    private func makeTracks(names: [Any], indexes: [Any], selected: Int) -> [PlaybackTrack] {
        let labels = names.compactMap { $0 as? String }
        let ids = indexes.map { String(describing: $0) }
        return zip(labels, ids).map { label, id in
            PlaybackTrack(id: id, label: label, selected: Int(id) == selected)
        }
    }

    private func ensurePlayer() -> VLCMediaPlayer {
        if let player { return player }
        let player = makePlayer()
        self.player = player
        return player
    }

    private func makePlayer() -> VLCMediaPlayer {
        let player = VLCMediaPlayer()
        player.delegate = self
        // The PiP drawable is also the normal native video host.  VLCKit creates
        // its video view inside it, which means PiP never needs a second decoder
        // or a parallel AVPlayer pipeline.
        pipDrawable.attach(backend: self)
        player.drawable = pipDrawable
        player.audio?.volume = configuredVolume
        player.rate = configuredRate
        applyVideoOutput(to: player)
        applyVideoCrop(to: player)
        return player
    }

    private var activeDrawableView: UIView? {
        externalDrawableView ?? drawableView
    }

    private func updateDrawable() {
        pipDrawable.updateHostView(activeDrawableView)
    }

    // MARK: - PiP drawable callbacks

    func pipAddVideoSubview(_ view: UIView) {
        guard let host = activeDrawableView else { return }
        view.frame = host.bounds
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        host.addSubview(view)
    }

    func pipPlay() { resume() }
    func pipPause() { pause() }

    func pipSeek(by offsetMs: Int64, completion: @escaping () -> Void) {
        guard let player, player.isSeekable else {
            completion()
            return
        }
        player.jump(withOffset: Int32(clamping: offsetMs), completion: completion)
    }

    func pipMediaLength() -> Int64 { Int64(player?.media?.length.intValue ?? 0) }
    func pipMediaTime() -> Int64 { Int64(player?.time.intValue ?? 0) }
    func pipIsSeekable() -> Bool { player?.isSeekable == true }
    func pipIsPlaying() -> Bool { player?.isPlaying == true }

    /**
     * Renderer assignment is a construction-time option in VLCKit. Create and validate
     * the replacement first, then detach the previous player so a failed selection never
     * interrupts playback. The native player owns decoder/output state; the shared queue and
     * UI state deliberately remain untouched.
     */
    private func replacePlayer(renderer: VLCRendererItem?) -> Bool {
        let replacement = makePlayer()
        guard replacement.setRendererItem(renderer) else { return false }
        guard let previous = player else {
            player = replacement
            return true
        }

        let media = previous.media
        let positionMs = max(0, Int64(previous.time.intValue))
        let wasPlaying = previous.isPlaying
        let equalizer = previous.equalizer
        let audioTrack = previous.currentAudioTrackIndex
        let subtitleTrack = previous.currentVideoSubTitleIndex
        let audioDelay = previous.currentAudioPlaybackDelay
        let subtitleDelay = previous.currentVideoSubTitleDelay
        let chapter = previous.currentChapterIndex

        replacement.equalizer = equalizer
        replacement.media = media
        if let externalSubtitleURL {
            _ = replacement.addPlaybackSlave(externalSubtitleURL, type: .subtitle, enforce: true)
        }
        replacement.time = VLCTime(number: NSNumber(value: positionMs))
        replacement.currentAudioTrackIndex = audioTrack
        replacement.currentVideoSubTitleIndex = subtitleTrack
        replacement.currentAudioPlaybackDelay = audioDelay
        replacement.currentVideoSubTitleDelay = subtitleDelay
        replacement.currentChapterIndex = chapter
        copyVideoAdjust(from: previous, to: replacement)

        // Stop only after the replacement is completely configured. Clearing the delegate
        // prevents the intentional teardown from publishing a false "stopped" event to KMP.
        previous.delegate = nil
        previous.drawable = nil
        previous.stop()
        player = replacement

        if wasPlaying {
            activateAudioSession()
            replacement.play()
        }
        publishNowPlayingInfo(isPlaying: wasPlaying)
        return true
    }

    private func applyVideoOutput(to player: VLCMediaPlayer?) {
        player?.scaleFactor = videoOutputScale
        if let videoOutputAspectRatio {
            videoOutputAspectRatio.withCString { player?.videoAspectRatio = UnsafeMutablePointer(mutating: $0) }
        } else {
            player?.videoAspectRatio = nil
        }
    }

    private func applyVideoCrop(to player: VLCMediaPlayer?) {
        if let geometry = videoCropMode.geometry {
            geometry.withCString { player?.videoCropGeometry = UnsafeMutablePointer(mutating: $0) }
        } else {
            player?.videoCropGeometry = nil
        }
    }

    private func copyVideoAdjust(from source: VLCMediaPlayer, to destination: VLCMediaPlayer) {
        let sourceFilter = source.adjustFilter
        let destinationFilter = destination.adjustFilter
        destinationFilter.brightness.value = NSNumber(value: filterFloat(sourceFilter.brightness, fallback: 1))
        destinationFilter.contrast.value = NSNumber(value: filterFloat(sourceFilter.contrast, fallback: 1))
        destinationFilter.hue.value = NSNumber(value: filterFloat(sourceFilter.hue, fallback: 0))
        destinationFilter.saturation.value = NSNumber(value: filterFloat(sourceFilter.saturation, fallback: 1))
        destinationFilter.gamma.value = NSNumber(value: filterFloat(sourceFilter.gamma, fallback: 1))
        destinationFilter.isEnabled = sourceFilter.isEnabled
    }

    private func formatFrequency(_ frequency: Float) -> String {
        frequency >= 1_000 ? String(format: "%.1f kHz", frequency / 1_000) : String(format: "%.0f Hz", frequency)
    }

#if canImport(VLCKit)
    private func rendererId(for item: VLCRendererItem) -> String {
        "\(item.type)|\(item.name)|\(item.iconURI)"
    }
#endif

#if canImport(VLCKit)
    private func filterFloat(_ parameter: VLCFilterParameterProtocol, fallback: Float) -> Float {
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
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        let current = Int64(player?.time.intValue ?? 0)
        seekTo(positionMs: max(0, current + milliseconds))
#endif
    }

    private func publishNowPlayingInfo(isPlaying: Bool? = nil) {
#if canImport(VLCKit)
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
#if canImport(VLCKit)
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
#if canImport(VLCKit)
        guard let rawReason = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: rawReason) else { return }
        // Never blast sound through the speaker after headphones / an external route disappear.
        if reason == .oldDeviceUnavailable && player?.isPlaying == true {
            player?.pause()
        }
#endif
    }
}

#if canImport(VLCKit)
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
        VlcKitPipDrawable.shared.invalidatePlaybackState()
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard let player else { return }
        let time = Int64(player.time.intValue)
        let length = Int64(player.media?.length.intValue ?? 0)
        listener?.onTimeChanged(timeMs: time, lengthMs: length)
        publishNowPlayingInfo()
        VlcKitPipDrawable.shared.invalidatePlaybackState()
    }
}

#if canImport(VLCKit)
extension VlcKitBackend: VLCRendererDiscovererDelegate {
    func rendererDiscovererItemAdded(_ rendererDiscoverer: VLCRendererDiscoverer, item: VLCRendererItem) {
        rendererItems[rendererId(for: item)] = item
    }

    func rendererDiscovererItemDeleted(_ rendererDiscoverer: VLCRendererDiscoverer, item: VLCRendererItem) {
        rendererItems.removeValue(forKey: rendererId(for: item))
        if selectedRendererId == rendererId(for: item) { selectedRendererId = nil }
    }
}
#endif
#endif
