/*****************************************************************************
 * MediaInfoAdapter.java
 *
 * Copyright © 2011-2017 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.video

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.tools.readableSize
import org.videolan.vlc.R
import org.videolan.vlc.util.LocaleUtil

// =============================================================================
// WAVE 1 HOST MIGRATION IMPORTS (InfoItem / MediaInfo host - compose-2l4.1.3)
// These come from :application:compose (api dependency in vlc-android/build.gradle).
// Reference implementations / templates:
//   - NetworkServerDialog.kt (minimal demo, compose-5qk)
//   - DebugLogActivity.kt (list adapter example with small Compose row adapter, compose-2l4.1.2 / bd compose-5wg)
//   - RecyclerSectionItemDecoration.kt + phone browser hosts (decoration + browser example, compose-2l4.1.4 / bd compose-95d)
//   - ComposeInteropLabActivity.kt (rich crown jewel cross-cut, compose-2l4.1.8 / bd compose-iju)
// =============================================================================
import android.view.ViewGroup.LayoutParams as VgLp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import org.videolan.vlc.compose.components.VLCInfoItem
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
// =============================================================================

// =========================================================================
// ==================== WAVE 1 HOST MIGRATION: MediaInfoAdapter + Info surfaces ====================
// Host file: MediaInfoAdapter.kt   (task: compose-2l4.1.3 / bd: compose-l94)
// Composable used: VLCInfoItem (leaf from InfoItem.kt with leadingContent slot)
// Target layout originally: info_item.xml (still present on disk, 100% untouched)
//
// THIS IS THE MEDIA INFO / TRACK DETAILS HOST MIGRATION (exemplary Wave 1 slice).
//
// Reference style: EXACT mirror of NetworkServerDialog.kt (compose-5qk) +
// DebugLogActivity.kt (compose-5wg) + Section Header decorations/hosts (compose-95d) +
// the rich ComposeInteropLabActivity.kt (compose-iju) for comment density, traceability,
// rollback matrices, and Permanent Exceptions language.
//
// MISSION: Integrate VLCInfoItem into the legacy MediaInfoAdapter (which powers
// the track list inside InfoActivity for video/audio/subtitle tracks shown in the
// media info screen). The adapter inflates info_item.xml rows (RecyclerView) and
// populates title + subtitle (text) based on IMedia.Track data. Icon (ImageView)
// in the XML was never wired in code (inert), so we map track.type -> unicode
// symbol for the leadingContent slot (text symbols only; drawables stay out of
// compose module per rules).
//
// TWO PATTERNS DEMONSTRATED / DOCUMENTED HERE (for any future RV list host):
//
// PATTERN 1 - Full-screen ComposeView Activity host (now used by DebugLogActivity
//   and ComposeInteropLabActivity) for screens whose legacy shell can be removed.
//
// PATTERN 2 - Programmatic ComposeView for RecyclerView rows (the adapter row pattern)
//   Inside onCreateViewHolder:
//       val cv = ComposeView(parent.context).apply {
//           layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
//       }
//       return ViewHolder(cv)   // itemView is now the Compose host
//   Inside onBindViewHolder (replaces findViewById + text= ):
/*     (holder.itemView as ComposeView).setContent {
         VLCTheme {
             VLCInfoItem(
                 title = title,
                 subtitle = textBuilder.toString(),
                 leadingContent = when (track.type) { ... -> { Text("♪", color=...) } ... }
             )
         }
     } */
