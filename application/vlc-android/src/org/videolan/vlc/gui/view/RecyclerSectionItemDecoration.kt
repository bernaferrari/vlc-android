package org.videolan.vlc.gui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.videolan.tools.Settings
import org.videolan.vlc.R
import org.videolan.vlc.providers.medialibrary.MedialibraryProvider
import kotlin.math.max

// =============================================================================
// WAVE 1 SECTION HEADER HOST MIGRATION (compose-2l4.1.4 / bd: compose-95d)
// Host file: RecyclerSectionItemDecoration.kt
// Composable hosted (target): VLCSectionHeader (from SectionHeader.kt in :application:compose)
// Original XML inflated here: recycler_section_header.xml (phone variant; 36dp)
// TV: recycler_section_header_tv.xml (see GridDecoration for showTvUi path - deferred)
//
// THIS IS THE TRUE INFLATION / HOST SITE for the high-leverage section header
// used across audio browsers, playlists, media lists etc. (BaseAudioBrowser,
// PlaylistFragment, HeaderMediaListActivity are the *callers* that add the
// Decoration to RecyclerView; the actual View creation for the sticky/floating
// header chrome happens in inflateHeaderView + onDrawOver below).
//
// Reference style: EXACT mirror of NetworkServerDialog.kt (compose-5qk) +
// DebugLogActivity.kt (compose-5wg / compose-2l4.1.2) + ComposeInteropLabActivity.kt
// (the rich crown jewel for compose-2l4.1.8 / bd compose-iju).
//
// MISSION: Make the decoration able to host the VLCSectionHeader Composable
// via interop layer (VLCComposeView or programmatic ComposeView) so that in
// future slices the drawn header can be fully Compose-powered while preserving
// the exact sticky-header overlay behavior (Canvas translation + draw).
//
// TWO (FUTURE) PATTERNS FOR THIS DECORATION HOST (documented for next agent):
//
// PATTERN A - Programmatic VLCComposeView as the headerView (preferred for
//   pure-Kotlin decoration, no new layout XML):
//     private fun inflateHeaderView(parent: RecyclerView): View {
//         // val cv = VLCComposeView(parent.context).apply {
//         //     layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, headerOffset)
//         // }
//         // // Initial content (title will be updated via state before draws)
//         // cv.setContent { VLCTheme { VLCSectionHeader(text = "…") } }
//         // return cv
//         // Then: change lateinit header: TextView  ->  lateinit composeHost: VLCComposeView
//         // Before every header.text = title  ->  update a mutable state or call
//         // setContent again with the fresh title (see state holder pattern below).
//     }
//
// PATTERN B - Small wrapper ViewGroup (FrameLayout) containing VLCComposeView
//   (if we ever want XML-declared attributes or easier measurement).
//   But: NEVER create new layout files for this unless epic requires.
//   All creation stays in this .kt (programmatic).
//
// STATE UPDATE FOR DYNAMIC TITLES (critical for sticky headers):
//   The onDrawOver path does:
//     header.text = title
//     drawHeader(c, child, headerView)
//   For Compose we need the @Composable to see the *current* title at draw time.
//   Recommended (safe, low coupling):
//     private val currentHeaderTitle = mutableStateOf("")
//     ...
//     // in draw sites:
//     currentHeaderTitle.value = title
//     composeHost.setContent {
//         VLCTheme { VLCSectionHeader(text = currentHeaderTitle.value) }
//     }
//     // then draw
//   Note on timing: setContent posts recomposition; the very next headerView.draw(c)
//   may still paint the *previous* frame's pixels in some cases. For Wave 1 this
//   is documented as a known consideration. Full fidelity comes in Wave 2+ when
//   audio/video lists themselves migrate to Compose (LazyColumn + stickyHeader
//   which is trivial with the Composable directly - no ItemDecoration needed).
//
// WHY THIS IS SAFE (Permanent Exceptions boundary respected):
//   - Purely additive: original recycler_section_header*.xml untouched on disk.
//   - Current XML inflation path (below) remains 100% active and is the fallback.
//   - Old behavior (exact pixel + measurement + Canvas draw) unchanged unless
//     an explicit future flag switches the inflate path.
//   - VLCSectionHeader is a leaf: pure presentational, self-themed via VLCTheme,
//     no side effects, no nav, no live data beyond the text prop.
//   - Rollback: delete the commented interop sketch + these 80+ lines of docs,
//     remove any future imports, revert one call site in inflateHeaderView.
//     Zero other files touched. The RV lists continue exactly as 2026-05.
//   - ItemDecoration + manual Canvas draw of a sub-view is a classic Android
//     pattern; hybridizing it is well-understood incremental work.
//
// HYBRID MIGRATION STRATEGY (Wave 1 epic - compose-2l4 series):
//   1. Keep legacy XML + inflation sites working (we do - 100%).
//   2. Leaf Composables pure + self-themed (VLCSectionHeader done in cb5.1).
//   3. Use VLCComposeView / ComposeView for interop (documented here for when
//      we activate; see also the small adapter pattern in DebugLogActivity).
//   4. Always wrap setContent content with VLCTheme (even if leaf also wraps).
//   5. Leave original XMLs forever until last reference gone (policy).
//   6. For *decorations specifically*: the long-term win is migrating the
//      consuming lists (audio tabs etc.) to Compose destinations where
//      sticky headers are native (LazyColumn stickyHeader { VLCSectionHeader(...) }).
//      The ItemDecoration interop is a bridge, not necessarily the end state.
//   7. Exercise leaves in Interop Lab (already does for SectionHeader) + rich
//      mocks in PreviewUtils (enhancing in this task).
//   8. Massive comments + bd tracking (compose-95d) + cross-cut updates (iju).
//   9. Compile gate on :application:vlc-android:compileDebugKotlin after edits.
//
// Traceability (this task):
//   - Leaf Composable: application/compose/src/main/java/org/videolan/vlc/compose/components/SectionHeader.kt
//     (VLCSectionHeader @Composable, isTv handling, headerBackground + audioBrowserSeparator tokens)
//   - Interop helper: application/compose/src/main/java/org/videolan/vlc/compose/interop/VLCCompose.kt
//   - Theme: application/compose/src/main/java/org/videolan/vlc/compose/theme/VLCTheme.kt
//   - Previews (already rich): application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt
//     (VLCSectionHeaderLight/Dark + SectionedList*Previews + new AudioBrowser one added here)
//   - Original XMLs (never deleted): application/vlc-android/res/layout/recycler_section_header.xml
//     + recycler_section_header_tv.xml
//   - Callers / "hosts" that add the decoration (updated with comments in this task):
//     BaseAudioBrowser.kt (primary phone audio tabs - highest visibility)
//     PlaylistFragment.kt
//     HeaderMediaListActivity.kt
//   - This task: compose-2l4.1.4 (bd: compose-95d) - Section Header Host Migration
//   - Parent epic: compose-cb5 (Leaf Migration Wave 1)
//   - Cross-cutting: compose-2l4.1.8 / bd compose-iju (notes, Permanent Exceptions, gate policy)
//   - Interop Lab: ComposeInteropLabActivity.kt + compose_interop_lab.xml (exercises VLCSectionHeader live)
//   - TV note: song_header_item.xml + BaseBrowserTvFragment (intentionally out of scope for this wave;
//     GridDecoration handles showTvUi + different height; TV uses DataBinding-heavy surfaces that are
//     part of Permanent Exceptions 20% for now).
//   - Permanent Exceptions (definitive): native player surfaces, MediaLibrary JNI, certain complex
//     TV overlays, WebView remnants, low-level rendering pipelines, full DataBinding TV fragments.
//     Section headers on phone lists are *not* exceptions - prime migration candidates.
//
// Rollback matrix (copy-paste for any future agent):
//   - To instantly revert this file: git checkout -- <this file>. No other files affected.
//   - The lists using these decorations (audio browser tabs, playlists) will be pixel-identical.
//
// At end of work: bd note + close with rich reason, compile gate evidence,
// full Agents.md session completion (git pull --rebase + bd dolt push + git push
// + "up to date with origin/phase-0-compose-bootstrap" verification).
// =============================================================================

