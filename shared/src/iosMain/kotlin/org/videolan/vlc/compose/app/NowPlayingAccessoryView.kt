@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.videolan.vlc.compose.app

import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.videolan.vlc.viewmodel.PlayerUiState
import org.videolan.vlc.viewmodel.PlayerViewModel
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageSymbolWeightSemibold
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIStackView
import platform.UIKit.UIViewContentMode
import platform.UIKit.labelColor
import platform.UIKit.secondaryLabelColor

/**
 * Native content for the iOS 26+ tab bar accessory, like Music's now-playing row: artwork,
 * title and artist, play/pause and next as SF Symbols. Being UIKit, its labels and symbols adopt
 * the Liquid Glass vibrancy; a tap anywhere else opens the full player.
 */
internal class NowPlayingAccessoryView(
    private val player: PlayerViewModel,
    scope: CoroutineScope,
    onExpand: () -> Unit,
) : UIControl(frame = CGRectZero.readValue()) {
    private val artwork = UIImageView()
    private val title = UILabel()
    private val subtitle = UILabel()
    private val playPause = UIButton.buttonWithType(UIButtonTypeSystem)
    private val next = UIButton.buttonWithType(UIButtonTypeSystem)
    private val symbols = UIImageSymbolConfiguration.configurationWithPointSize(17.0, UIImageSymbolWeightSemibold)
    // Sentinel so the very first render always resolves artwork (or its music-note fallback).
    private var shownArtwork: String? = "\u0000"

    init {
        artwork.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
        artwork.clipsToBounds = true
        artwork.layer.cornerRadius = 8.0
        artwork.backgroundColor = UIColor.secondaryLabelColor.colorWithAlphaComponent(0.15)
        title.font = UIFont.systemFontOfSize(15.0, UIFontWeightSemibold)
        title.textColor = UIColor.labelColor
        subtitle.font = UIFont.systemFontOfSize(13.0)
        subtitle.textColor = UIColor.secondaryLabelColor

        val labels = UIStackView(arrangedSubviews = listOf(title, subtitle)).apply {
            axis = UILayoutConstraintAxisVertical
            spacing = 1.0
            userInteractionEnabled = false
        }
        listOf(playPause, next).forEach { it.tintColor = UIColor.labelColor }
        next.setImage(UIImage.systemImageNamed("forward.fill", withConfiguration = symbols), forState = 0u)
        playPause.addAction(UIAction.actionWithHandler { player.togglePlayPause() }, forControlEvents = UIControlEventTouchUpInside)
        next.addAction(UIAction.actionWithHandler { player.next() }, forControlEvents = UIControlEventTouchUpInside)
        addAction(UIAction.actionWithHandler { onExpand() }, forControlEvents = UIControlEventTouchUpInside)
        artwork.userInteractionEnabled = false

        listOf(artwork, labels, playPause, next).forEach {
            it.translatesAutoresizingMaskIntoConstraints = false
            addSubview(it)
        }
        NSLayoutConstraint.activateConstraints(
            listOf(
                artwork.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = 10.0),
                artwork.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
                artwork.widthAnchor.constraintEqualToConstant(32.0),
                artwork.heightAnchor.constraintEqualToConstant(32.0),
                labels.leadingAnchor.constraintEqualToAnchor(artwork.trailingAnchor, constant = 10.0),
                labels.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
                labels.trailingAnchor.constraintLessThanOrEqualToAnchor(playPause.leadingAnchor, constant = -8.0),
                playPause.trailingAnchor.constraintEqualToAnchor(next.leadingAnchor, constant = -4.0),
                playPause.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
                playPause.widthAnchor.constraintEqualToConstant(44.0),
                playPause.heightAnchor.constraintEqualToConstant(44.0),
                next.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -6.0),
                next.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
                next.widthAnchor.constraintEqualToConstant(44.0),
                next.heightAnchor.constraintEqualToConstant(44.0),
            ),
        )
        scope.launch { player.state.collect(::render) }
    }

    private fun render(state: PlayerUiState) {
        title.text = state.title
        subtitle.text = state.subtitle
        subtitle.hidden = state.subtitle.isBlank()
        playPause.setImage(
            UIImage.systemImageNamed(if (state.playing) "pause.fill" else "play.fill", withConfiguration = symbols),
            forState = 0u,
        )
        val art = state.queue.getOrNull(state.currentQueueIndex)?.artworkUri ?: state.artworkUri
        if (art != shownArtwork) {
            shownArtwork = art
            artwork.image = art?.let { NSURL.URLWithString(it)?.path }?.let { UIImage.imageWithContentsOfFile(it) }
                ?: UIImage.systemImageNamed("music.note", withConfiguration = symbols)
            artwork.tintColor = UIColor.secondaryLabelColor
        }
    }
}