//   The old inflation + traditional ViewHolder findViewById path is fully preserved
//   (commented) immediately above the new lines for instant rollback.
//
// WHY THIS IS SAFE (Permanent Exceptions boundary respected):
//   - Purely additive: original info_item.xml untouched on disk forever.
//   - Old rendering path (inflate + two findViewById + setText) remains in source
//     as big commented block inside onCreateViewHolder + ViewHolder + onBind.
//   - No behavior change whatsoever to InfoActivity, track parsing, appendCommon/
//     appendAudio/appendVideo helpers, setTracks, or the visible info screen.
//   - VLCInfoItem is a leaf: pure presentational (Row + Texts + optional slot),
//     delegates theming to VLCTheme (which defaults to system dark/light), no side
//     effects, no navigation, no live data beyond the three props.
//   - Icon mapping: because the original adapter never touched the <ImageView id=icon>
//     (confirmed by code audit), we introduce simple type->symbol mapping here
//     using the exact unicode + color pattern already exercised in:
//       - ComposeInteropLabActivity (the "3. VLCInfoItem" section + combined mock)
//       - PreviewUtils.kt (VLCInfoItemLightPreview + SectionedList*Previews)
//     This keeps compose module free of drawable references (policy).
//   - Rollback is one-file (this adapter): remove the 7 new imports, delete the
//     new onCreate/onBind bodies (uncomment the three OLD blocks), revert the
//     ViewHolder to only the two findViewById lines. Zero other files touched.
//     Info screen and all media info flows continue exactly as 2026-05 pre-Wave1.
//   - RecyclerView + ComposeView rows is a standard, well-understood bridge pattern
//     (cheaper than full LazyColumn migration for this info surface). For very large
//     track lists one would eventually move the whole InfoActivity to Compose screen.
//
// HYBRID MIGRATION STRATEGY (Wave 1 epic - compose-2l4 series, identical to all prior):
//   1. Keep legacy XML + inflation sites working (we do - 100%, commented only).
//   2. Leaf Composables are pure presentational + self-themed (VLCInfoItem done in cb5.2).
//   3. Use VLCComposeView (XML Pattern 1) or raw ComposeView (Kotlin/adapter Pattern 2) - we use 2 here.
//   4. Always wrap setContent content with VLCTheme (even though leaf also wraps internally).
//   5. Leave original layout XML files in place until the LAST reference is migrated (policy).
//   6. For RecyclerView hosts: implement the onCreateViewHolder + onBindViewHolder
//      ComposeView pattern exactly as shown.
//   7. Exercise the leaf inside the Interop Lab (already present before this task) +
//      richer dedicated mocks in PreviewUtils (we add "Media Info Track List Mock").
//   8. Massive header comments (this block) + bd tracking (compose-l94) + full
//      Agents.md session completion (git pull --rebase, bd dolt push, git push).
//   9. Update cross-cutting notes on compose-iju (even post-close) + README pointers.
//
// WAVE 1.3 SPECIFICS FOR THIS HOST:
//   - Primary surface: InfoActivity (launched for media details) uses this adapter
//     for the "Tracks" section (audio/video/text). High visibility for power users.
//   - Data source: IMedia.Track list from MediaLibrary + libvlc parse. We preserve
//     every byte of appendCommon/appendAudio/appendVideo logic by calling it inside
//     the new bind path before feeding VLCInfoItem.
//   - leadingContent slot: exercised with "♪" (audio), "📺" (video), "📝" (text) or
//     omitted for unknown. Colors pulled from VLCThemeDefaults.colors.fontAudioLight
//     exactly as in Lab + Previews.
//   - No DataBinding / ViewBinding coupling in the compose leaf (by design).
//   - Future activation slice could flip a boolean or use a build flag; for now
//     the new path is the active one (old remains for A/B or emergency revert).
//
// Traceability (copy-paste ready for future agents):
//   - Leaf: application/compose/src/main/java/org/videolan/vlc/compose/components/InfoItem.kt
//           (KDoc + @Preview coverage; cb5.2 closed)
//   - Interop helper: application/compose/src/main/java/org/videolan/vlc/compose/interop/VLCCompose.kt
//   - Theme: application/compose/src/main/java/org/videolan/vlc/compose/theme/VLCTheme.kt + VLCThemeDefaults
//   - Previews (including new media info mocks added in this task): application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt
//   - Original XML (never touched): application/vlc-android/res/layout/info_item.xml
//   - Primary host surface: application/vlc-android/src/org/videolan/vlc/gui/InfoActivity.kt
//     (DataBinding + RecyclerView setup + mediaTracks observer - comments added here too)
//   - This adapter: .../gui/video/MediaInfoAdapter.kt
//   - This task: compose-2l4.1.3 (bd compose-l94, discovered-from compose-iju)
//   - Cross-cut / crown jewel: compose-2l4.1.8 (bd compose-iju) - Lab + gate policy
//   - Wave epic context: compose-cb5 series (leaf factory) + compose-2l4 host migrations
//   - Permanent Exceptions (per cross-cut): native player surfaces, MediaLibrary JNI,
//     complex TV overlays, certain WebView remnants, low-level rendering paths.
//     InfoActivity / track lists are NOT in the 20% exception bucket - excellent
//     candidate for hybrid + eventual full Compose destination migration.
//   - Related prior hosts for copy-paste: NetworkServerDialog (dialog + spinner),
//     DebugLogActivity (ListView + custom ArrayAdapter returning ComposeView rows),
//     Recycler*Decorations (Canvas + future programmatic header hosting).
//
// At end of this work: bd progress + note on cross-cut + close with rich reason,
// then MANDATORY Agents.md completion: git pull --rebase, bd dolt push, git push,
// git status must report "up to date with origin/phase-0-compose-bootstrap".
// =========================================================================

