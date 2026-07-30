/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.vlc

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.VlcPlaybackState
import org.videolan.vlc.media.MediaSessionBrowser
import org.videolan.vlc.media.PlayerController

/**
 * The Media3 view of VLC's native libVLC engine.
 *
 * libVLC remains the renderer and playlist owner. This adapter is deliberately
 * thin: it maps Media3 commands to the existing service contract and exposes a
 * fresh immutable Media3 state whenever that native contract changes.
 */
@androidx.media3.common.util.UnstableApi
internal class VlcMedia3Player(
    private val service: PlaybackService,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    fun syncState() = invalidateState()

    override fun getState(): State {
        val playlist = service.media.map(::toMediaItemData)
        val currentIndex = service.currentMediaPosition.takeIf { it in playlist.indices } ?: C.INDEX_UNSET
        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                        Player.COMMAND_SET_REPEAT_MODE,
                        Player.COMMAND_SET_SHUFFLE_MODE,
                        Player.COMMAND_SET_SPEED_AND_PITCH,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_GET_MEDIA_ITEMS_METADATA,
                        Player.COMMAND_SET_MEDIA_ITEM,
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                    )
                    .build(),
            )
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(currentIndex)
            .setContentPositionMs(service.getTime())
            .setPlaybackState(
                when (PlayerController.playbackState) {
                    VlcPlaybackState.PLAYING, VlcPlaybackState.PAUSED -> Player.STATE_READY
                    VlcPlaybackState.CONNECTING -> Player.STATE_BUFFERING
                    VlcPlaybackState.STOPPED -> Player.STATE_ENDED
                    else -> Player.STATE_IDLE
                },
            )
            .setPlayWhenReady(service.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setRepeatMode(
                when (service.repeatType) {
                    VlcPlaybackState.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                    VlcPlaybackState.REPEAT_ALL, VlcPlaybackState.REPEAT_GROUP -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                },
            )
            .setShuffleModeEnabled(service.isShuffling)
            .setPlaybackParameters(PlaybackParameters(service.speed))
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) service.play() else service.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        service.stop()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, command: Int): ListenableFuture<*> {
        when {
            mediaItemIndex != C.INDEX_UNSET && mediaItemIndex != service.currentMediaPosition -> service.playIndex(mediaItemIndex)
            positionMs != C.TIME_UNSET -> service.setTime(positionMs)
            command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> service.next()
            command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> service.previous(force = false)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        service.repeatType = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> VlcPlaybackState.REPEAT_ONE
            Player.REPEAT_MODE_ALL -> VlcPlaybackState.REPEAT_ALL
            else -> VlcPlaybackState.REPEAT_NONE
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        if (service.isShuffling != shuffleModeEnabled) service.shuffle()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        service.setRate(playbackParameters.speed, save = true)
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        mediaItems.getOrNull(startIndex.takeIf { it in mediaItems.indices } ?: 0)
            ?.let(service::playMedia3Item)
        return Futures.immediateVoidFuture()
    }

    private fun toMediaItemData(media: MediaWrapper): MediaItemData {
        val metadata = MediaMetadata.Builder()
            .setTitle(media.nowPlaying ?: media.title)
            .setArtist(media.artistName)
            .setAlbumTitle(media.albumName)
            .setArtworkUri(media.artworkMrl?.let(android.net.Uri::parse))
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(MediaSessionBrowser.generateMediaId(media))
            .setUri(media.uri)
            .setMediaMetadata(metadata)
            .build()
        return MediaItemData.Builder(media.uri)
            .setMediaItem(mediaItem)
            .setMediaMetadata(metadata)
            .setIsSeekable(service.isSeekable)
            .setDurationUs(C.msToUs(media.length))
            .build()
    }
}
