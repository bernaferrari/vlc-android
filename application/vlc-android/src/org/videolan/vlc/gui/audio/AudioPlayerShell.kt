/*****************************************************************************
 * AudioPlayerShell.kt
 *
 * Copyright © 2026 VLC authors and VideoLAN
 *
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
 */

package org.videolan.vlc.gui.audio

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import org.videolan.tools.dp
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCComposeView

internal fun Context.createAudioPlayerShell(): ConstraintLayout =
        ConstraintLayout(this).apply {
            id = R.id.content_layout
            layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
            setBackgroundFromAttr(if (isLandscape()) R.attr.background_default_darker else R.attr.bottom_navigation_background)
            isClickable = true
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isKeyboardNavigationCluster = true

            if (isLandscape()) addLandscapeAudioPlayerChildren() else addPortraitAudioPlayerChildren()
        }

private fun Context.isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

private fun ConstraintLayout.addPortraitAudioPlayerChildren() {
    addSharedBackgroundChildren(includeBottomGradient = false)
    addAudioHeader(landscape = false)
    addView(composeView(R.id.resume_video_hint).apply {
        elevation = 8.dp.toFloat()
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        startToStart = parentId
        endToEnd = parentId
        topToBottom = R.id.header
    })
    addView(composeView(R.id.songs_list).apply {
        clipToPadding = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setPadding(paddingLeft, paddingTop, paddingRight, 68.dp)
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToBottom = R.id.header
        bottomToTop = R.id.songs_list_guide
        matchConstraintMaxWidth = 800.dp
    })
    addView(composeView(R.id.bottom_gradient).apply {
        id = R.id.bottom_gradient
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = R.id.guideline8
        bottomToBottom = parentId
    })
    addCommonProgressAndCoverChildren(landscape = false)
    addView(View(context).apply { id = R.id.songs_list_guide }, audioLayout(matchConstraint, 1.dp) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = R.id.timeline
        bottomToBottom = R.id.timeline
    })
    addCommonTransportChildren(landscape = false)
    addAudioOverlays(landscape = false)
}

private fun ConstraintLayout.addLandscapeAudioPlayerChildren() {
    addSharedBackgroundChildren(includeBottomGradient = true)
    addVerticalGuideline(R.id.guideline13, 0.5f)
    addAudioHeader(landscape = true)
    addVerticalGuideline(R.id.guideline14, 1f / 3f)
    addView(composeView(R.id.resume_video_hint).apply {
        elevation = 8.dp.toFloat()
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        startToStart = R.id.guideline14
        endToEnd = parentId
        topToBottom = R.id.header
    })
    addView(composeView(R.id.songs_list).apply {
        setBackgroundFromAttr(R.attr.audio_list_background)
        clipToPadding = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimensionPixelSize(R.dimen.listview_bottom_padding))
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = R.id.guideline14
        endToEnd = parentId
        topToBottom = R.id.header
        bottomToBottom = R.id.timeline
        bottomMargin = 16.dp
    })
    addLandscapeTrackInfo()
    addCommonProgressAndCoverChildren(landscape = true)
    addCommonTransportChildren(landscape = true)
    addAudioOverlays(landscape = true)
}

private fun ConstraintLayout.addSharedBackgroundChildren(includeBottomGradient: Boolean) {
    addView(composeView(R.id.backgroundView).apply {
        id = R.id.backgroundView
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = parentId
    })
    if (includeBottomGradient) {
        addView(composeView(R.id.bottom_gradient).apply {
            id = R.id.bottom_gradient
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, audioLayout(matchConstraint, matchConstraint) {
            startToStart = parentId
            endToEnd = parentId
            topToTop = R.id.guideline8
            bottomToBottom = parentId
        })
    }
    addView(composeView(R.id.top_gradient).apply {
        id = R.id.top_gradient
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = R.id.guideline9
    })
    addHorizontalGuideline(R.id.guideline8, 0.65f)
    addHorizontalGuideline(R.id.guideline9, 0.3f)
    addView(composeView(R.id.progressBar).apply {
        id = R.id.progressBar
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        setPadding(0, paddingTop, 0, paddingBottom)
    }, audioLayout(matchConstraint, 4.dp) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
    })
}

