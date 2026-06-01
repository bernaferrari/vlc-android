package org.videolan.vlc.gui.view

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.videolan.resources.util.HeaderProvider
import org.videolan.tools.Settings
import org.videolan.tools.dp

private const val TAG = "RecyclerSectionItemDecoration"

@SuppressLint("LongLogTag")
class RecyclerSectionItemGridDecoration(private val headerOffset: Int, private val space: Int, private val sideSpace: Int, private val sticky: Boolean, private val nbColumns: Int, private val provider: HeaderProvider) : RecyclerView.ItemDecoration() {

    private val headerViews = mutableMapOf<String, SectionHeaderDecorationView>()
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

        //draw current header
        //look if previous header has been drawn
        var previousSectionPosition = 0

        val previousChild = parent.getChildAt(0)
        if (sticky && previousChild != null) {
            val position = parent.getChildAdapterPosition(previousChild)
            val sectionPosition = provider.getPositionForSection(position)
            previousSectionPosition = sectionPosition

            val title = provider.getSectionforPosition(sectionPosition)
            val headerView = getHeaderView(parent, title)
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
            if (provider.isFirstInSection(position)) {
                val headerView = getHeaderView(parent, title)
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

    private fun getHeaderView(parent: RecyclerView, title: String): View {
        val isTv = Settings.showTvUi
        val height = if (isTv) 48.dp else headerOffset
        val headerView = headerViews.getOrPut("${isTv}:$title") {
            SectionHeaderDecorationView(parent.context).apply {
                bind(title, isTv)
            }
        }
        headerView.attachToOverlayHost(parent, height)
        return headerView
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
