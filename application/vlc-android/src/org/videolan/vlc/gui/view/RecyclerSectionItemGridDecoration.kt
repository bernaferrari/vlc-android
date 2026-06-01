package org.videolan.vlc.gui.view

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.videolan.resources.util.HeaderProvider
import org.videolan.tools.Settings
import org.videolan.vlc.R

// =============================================================================
// WAVE 1 SECTION HEADER HOST MIGRATION (compose-2l4.1.4 / bd: compose-95d)
// Host file: RecyclerSectionItemGridDecoration.kt (sibling of phone Decoration)
// Composable target: VLCSectionHeader (SectionHeader.kt) - phone + isTv support
// Original XMLs: recycler_section_header.xml (phone) + recycler_section_header_tv.xml (TV)
//
// This variant is used for grid/card layouts in audio browser etc. It also
// handles Settings.showTvUi -> inflates the TV XML (different height/padding).
//
// TV file/media browser routes now use Compose directly; this RecyclerView
// decoration remains for non-TV and legacy RecyclerView hosts.
//
// All other comments (interop patterns, state update for titles, rollback,
// Canvas draw timing considerations, full traceability, Permanent Exceptions,
// hybrid strategy, session completion) are IDENTICAL to the sibling file
// RecyclerSectionItemDecoration.kt - read that one for the canonical text.
// The inflateHeaderView here simply chooses phone vs TV XML; the interop
// sketch would live in the same place (programmatic ComposeView creation).
// =============================================================================

private const val TAG = "RecyclerSectionItemDecoration"

@SuppressLint("LongLogTag")
class RecyclerSectionItemGridDecoration(private val headerOffset: Int, private val space: Int, private val sideSpace: Int, private val sticky: Boolean, private val nbColumns: Int, private val provider: HeaderProvider) : RecyclerView.ItemDecoration() {

    private lateinit var headerView: View
    private lateinit var header: TextView
    var isList = false

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        if (isList && Settings.showHeaders) {
            val pos = parent.getChildAdapterPosition(view)
            if (provider.isFirstInSection(pos)) {
                outRect.top = headerOffset
            }
            if (provider.isLastInSection(pos)) {
                outRect.bottom = space * 2
            }
            return
        }
        if (isList) return

        val pos = parent.getChildAdapterPosition(view)
        val positionForSection = provider.getPositionForSection(pos)
        val isFirstInLine = positionForSection == pos || (pos - positionForSection) % nbColumns == 0
        val isLastInLine = (pos - positionForSection) % nbColumns == nbColumns - 1


        outRect.left = if (isFirstInLine && Settings.showHeaders) sideSpace else space / 2
        outRect.right = if (isLastInLine && Settings.showHeaders) sideSpace else space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        if (Settings.showHeaders) for (i in 0 until nbColumns) {
            if ((pos - i) >= 0 && provider.isFirstInSection(pos - i)) {
                outRect.top = headerOffset + space * 2
            }
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        if (!Settings.showHeaders) return
        if (provider.liveHeaders.value?.isEmpty != false) {
            return
        }

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
            previousSectionPosition = sectionPosition

            val title = provider.getSectionforPosition(sectionPosition)
            header.text = title
            fixLayoutSize(headerView, parent)
            drawHeader(c, parent.getChildAt(0), headerView)
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
                fixLayoutSize(headerView, parent)
                drawHeader(c, child, headerView)
                drawnPositions.add(i)
            }
        }
    }

    private fun drawHeader(c: Canvas, child: View, headerView: View) {
        if (!Settings.showHeaders) return
        c.save()
        if (sticky) {
            c.translate(0f, (child.top - headerView.height - (space * 1.5).toInt()).coerceAtLeast(0).toFloat())
        } else {
            c.translate(0f, (child.top - headerView.height).toFloat())
        }
        headerView.draw(c)
        c.restore()
    }

    private fun inflateHeaderView(parent: RecyclerView): View {
        // =====================================================================
        // WAVE 1 INTEROP SKETCH (compose-2l4.1.4) - see full docs in sibling
        // RecyclerSectionItemDecoration.kt (the phone Decoration).
        //
        // TV branch (showTvUi) intentionally left on XML for this wave.
        // Phone branch would become the programmatic VLCComposeView host
        // exactly as sketched in the other file (state-driven title updates
        // before each drawHeader + Canvas draw).
        //
        // Rollback / safety: identical guarantees. Original two XMLs untouched.
        // =====================================================================
        if (Settings.showTvUi) {
            return LayoutInflater.from(parent.context).inflate(R.layout.recycler_section_header_tv, parent, false)
        }
        return LayoutInflater.from(parent.context).inflate(R.layout.recycler_section_header, parent, false)
    }

    /**
     * Measures the header view to make sure its size is greater than 0 and will be drawn
     * https://yoda.entelect.co.za/view/9627/how-to-android-recyclerview-item-decorations
     */
    private fun fixLayoutSize(view: View, parent: ViewGroup) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec, 0, view.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, view.layoutParams.height)

        view.measure(childWidth, childHeight)

        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    companion object {
        fun getItemSize(screenWidth: Int, nbColumns: Int, spacing: Int, sideSpacing:Int) = ((screenWidth - (spacing * (nbColumns - 1)) - 2 * sideSpacing).toFloat() / nbColumns).toInt()
    }
}