private fun ConstraintLayout.addAudioHeader(landscape: Boolean) {
    addView(audioHeader(context, landscape), audioLayout(matchConstraint, if (landscape) 68.dp else wrapContent) {
        startToStart = parentId
        endToEnd = parentId
        topToBottom = R.id.progressBar
    })
}

private fun audioHeader(context: Context, landscape: Boolean) = ConstraintLayout(context).apply {
    id = R.id.header
    setBackgroundColor(Color.TRANSPARENT)
    fitsSystemWindows = true

    addHeaderTransportButton(R.id.header_shuffle, R.id.header_previous, startToParent = true)
    addHeaderTransportButton(R.id.header_previous, R.id.header_large_play_pause, startTo = R.id.header_shuffle)
    addView(composeView(R.id.header_large_play_pause).apply {
        elevation = 4.dp.toFloat()
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        startToEnd = R.id.header_previous
        endToStart = R.id.header_next
        topToTop = parentId
        bottomToBottom = parentId
        marginStart = 4.dp
        marginEnd = 4.dp
        if (landscape) horizontalBias = 0.5f
    })
    addHeaderTransportButton(R.id.header_next, R.id.header_repeat, startTo = R.id.header_large_play_pause)
    addHeaderTransportButton(R.id.header_repeat, endToParent = true, startTo = R.id.header_next, endMargin = 16.dp)

    addView(composeView(R.id.header_background), audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = parentId
    }.apply {
        if (!landscape) minHeight = 68.dp
    })
    addView(composeView(R.id.header_divider), audioLayout(matchConstraint, 1.dp) {
        startToStart = parentId
        endToEnd = parentId
        bottomToBottom = parentId
    })
    addView(composeView(R.id.audio_media_switcher).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }, audioLayout(matchConstraint, if (landscape) matchConstraint else 68.dp) {
        startToStart = parentId
        endToStart = if (landscape) R.id.playback_chips else R.id.barrier
        topToTop = parentId
        if (landscape) bottomToBottom = parentId
    })
    addView(composeView(R.id.playback_chips).apply {
        clipToPadding = false
    }, audioLayout(wrapContent, wrapContent) {
        if (landscape) {
            endToStart = R.id.barrier
            topToTop = parentId
            bottomToBottom = parentId
            marginEnd = 8.dp
        } else {
            startToStart = parentId
            topToBottom = R.id.audio_media_switcher
            bottomToBottom = parentId
            bottomMargin = 8.dp
        }
    })
    addView(composeView(R.id.playlist_search_text).apply {
        isFocusable = true
        isFocusableInTouchMode = true
        visibility = View.GONE
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = R.id.header_play_pause
        topToTop = parentId
        bottomToBottom = if (landscape) parentId else R.id.guideline_header_bottom
        marginStart = 8.dp
        topMargin = 4.dp
        marginEnd = 8.dp
        bottomMargin = 4.dp
    })
    if (!landscape) addHorizontalGuideline(R.id.guideline_header_bottom, begin = 68.dp)
    val headerBottom = if (landscape) parentId else R.id.guideline_header_bottom
    addHeaderAction(R.id.ab_repeat_reset, R.id.ab_repeat_stop, headerBottom, marginEnd = 8.dp)
    addHeaderAction(R.id.ab_repeat_stop, R.id.playlist_search, headerBottom, marginEnd = 8.dp)
    addHeaderAction(R.id.playlist_search, R.id.playlist_switch, headerBottom)
    addHeaderAction(R.id.playlist_switch, R.id.adv_function, headerBottom)
    addView(composeView(R.id.adv_function), audioLayout(wrapContent, wrapContent) {
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = headerBottom
    })
    addView(composeView(R.id.header_time), audioLayout(wrapContent, wrapContent) {
        endToStart = R.id.header_play_pause
        topToTop = parentId
        bottomToBottom = headerBottom
        marginStart = resources.getDimensionPixelSize(R.dimen.default_margin)
        marginEnd = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(composeView(R.id.header_play_pause), audioLayout(38.dp, 38.dp) {
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = headerBottom
        marginEnd = resources.getDimensionPixelSize(R.dimen.default_margin)
    })
    addView(Barrier(context).apply {
        id = R.id.barrier
        type = Barrier.START
        setReferencedIds(intArrayOf(R.id.header_time, R.id.playlist_search, R.id.ab_repeat_reset, R.id.ab_repeat_stop, R.id.header_shuffle))
    }, audioLayout(matchConstraint, matchConstraint) {
        topToTop = parentId
        bottomToBottom = parentId
    })
}

private fun ConstraintLayout.addHeaderTransportButton(
    id: Int,
    endTo: Int = parentId,
    startTo: Int = parentId,
    startToParent: Boolean = false,
    endToParent: Boolean = false,
    endMargin: Int = 4.dp
) {
    addView(composeView(id).apply {
        elevation = 4.dp.toFloat()
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        if (startToParent) startToStart = parentId else startToEnd = startTo
        if (endToParent) endToEnd = parentId else endToStart = endTo
        topToTop = R.id.header_large_play_pause
        bottomToBottom = R.id.header_large_play_pause
        marginStart = if (startToParent) 16.dp else 4.dp
        marginEnd = endMargin
        horizontalBias = 0.5f
        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
    })
}

private fun ConstraintLayout.addHeaderAction(id: Int, endTo: Int, bottomTo: Int, marginEnd: Int = 0) {
    addView(composeView(id).apply {
        if (id == R.id.ab_repeat_reset || id == R.id.ab_repeat_stop) visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        endToStart = endTo
        topToTop = parentId
        bottomToBottom = bottomTo
        this.marginEnd = marginEnd
    })
}

private fun ConstraintLayout.addLandscapeTrackInfo() {
    addView(composeView(R.id.track_info_container).apply {
        id = R.id.track_info_container
        visibility = View.GONE
    }, audioLayout(matchConstraint, wrapContent) {
        startToStart = R.id.guideline14
        endToEnd = parentId
        topToBottom = R.id.header
        bottomToTop = R.id.time
        topMargin = 32.dp
        bottomMargin = 8.dp
    })
}

private fun ConstraintLayout.addCommonProgressAndCoverChildren(landscape: Boolean) {
    addView(composeView(R.id.audio_play_progress).apply {
        elevation = 4.dp.toFloat()
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
    }, audioLayout(wrapContent, wrapContent) {
        bottomToTop = R.id.time
        if (landscape) {
            startToStart = parentId
            endToStart = R.id.guideline14
            marginStart = 4.dp
            marginEnd = 4.dp
            constrainedWidth = true
            horizontalBias = 0.5f
        } else {
            startToStart = parentId
            endToEnd = parentId
        }
    })
    addView(composeView(R.id.cover_media_switcher).apply {
        if (!landscape) visibility = View.GONE
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        topToBottom = R.id.header
        if (landscape) {
            endToStart = R.id.guideline14
            bottomToTop = R.id.time
            topMargin = 8.dp
            bottomMargin = 8.dp
            dimensionRatio = "1"
        } else {
            endToEnd = parentId
            bottomToTop = R.id.audio_rewind_10
            topMargin = 16.dp
            verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
        }
    })
    if (!landscape) addPortraitSeekJumpControls()
}

private fun ConstraintLayout.addPortraitSeekJumpControls() {
    addPortraitSeekJumpView(R.id.audio_rewind_bookmark, startTo = parentId, endTo = R.id.audio_rewind_10)
    addPortraitSeekJumpView(R.id.audio_rewind_10, startTo = R.id.audio_rewind_bookmark, endTo = R.id.audio_forward_10)
    addView(composeView(R.id.audio_rewind_text).apply {
        visibility = View.GONE
    }, audioLayout(48.dp, 48.dp) {
        startToStart = R.id.audio_rewind_10
        endToEnd = R.id.audio_rewind_10
        topToTop = R.id.audio_rewind_10
        bottomToBottom = R.id.audio_rewind_10
    })
    addView(composeView(R.id.audio_forward_10).apply {
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        startToEnd = R.id.audio_rewind_10
        endToStart = R.id.audio_forward_bookmark
        topToTop = R.id.audio_rewind_10
        bottomToBottom = R.id.audio_rewind_10
        horizontalBias = 0.5f
    })
    addPortraitSeekJumpView(R.id.audio_forward_bookmark, startTo = R.id.audio_forward_10, endTo = parentId, alignTo = R.id.audio_rewind_10)
    addView(composeView(R.id.audio_forward_text).apply {
        visibility = View.GONE
    }, audioLayout(48.dp, 48.dp) {
        startToStart = R.id.audio_forward_10
        endToEnd = R.id.audio_forward_10
        topToTop = R.id.audio_forward_10
        bottomToBottom = R.id.audio_forward_10
    })
}

private fun ConstraintLayout.addPortraitSeekJumpView(id: Int, startTo: Int, endTo: Int, alignTo: Int = R.id.cover_media_switcher) {
    addView(composeView(id).apply {
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        if (startTo == parentId) startToStart = parentId else startToEnd = startTo
        if (endTo == parentId) endToEnd = parentId else endToStart = endTo
        if (alignTo == R.id.cover_media_switcher) {
            topToBottom = R.id.cover_media_switcher
            bottomToTop = R.id.audio_play_progress
        } else {
            topToTop = alignTo
            bottomToBottom = alignTo
        }
        bottomMargin = if (alignTo == R.id.cover_media_switcher) 24.dp else 0
        horizontalBias = 0.5f
        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
    })
}

private fun ConstraintLayout.addCommonTransportChildren(landscape: Boolean) {
    addView(composeView(R.id.time).apply {
        elevation = 4.dp.toFloat()
    }, audioLayout(wrapContent, wrapContent) {
        if (landscape) {
            leftToLeft = R.id.timeline
            bottomToTop = R.id.timeline
            marginStart = 8.dp
        } else {
            leftToLeft = parentId
            topToTop = R.id.length
            bottomToBottom = R.id.length
            marginStart = resources.getDimensionPixelSize(R.dimen.default_margin)
        }
    })
    addView(composeView(R.id.timeline).apply {
        id = R.id.timeline
        elevation = 4.dp.toFloat()
        isFocusable = true
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        setPadding(0, 0, 0, 0)
    }, audioLayout(matchConstraint, wrapContent) {
        startToStart = parentId
        endToEnd = parentId
        bottomToTop = R.id.play_pause
        if (!landscape) bottomMargin = 8.dp
    })
    addView(composeView(R.id.length).apply {
        elevation = 4.dp.toFloat()
    }, audioLayout(wrapContent, wrapContent) {
        if (landscape) {
            rightToRight = R.id.timeline
            bottomToTop = R.id.timeline
            marginEnd = 8.dp
        } else {
            rightToRight = parentId
            bottomToTop = R.id.timeline
            marginEnd = resources.getDimensionPixelSize(R.dimen.default_margin)
        }
    })
    addTransportButton(R.id.shuffle, startToParent = true, endTo = R.id.previous, landscape = landscape, invisible = true)
    addTransportButton(R.id.previous, startTo = R.id.shuffle, endTo = R.id.play_pause, landscape = landscape)
    addView(composeView(R.id.play_pause).apply {
        elevation = 4.dp.toFloat()
    }, audioLayout(wrapContent, wrapContent) {
        startToEnd = R.id.previous
        endToStart = R.id.next
        bottomToBottom = if (landscape) parentId else R.id.backgroundView
        marginStart = if (landscape) 16.dp else 4.dp
        marginEnd = if (landscape) 16.dp else 4.dp
        if (!landscape) bottomMargin = 8.dp
        horizontalBias = 0.5f
    })
    addTransportButton(R.id.next, startTo = R.id.play_pause, endTo = R.id.repeat, landscape = landscape)
    addTransportButton(R.id.repeat, startTo = R.id.next, endToParent = true, landscape = landscape)
    addVerticalGuideline(R.id.centerGuideline, 0.5f)
    addHingeButton(R.id.hinge_go_left, endToStart = R.id.centerGuideline, marginEnd = 24.dp)
    addHingeButton(R.id.hinge_go_right, startToStart = R.id.centerGuideline, marginStart = 24.dp)
}

private fun ConstraintLayout.addTransportButton(
    id: Int,
    startTo: Int = parentId,
    endTo: Int = parentId,
    startToParent: Boolean = false,
    endToParent: Boolean = false,
    landscape: Boolean,
    invisible: Boolean = false
) {
    addView(composeView(id).apply {
        elevation = 4.dp.toFloat()
        if (invisible) visibility = View.INVISIBLE
    }, audioLayout(wrapContent, wrapContent) {
        if (startToParent) leftToLeft = parentId else leftToRight = startTo
        if (endToParent) rightToRight = parentId else rightToLeft = endTo
        topToTop = R.id.play_pause
        bottomToBottom = R.id.play_pause
        marginStart = if (landscape) 16.dp else if (startToParent) 16.dp else 4.dp
        marginEnd = if (landscape) 16.dp else if (endToParent) 16.dp else 4.dp
        horizontalBias = 0.5f
        horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
    })
}

private fun ConstraintLayout.addHingeButton(
    id: Int,
    startToStart: Int = 0,
    endToStart: Int = 0,
    marginStart: Int = 0,
    marginEnd: Int = 0
) {
    addView(composeView(id).apply {
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        if (startToStart != 0) this.startToStart = startToStart
        if (endToStart != 0) this.endToStart = endToStart
        topToTop = R.id.play_pause
        bottomToBottom = R.id.play_pause
        this.marginStart = marginStart
        this.marginEnd = marginEnd
    })
}

private fun ConstraintLayout.addAudioOverlays(landscape: Boolean) {
    addView(composeView(R.id.options_background).apply {
        id = R.id.options_background
        isClickable = true
        elevation = 32.dp.toFloat()
        isFocusable = false
        visibility = View.GONE
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = parentId
        verticalBias = 1F
    })
    addView(composeView(R.id.bookmarks_background).apply {
        id = R.id.bookmarks_background
        isClickable = true
        isFocusable = false
        visibility = View.GONE
    }, audioLayout(matchConstraint, matchConstraint) {
        startToStart = parentId
        endToEnd = parentId
        topToTop = parentId
        bottomToBottom = parentId
    })
    addView(composeView(R.id.bookmark_marker_container).apply {
        id = R.id.bookmark_marker_container
    }, audioLayout(matchConstraint, wrapContent) {
        startToStart = R.id.timeline
        endToEnd = R.id.timeline
        bottomToBottom = R.id.timeline
        if (!landscape) topToTop = R.id.timeline
        bottomMargin = if (landscape) 14.dp else 18.dp
    })
    addView(composeView(R.id.ab_repeat_marker_guideline_container).apply {
        id = R.id.ab_repeat_marker_guideline_container
        clipToPadding = false
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        visibility = View.GONE
        if (landscape) setPadding(8.dp, paddingTop, 8.dp, paddingBottom)
        else elevation = 4.dp.toFloat()
    }, audioLayout(matchConstraint, wrapContent) {
        startToStart = R.id.timeline
        endToEnd = R.id.timeline
        if (landscape) {
            bottomToTop = R.id.timeline
        } else {
            topToTop = R.id.timeline
            bottomToBottom = R.id.timeline
            bottomMargin = 32.dp
        }
    })
    addView(composeView(R.id.ab_repeat_container).apply {
        id = R.id.ab_repeat_container
        visibility = View.GONE
    }, audioLayout(wrapContent, wrapContent) {
        bottomToTop = R.id.time
        bottomMargin = 8.dp
        if (landscape) {
            startToStart = R.id.cover_media_switcher
            endToEnd = R.id.cover_media_switcher
        } else {
            startToStart = parentId
            endToEnd = parentId
        }
    })
}

private fun ConstraintLayout.addHorizontalGuideline(id: Int, percent: Float? = null, begin: Int? = null) {
    addView(Guideline(context).apply { this.id = id }, audioLayout(wrapContent, wrapContent) {
        orientation = ConstraintLayout.LayoutParams.HORIZONTAL
        percent?.let { guidePercent = it }
        begin?.let { guideBegin = it }
    })
}

private fun ConstraintLayout.addVerticalGuideline(id: Int, percent: Float) {
    addView(Guideline(context).apply { this.id = id }, audioLayout(wrapContent, wrapContent) {
        orientation = ConstraintLayout.LayoutParams.VERTICAL
        guidePercent = percent
    })
}

private fun View.composeView(id: Int) = VLCComposeView(context).apply { this.id = id }

private fun audioLayout(width: Int, height: Int, block: ConstraintLayout.LayoutParams.() -> Unit = {}) =
        ConstraintLayout.LayoutParams(width, height).apply(block)

private fun View.setBackgroundFromAttr(@AttrRes attr: Int) {
    val typedValue = TypedValue()
    if (!context.theme.resolveAttribute(attr, typedValue, true)) return
    if (typedValue.resourceId != 0) setBackgroundResource(typedValue.resourceId)
    else setBackgroundColor(typedValue.data)
}

private const val parentId = ConstraintLayout.LayoutParams.PARENT_ID
private const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
private const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT
private const val matchConstraint = 0
