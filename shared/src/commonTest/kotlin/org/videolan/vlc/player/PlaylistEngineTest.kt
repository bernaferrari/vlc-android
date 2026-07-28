package org.videolan.vlc.player

import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistEngineTest {

    private fun item(id: Long, title: String = "t$id") =
        MediaItem(id, title, "file:///$id.mp3", MediaType.AUDIO, duration = 60_000)

    @Test
    fun loadAndNext() {
        val engine = PlaylistEngine()
        val items = listOf(item(1), item(2), item(3))
        engine.playFromIndex(items, 0)
        assertEquals(0, engine.snapshot().currentIndex)
        engine.next()
        assertEquals(1, engine.snapshot().currentIndex)
        engine.next()
        assertEquals(2, engine.snapshot().currentIndex)
    }

    @Test
    fun appendAndInsertNext() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1)), 0)
        engine.append(listOf(item(2), item(3)))
        assertEquals(3, engine.snapshot().size)
        engine.insertNext(listOf(item(9)))
        assertEquals(item(9).uri, engine.snapshot().items[1].uri)
    }

    @Test
    fun removeAdjustsIndex() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1), item(2), item(3)), 1)
        engine.removeAt(0)
        assertEquals(0, engine.snapshot().currentIndex)
        assertEquals(2, engine.snapshot().size)
    }

    @Test
    fun shuffleAndRepeatFlags() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1), item(2)), 0)
        engine.setShuffle(true)
        engine.setRepeatMode(RepeatMode.ALL)
        assertTrue(engine.snapshot().shuffle)
        assertEquals(RepeatMode.ALL, engine.snapshot().repeatMode)
    }

    @Test
    fun indexHelperMove() {
        assertEquals(2, PlaylistIndexHelper.adjustCurrentOnMove(1, 1, 3))
        assertEquals(1, PlaylistIndexHelper.adjustCurrentOnAdd(0, 0, false))
    }

    @Test
    fun backendAttachedAfterSettingsRestorationReceivesCurrentAudioConfiguration() {
        val engine = PlaylistEngine()
        engine.setVolume(156)
        engine.setRate(1.25f)
        val backend = RecordingBackend()

        engine.setBackend(backend)

        assertEquals(156, backend.reportedVolume)
        assertEquals(1.25f, backend.reportedRate)
    }

    @Test
    fun playbackRateIsFiniteAndWithinDecoderRange() {
        val engine = PlaylistEngine()

        engine.setRate(Float.NaN)
        assertEquals(1f, engine.getRate())
        engine.setRate(20f)
        assertEquals(8f, engine.getRate())
        engine.setRate(8f)
        assertEquals(8f, engine.getRate())
        engine.setRate(0.01f)
        assertEquals(0.25f, engine.getRate())
    }

    @Test
    fun abRepeatPublishesObservableMarkerState() {
        val engine = PlaylistEngine()

        engine.toggleABRepeat()
        engine.setABRepeatValue(1_000)
        engine.setABRepeatValue(3_000)

        assertTrue(engine.abRepeatEnabled.value)
        assertEquals(1_000, engine.abRepeat.value.start)
        assertEquals(3_000, engine.abRepeat.value.stop)
        assertTrue(engine.abRepeat.value.isActive)

        engine.clearABRepeat()
        assertFalse(engine.abRepeatEnabled.value)
        assertFalse(engine.abRepeat.value.isActive)
    }

    @Test
    fun stopAfterCurrentSurvivesReorderAndClearsWhenTargetIsRemoved() {
        val engine = PlaylistEngine()
        engine.playFromIndex(listOf(item(1), item(2), item(3)), 1)

        engine.setStopAfterThis()
        assertTrue(engine.stopAfterCurrent.value)

        engine.moveItem(1, 0)
        assertEquals(0, engine.snapshot().currentIndex)
        assertTrue(engine.stopAfterCurrent.value)

        engine.removeAt(0)
        assertFalse(engine.stopAfterCurrent.value)
    }

    @Test
    fun videoScaleModeIsSharedWithAnAttachedBackend() {
        val backend = RecordingBackend()
        val engine = PlaylistEngine(backend)

        engine.setVideoScaleMode(VideoScaleMode.RATIO_235_1)

        assertEquals("235:100", backend.reportedAspectRatio)
        assertEquals(0f, backend.reportedScale)
        assertEquals(VideoScaleMode.RATIO_235_1, engine.videoScaleMode.value)
    }

    @Test
    fun videoCropDelegatesOnlyToCapableNativeBackends() {
        val backend = RecordingBackend().apply {
            availableVideoCrop = PlaybackVideoCrop(supported = true)
        }
        val engine = PlaylistEngine(backend)

        engine.setVideoCrop(VideoCropMode.RATIO_16_9)

        assertEquals(VideoCropMode.RATIO_16_9, backend.reportedVideoCrop)
        assertTrue(engine.videoCrop.value.supported)
        assertEquals(VideoCropMode.RATIO_16_9, engine.videoCrop.value.mode)
    }

    @Test
    fun videoCropNeverInvokesAnUnavailableBackend() {
        val backend = RecordingBackend()
        val engine = PlaylistEngine(backend)

        engine.setVideoCrop(VideoCropMode.RATIO_16_9)

        assertEquals(null, backend.reportedVideoCrop)
        assertFalse(engine.videoCrop.value.supported)
    }

    @Test
    fun videoAdjustIsSharedOnlyWithACapableDecoder() {
        val backend = RecordingBackend().apply {
            availableVideoAdjust = PlaybackVideoAdjust(supported = true)
        }
        val engine = PlaylistEngine(backend)

        engine.setVideoAdjustEnabled(true)
        engine.setVideoAdjust(VideoAdjustParameter.HUE, 999f)
        engine.resetVideoAdjust()

        assertTrue(backend.videoAdjustWasEnabled)
        assertEquals(180f, backend.reportedVideoAdjust[VideoAdjustParameter.HUE])
        assertTrue(backend.videoAdjustWasReset)
        assertTrue(engine.videoAdjust.value.supported)
    }

    @Test
    fun trackSelectionIsSharedAndRefreshesTheNativeSnapshot() {
        val backend = RecordingBackend().apply {
            availableTracks = PlaybackTracks(
                audio = listOf(PlaybackTrack("1", "English", true), PlaybackTrack("2", "Spanish", false)),
                subtitles = listOf(PlaybackTrack("-1", "Disabled", true), PlaybackTrack("5", "Portuguese", false)),
            )
        }
        val engine = PlaylistEngine(backend)

        engine.selectAudioTrack("2")
        engine.selectSubtitleTrack("5")

        assertEquals("2", backend.selectedAudioTrack)
        assertEquals("5", backend.selectedSubtitleTrack)
        assertEquals("English", engine.tracks.value.audio.first().label)
    }

    @Test
    fun decoderDelaysAreSharedInMicroseconds() {
        val backend = RecordingBackend().apply {
            availableDelays = PlaybackDelays(audioUs = 250_000L, subtitleUs = -500_000L, supported = true)
        }
        val engine = PlaylistEngine(backend)

        engine.setAudioDelay(500_000L)
        engine.setSubtitleDelay(-250_000L)

        assertEquals(500_000L, backend.reportedAudioDelay)
        assertEquals(-250_000L, backend.reportedSubtitleDelay)
        assertTrue(engine.delays.value.supported)
    }

    @Test
    fun externalSubtitleLoadingDelegatesToTheAttachedDecoder() {
        val backend = RecordingBackend().apply { subtitleLoadResult = true }
        val engine = PlaylistEngine(backend)

        assertTrue(engine.loadExternalSubtitle("file:///movie/subtitles.vtt"))
        assertEquals("file:///movie/subtitles.vtt", backend.loadedSubtitleUri)
    }

    @Test
    fun equalizerStateAndDecoderMutationsRemainShared() {
        val backend = RecordingBackend().apply {
            availableEqualizer = PlaybackEqualizer(
                supported = true,
                presets = listOf(PlaybackEqualizerPreset("1", "Rock")),
                bands = listOf(PlaybackEqualizerBand(0, "60 Hz", 0f)),
            )
        }
        val engine = PlaylistEngine(backend)

        engine.setEqualizerEnabled(true)
        engine.selectEqualizerPreset("1")
        engine.setEqualizerPreamp(42f)
        engine.setEqualizerBand(0, -42f)

        assertTrue(backend.equalizerWasEnabled)
        assertEquals("1", backend.selectedEqualizerPreset)
        assertEquals(20f, backend.reportedEqualizerPreamp)
        assertEquals(-20f, backend.reportedEqualizerBands[0])
        assertTrue(engine.equalizer.value.supported)
    }

    @Test
    fun restorePausedPreparesTheCurrentItemWithoutAutoPlay() {
        val backend = RecordingBackend()
        val engine = PlaylistEngine(backend)
        val playlist = org.videolan.vlc.model.Playlist(
            id = 0,
            name = "Current",
            items = listOf(item(1), item(2)),
            currentIndex = 1,
        )

        assertTrue(engine.restorePaused(playlist, positionMs = 12_345L))
        assertEquals(item(2).uri, backend.preparedUri)
        assertEquals(12_345L, backend.preparedPosition)
        assertEquals(1, engine.snapshot().currentIndex)
        assertEquals(12_345L, engine.progress.value.time)
        assertTrue(engine.state.value is PlaybackState.Paused)
    }

    private class RecordingBackend : PlayerBackend {
        var reportedVolume = -1
        var reportedRate = -1f
        var preparedUri: String? = null
        var preparedPosition = -1L
        var reportedAspectRatio: String? = null
        var reportedScale = -1f
        var availableTracks = PlaybackTracks()
        var selectedAudioTrack: String? = null
        var selectedSubtitleTrack: String? = null
        var availableDelays = PlaybackDelays()
        var reportedAudioDelay = 0L
        var reportedSubtitleDelay = 0L
        var subtitleLoadResult = false
        var loadedSubtitleUri: String? = null
        var availableEqualizer = PlaybackEqualizer()
        var availableVideoCrop = PlaybackVideoCrop()
        var availableVideoAdjust = PlaybackVideoAdjust()
        var equalizerWasEnabled = false
        var selectedEqualizerPreset: String? = null
        var reportedEqualizerPreamp = 0f
        val reportedEqualizerBands = mutableMapOf<Int, Float>()
        var reportedVideoCrop: VideoCropMode? = null
        var videoAdjustWasEnabled = false
        var videoAdjustWasReset = false
        val reportedVideoAdjust = mutableMapOf<VideoAdjustParameter, Float>()

        override fun playUri(uri: String, title: String?) = Unit
        override fun preparePaused(uri: String, title: String?, positionMs: Long): Boolean {
            preparedUri = uri
            preparedPosition = positionMs
            return true
        }
        override fun pause() = Unit
        override fun resume() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(volume: Int) { reportedVolume = volume }
        override fun getVolume(): Int = reportedVolume
        override fun setRate(rate: Float) { reportedRate = rate }
        override fun getRate(): Float = reportedRate
        override fun setVideoOutput(aspectRatio: String?, scale: Float) {
            reportedAspectRatio = aspectRatio
            reportedScale = scale
        }
        override fun tracks(): PlaybackTracks = availableTracks
        override fun selectAudioTrack(id: String) { selectedAudioTrack = id }
        override fun selectSubtitleTrack(id: String) { selectedSubtitleTrack = id }
        override fun delays(): PlaybackDelays = availableDelays
        override fun setAudioDelay(delayUs: Long) { reportedAudioDelay = delayUs }
        override fun setSubtitleDelay(delayUs: Long) { reportedSubtitleDelay = delayUs }
        override fun loadExternalSubtitle(uri: String): Boolean {
            loadedSubtitleUri = uri
            return subtitleLoadResult
        }
        override fun equalizer(): PlaybackEqualizer = availableEqualizer
        override fun setEqualizerEnabled(enabled: Boolean) { equalizerWasEnabled = enabled }
        override fun selectEqualizerPreset(id: String) { selectedEqualizerPreset = id }
        override fun setEqualizerPreamp(preampDb: Float) { reportedEqualizerPreamp = preampDb }
        override fun setEqualizerBand(index: Int, amplificationDb: Float) {
            reportedEqualizerBands[index] = amplificationDb
        }
        override fun videoCrop(): PlaybackVideoCrop = availableVideoCrop.copy(mode = reportedVideoCrop ?: availableVideoCrop.mode)
        override fun setVideoCrop(mode: VideoCropMode) { reportedVideoCrop = mode }
        override fun videoAdjust(): PlaybackVideoAdjust = availableVideoAdjust
        override fun setVideoAdjustEnabled(enabled: Boolean) { videoAdjustWasEnabled = enabled }
        override fun setVideoAdjust(parameter: VideoAdjustParameter, value: Float) {
            reportedVideoAdjust[parameter] = value
        }
        override fun resetVideoAdjust() { videoAdjustWasReset = true }
        override fun setListener(listener: PlayerBackend.Listener?) = Unit
        override fun release() = Unit
    }
}