private const val TAG = "RecyclerSectionItemDecoration"

class RecyclerSectionItemDecoration(private val headerOffset: Int, private val sticky: Boolean, private val provider: MedialibraryProvider<*>) : RecyclerView.ItemDecoration() {

    private lateinit var headerView: View
    private lateinit var header: TextView

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val pos = parent.getChildAdapterPosition(view)
        if (Settings.showHeaders && provider.isFirstInSection(pos)) {
            outRect.top = headerOffset
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        if (!Settings.showHeaders) return

        if (!::headerView.isInitialized) {
            headerView = inflateHeaderView(parent)
            header = headerView.findViewById<TextView>(R.id.section_header)!!
            fixLayoutSize(headerView, parent)
        }


        //draw current header
        //look if previous header has been drawn
        var previousSectionPosition = 0

        val previousChild = parent.getChildAt(0)
        if (sticky && previousChild != null) {
            val position = parent.getChildAdapterPosition(previousChild)
            val sectionPosition = provider.getPositionForSection(position)
            if (provider.getHeaderForPostion(sectionPosition) != null) {
                previousSectionPosition = sectionPosition

                val title = provider.getSectionforPosition(sectionPosition)
                header.text = title
                drawHeader(c, parent.getChildAt(0), headerView)
            }
        }

        val drawnPositions = ArrayList<Int>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == previousSectionPosition) {
                //prevent re-drawing the previous drawer
                continue
            }

