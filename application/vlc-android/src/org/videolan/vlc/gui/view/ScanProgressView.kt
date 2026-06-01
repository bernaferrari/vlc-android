/*
 * ************************************************************************
 *  ScanProgressView.kt
 * *************************************************************************
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
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 */

package org.videolan.vlc.gui.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.videolan.vlc.R
import org.videolan.vlc.compose.interop.VLCAbstractComposeWidget

private data class ScanProgressState(
    val text: String,
    val progress: Int = 0,
    val indeterminate: Boolean = true
)

/**
 * Compose replacement for scan_progress.xml. AudioPlayerContainerActivity owns
 * media-library parsing state and CoordinatorLayout positioning; this view owns
 * the text chip plus determinate/indeterminate progress rendering.
 */
class ScanProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VLCAbstractComposeWidget(context, attrs, defStyleAttr) {

    private var state by mutableStateOf(ScanProgressState(context.getString(R.string.loading_medialibrary)))
    private val textColor = context.resolveComposeColor(R.attr.font_default)
    private val trackColor = context.resolveComposeColor(R.attr.background_default)
    private val progressColor = Color(ContextCompat.getColor(context, R.color.orange500))
    private val chipColor = Color(
        ContextCompat.getColor(
            context,
            if ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) R.color.grey875 else R.color.grey200
        )
    )

    init {
        id = R.id.scan_progress_layout
        visibility = View.GONE
        isClickable = false
        isFocusable = false
    }

    fun showDiscoveryText(text: String?) {
        state = state.copy(text = text.takeUnless { it.isNullOrBlank() } ?: context.getString(R.string.loading_medialibrary))
    }

    fun updateProgress(
        text: String,
        progress: Int,
        indeterminate: Boolean
    ) {
        state = ScanProgressState(
            text = text,
            progress = progress.coerceIn(0, 100),
            indeterminate = indeterminate
        )
    }

    @Composable
    override fun WidgetContent() {
        val current = state
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = current.text,
                color = textColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(chipColor)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(trackColor)
            ) {
                if (current.indeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = progressColor,
                        trackColor = trackColor
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { current.progress / 100F },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = progressColor,
                        trackColor = trackColor
                    )
                }
            }
        }
    }
}

private fun Context.resolveComposeColor(attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return if (typedValue.resourceId != 0) Color(ContextCompat.getColor(this, typedValue.resourceId)) else Color(typedValue.data)
}