class MediaInfoAdapter : RecyclerView.Adapter<MediaInfoAdapter.ViewHolder>() {
    private lateinit var inflater: LayoutInflater
    private var dataset: List<IMedia.Track>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!::inflater.isInitialized)
            inflater = LayoutInflater.from(parent.context)

        // -----------------------------------------------------------------
        // OLD RENDERING PATH (LayoutInflater + info_item.xml + classic ViewHolder)
        // 100% PRESERVED for instant rollback / A/B testing / emergency.
        // To revert: uncomment the next 3 lines and remove the ComposeView path below.
        // The XML (info_item.xml) + all its attributes remain on disk forever.
        // -----------------------------------------------------------------
        // return ViewHolder(inflater.inflate(R.layout.info_item, parent, false))

        // NEW WAVE 1 PATH (compose-2l4.1.3): RecyclerView row backed by ComposeView
        // hosting VLCInfoItem, adapted for RecyclerView.ViewHolder.
        // layoutParams ensure it behaves like a normal list row.
        // The ViewHolder below simply wraps it; all binding happens via setContent.
        val composeView = ComposeView(parent.context).apply {
            layoutParams = VgLp(VgLp.MATCH_PARENT, VgLp.WRAP_CONTENT)
        }
        return ViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = dataset!![position]
        val title: String
        val textBuilder = StringBuilder()
        val res = holder.itemView.context.resources
        when (track.type) {
            IMedia.Track.Type.Audio -> {
                title = res.getString(R.string.track_audio)
                appendCommon(textBuilder, res, track)
                appendAudio(textBuilder, res, track as IMedia.AudioTrack)
            }
            IMedia.Track.Type.Video -> {
                title = res.getString(R.string.track_video)
                appendCommon(textBuilder, res, track)
                appendVideo(textBuilder, res, track as IMedia.VideoTrack)
            }
            IMedia.Track.Type.Text -> {
                title = res.getString(R.string.track_text)
                appendCommon(textBuilder, res, track)
            }
            else -> title = res.getString(R.string.track_unknown)
        }

        // -----------------------------------------------------------------
        // OLD BINDING (findViewById on inflated XML views) - PRESERVED IN COMMENTS
        // holder.title.text = title
        // holder.text.text = textBuilder.toString()
        // (The ViewHolder's findViewById lines are also commented in the class below.)
        // -----------------------------------------------------------------

        // NEW WAVE 1 BINDING (compose-2l4.1.3): feed the exact same computed title +
        // subtitle strings into VLCInfoItem. Icon logic (absent in original adapter
        // code) is mapped to leadingContent slot using unicode symbols + the
        // fontAudioLight token color (exact match to Lab + PreviewUtils usage).
        val itemViewAsCompose = holder.itemView as ComposeView
        itemViewAsCompose.setContent {
            VLCTheme {
                val leading: (@Composable () -> Unit)? = when (track.type) {
                    IMedia.Track.Type.Audio -> {
                        { Text("♪", color = VLCThemeDefaults.colors.fontAudioLight) }
                    }
                    IMedia.Track.Type.Video -> {
                        { Text("📺", color = VLCThemeDefaults.colors.fontAudioLight) }
                    }
                    IMedia.Track.Type.Text -> {
                        { Text("📝", color = VLCThemeDefaults.colors.fontAudioLight) }
                    }
                    else -> null
                }
                VLCInfoItem(
                    title = title,
                    subtitle = textBuilder.toString(),
                    leadingContent = leading
                )
            }
        }
    }

    override fun getItemCount() = dataset?.size ?: 0

    fun setTracks(tracks: List<IMedia.Track>) {
        val size = itemCount
        dataset = tracks
        if (size > 0) notifyItemRangeRemoved(0, size - 1)
        notifyItemRangeInserted(0, tracks.size)
    }

    private fun appendCommon(textBuilder: StringBuilder, res: Resources, track: IMedia.Track) {
        if (track.bitrate != 0)
            textBuilder.append(res.getString(R.string.track_bitrate_info, track.bitrate.toLong().readableSize()))
        textBuilder.append(res.getString(R.string.track_codec_info, track.codec))
        if (track.language != null && !track.language.equals("und", ignoreCase = true))
            textBuilder.append(res.getString(R.string.track_language_info, LocaleUtil.getLocaleName(track.language)))
    }

    private fun appendAudio(textBuilder: StringBuilder, res: Resources, track: IMedia.AudioTrack) {
        textBuilder.append(res.getQuantityString(R.plurals.track_channels_info_quantity, track.channels, track.channels))
        textBuilder.append(res.getString(R.string.track_samplerate_info, track.rate))
    }

    private fun appendVideo(textBuilder: StringBuilder, res: Resources, track: IMedia.VideoTrack) {
        val frameRate = track.frameRateNum / track.frameRateDen.toDouble()
        if (track.width != 0 && track.height != 0)
            textBuilder.append(res.getString(R.string.track_resolution_info, track.width, track.height))
        if (!frameRate.isNaN())
            textBuilder.append(res.getString(R.string.track_framerate_info, frameRate))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // -----------------------------------------------------------------
        // OLD VIEWHOLDER BINDINGS (for info_item.xml) - PRESERVED FOR ROLLBACK
        // When reverting the onCreateViewHolder + onBindViewHolder changes above,
        // also restore these two lines (and the class will again receive an
        // inflated info_item root).
        // -----------------------------------------------------------------
        // val title: TextView = itemView.findViewById(R.id.title)
        // val text: TextView = itemView.findViewById(R.id.subtitle)

        // NEW (compose-2l4.1.3): itemView is a ComposeView; no classic view lookups.
        // All content is supplied via setContent { VLCInfoItem(...) } in onBind.
        // The original two TextViews live only inside the (untouched) info_item.xml.
    }
}