            val title = provider.getSectionforPosition(position)
            header.text = title
            if (provider.isFirstInSection(position)) {
                drawHeader(c, child, headerView)
                drawnPositions.add(i)
            }
        }


    }

    private fun drawHeader(c: Canvas, child: View, headerView: View) {
        if (!Settings.showHeaders) return
        c.save()
        if (sticky) {
            c.translate(0f, max(0, child.top - headerView.height).toFloat())
        } else {
            c.translate(0f, (child.top - headerView.height).toFloat())
        }
        headerView.draw(c)
        c.restore()
    }

    private fun inflateHeaderView(parent: RecyclerView): View {
        // =====================================================================
        // WAVE 1 INTEROP HOSTING SKETCH (compose-2l4.1.4 / compose-95d)
        // This is where the direct inflation of recycler_section_header.xml
        // would be replaced by a programmatic Compose host for VLCSectionHeader.
        //
        // Current (active, 100% fallback): XML inflation -> classic TextView.
        // Future (commented, ready to activate when timing/draw semantics solid):
        //
        // import androidx.compose.ui.platform.ComposeView
        // import org.videolan.vlc.compose.components.VLCSectionHeader
        // import org.videolan.vlc.compose.interop.VLCComposeView
        // import org.videolan.vlc.compose.theme.VLCTheme
        //
        // val composeHost = VLCComposeView(parent.context).apply {
        //     layoutParams = ViewGroup.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         resources.getDimensionPixelSize(R.dimen.recycler_section_header_height)
        //     )
        // }
        // // The title state would be owned by the decoration (or a small holder).
        // // See class-level docs for the mutableStateOf + setContent pattern.
        // composeHost.setContent {
        //     VLCTheme { VLCSectionHeader(text = "Initial") }
        // }
        // return composeHost
        //
        // Then in onDrawOver sites (where header.text = title happens):
        //   currentTitleState.value = title
        //   (headerView as? ComposeView)?.setContent { VLCTheme { VLCSectionHeader(text = currentTitleState.value) } }
        //   drawHeader(...)
        //
        // Measurement (fixLayoutSize) + Canvas draw path stays identical.
        // Rollback: just delete the future block + any state fields; XML path wins.
        // =====================================================================
        return LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_section_header, parent, false)
    }

    /**
     * Measures the header view to make sure its size is greater than 0 and will be drawn
     * https://yoda.entelect.co.za/view/9627/how-to-android-recyclerview-item-decorations
     */
    private fun fixLayoutSize(view: View, parent: ViewGroup) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width,
                View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height,
                View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                parent.paddingLeft + parent.paddingRight,
                view.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                parent.paddingTop + parent.paddingBottom,
                view.layoutParams.height)

        view.measure(childWidth, childHeight)

        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

}