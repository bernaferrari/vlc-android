package org.videolan.television.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A stationary focus ring keeps remote navigation visible without shifting surrounding controls. */
@Composable
internal fun Modifier.tvFocusOutline(): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.hasFocus }
        .border(
            width = 3.dp,
            color = if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
        )
}
