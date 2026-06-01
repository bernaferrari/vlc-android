/*
 * ************************************************************************
 *  VideoStatsDelegate.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.video

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import org.videolan.libvlc.Media
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.tools.readableSize
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.getAllTracks
import org.videolan.vlc.gui.view.VideoStatEntry
import org.videolan.vlc.gui.view.VideoStatTrackInfo
import org.videolan.vlc.gui.view.VideoStatsOverlayView
import org.videolan.vlc.util.LocaleUtil

class VideoStatsDelegate(private val player: VideoPlayerActivity, val scrolling: () -> Unit, val idle: () -> Unit) {
    lateinit var container: VideoStatsOverlayView
    private var lastMediaUri: Uri? = null
    private var started = false
    private val plotHandler: Handler = Handler(Looper.getMainLooper())
    private val firstTimecode = System.currentTimeMillis()
    lateinit var binding: VideoHudOverlayViews

    fun stop() {
        started = false
        plotHandler.removeCallbacks(runnable)
        if (::container.isInitialized) {
            container.visibility = View.GONE
            container.clearPlot()
        }
    }

    fun start() {
        started = true
        plotHandler.postDelayed(runnable, 300)
        if (::container.isInitialized) container.visibility = View.VISIBLE
    }

    fun initStatsView(binding: VideoHudOverlayViews) {
        this.binding = binding
        container = binding.statsContainer
        binding.statsContainer.setScrollCallbacks(scrolling = { scrolling() }, idle = { idle() })
        binding.statsContainer.addLine(StatIndex.DEMUX_BITRATE.ordinal, player.getString(R.string.demux_bitrate), ContextCompat.getColor(player, R.color.material_blue))
        binding.statsContainer.addLine(StatIndex.INPUT_BITRATE.ordinal, player.getString(R.string.input_bitrate), ContextCompat.getColor(player, R.color.material_pink))
    }

    private val runnable = Runnable {
        val media = player.service?.mediaplayer?.media as? Media ?: return@Runnable

        val stats = media.stats
        if (BuildConfig.DEBUG) Log.i(this::class.java.simpleName, "Stats: demuxBitrate: ${stats?.demuxBitrate} demuxCorrupted: ${stats?.demuxCorrupted} demuxDiscontinuity: ${stats?.demuxDiscontinuity} demuxReadBytes: ${stats?.demuxReadBytes}")
        val now = System.currentTimeMillis() - firstTimecode
        stats?.demuxBitrate?.let {
            binding.statsContainer.addData(StatIndex.DEMUX_BITRATE.ordinal, Pair(now, it * 8 * 1024))
        }
        stats?.inputBitrate?.let {
            binding.statsContainer.addData(StatIndex.INPUT_BITRATE.ordinal, Pair(now, it * 8 * 1024))
        }

        if (lastMediaUri != media.uri) {
            lastMediaUri = media.uri
            binding.statsContainer.setTrackInfo(media.buildTrackInfo())
        }

        if (started) {
            start()
        }
    }

    private fun Media.buildTrackInfo(): List<VideoStatTrackInfo> {
        return getAllTracks().mapNotNull { track ->
            val entries = mutableListOf<VideoStatEntry>()
            if (track.bitrate > 0) entries += VideoStatEntry(player.getString(R.string.bitrate), player.getString(R.string.bitrate_value, track.bitrate.toLong().readableSize()))
            entries += VideoStatEntry(player.getString(R.string.codec), track.codec.orEmpty())
            if (track.language != null && !track.language.equals("und", ignoreCase = true)) {
                entries += VideoStatEntry(player.getString(R.string.language), LocaleUtil.getLocaleName(track.language))
            }

            when (track.type) {
                IMedia.Track.Type.Audio -> (track as? IMedia.AudioTrack)?.let {
                    entries += VideoStatEntry(player.getString(R.string.channels), it.channels.toString())
                    entries += VideoStatEntry(player.getString(R.string.track_samplerate), player.getString(R.string.track_samplerate_value, it.rate))
                }
                IMedia.Track.Type.Video -> (track as? IMedia.VideoTrack)?.let {
                    val frameRate = if (it.frameRateDen == 0) Double.NaN else it.frameRateNum / it.frameRateDen.toDouble()
                    if (it.width != 0 && it.height != 0) {
                        entries += VideoStatEntry(player.getString(R.string.resolution), player.getString(R.string.resolution_value, it.width, it.height))
                    }
                    if (!frameRate.isNaN() && !frameRate.isInfinite()) {
                        entries += VideoStatEntry(player.getString(R.string.framerate), player.getString(R.string.framerate_value, frameRate))
                    }
                }
            }

            if (entries.isEmpty()) {
                null
            } else {
                VideoStatTrackInfo(
                    title = when (track.type) {
                        IMedia.Track.Type.Video -> player.getString(R.string.video)
                        IMedia.Track.Type.Audio -> player.getString(R.string.audio)
                        IMedia.Track.Type.Text -> player.getString(R.string.text)
                        else -> player.getString(R.string.unknown)
                    },
                    entries = entries
                )
            }
        }
    }

    fun onConfigurationChanged() = Unit
}

enum class StatIndex {
    INPUT_BITRATE, DEMUX_BITRATE
}
