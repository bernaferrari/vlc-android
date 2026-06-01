/*
 * ************************************************************************
 *  ColorPickerActivity.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
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

package org.videolan.television.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.television.R
import org.videolan.vlc.compose.theme.VLCTheme
import org.videolan.vlc.compose.theme.VLCThemeDefaults
import org.videolan.vlc.R as VlcR
import kotlin.math.absoluteValue

const val COLOR_PICKER_SELECTED_COLOR = "color_picker_selected_color"
const val COLOR_PICKER_TITLE = "color_picker_title"

class ColorPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOverscanMargin(this)

        val title = intent.extras?.getString(COLOR_PICKER_TITLE) ?: getString(R.string.subtitles_color)
        val previousColor = intent.extras?.getInt(COLOR_PICKER_SELECTED_COLOR) ?: Color.BLACK
        val colorsAndSelection = generateColorsAndSelection(previousColor)
        val closestColorIndex = colorsAndSelection.first
        val colors = colorsAndSelection.second

        val closestVariantIndex = findClosestVariant(colors, closestColorIndex, previousColor)

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    VLCTheme(darkTheme = true) {
                        TvColorPickerScreen(
                            title = title,
                            previousColor = previousColor,
                            colors = colors,
                            initialSelectedIndex = closestColorIndex,
                            initialSelectedVariantIndex = closestVariantIndex,
                            onCancel = ::finish,
                            onConfirm = { selectedColor ->
                                setResult(
                                    RESULT_OK,
                                    Intent(Intent.ACTION_PICK).apply {
                                        putExtra(COLOR_PICKER_SELECTED_COLOR, selectedColor)
                                    }
                                )
                                finish()
                            }
                        )
                    }
                }
            }
        )
    }

    /**
     * Finds the closest color variant index in the [colors]
     *
     * @param colors the colors list
     * @param closestColorIndex the closest colors index in the list depending only on the hue
     * @param previousColor the previous color used
     * @return the index of the closest color variant depending on the saturation and value
     */
    private fun findClosestVariant(colors: ArrayList<Int>, closestColorIndex: Int, @ColorInt previousColor: Int): Int {
        val distances = HashMap<Int, Pair<Float, Float>>()

        for (i in 0..19) {
            val variant = getVariantColor(colors[closestColorIndex], i)
            val satDistance = colorHsvDistance(previousColor, variant, 1)
            val valDistance = colorHsvDistance(previousColor, variant, 2)
            distances[i] = Pair(satDistance, valDistance)
        }

        var closestVariantIndex = 10
        var minSVDistance = 2F
        distances.forEach {
            val combinedDistance = it.value.first + it.value.second
            if (combinedDistance < minSVDistance) {
                closestVariantIndex = it.key
                minSVDistance = combinedDistance
            }
        }
        return closestVariantIndex
    }

    /**
     * Generate the colors list by making the hue vary
     * and also determine the closest color from the previous selected on
     *
     * @param previousColor the previous color used
     * @return a pair with first being the index of the closest color from [previousColor] and the full color list
     */
    private fun generateColorsAndSelection(@ColorInt previousColor: Int): Pair<Int, ArrayList<Int>> {
        var minHueDistance = colorHsvDistance(previousColor, Color.GRAY)
        var closestColorIndex = 0
        val colors = ArrayList<Int>(100)
        colors.add(Color.GRAY)
        closestColorIndex = colors.size - 1
        var hue = 1
        while (hue < 100) {
            val color = Color.HSVToColor(floatArrayOf(3.6F * hue, 1F, 1F))
            colors.add(color)
            val colorHueDistance = colorHsvDistance(previousColor, color)
            if (colorHueDistance < minHueDistance) {
                minHueDistance = colorHueDistance
                closestColorIndex = colors.size - 1
            }
            hue++
        }
        return Pair(closestColorIndex, colors)
    }

    /**
     * Calculate a color distance between two colors on one HSV component
     *
     * @param color1 the first color
     * @param color2 th second color
     * @param hsvIndex the HSV index to use (0 is hue, 1 is saturation, 2 is value)
     * @return
     */
    private fun colorHsvDistance(@ColorInt color1: Int, @ColorInt color2: Int, hsvIndex: Int): Float {
        if (hsvIndex !in 0..2) throw IllegalStateException("hsvIndex must be between 0 and 2")
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.colorToHSV(color1, hsv1)
        Color.colorToHSV(color2, hsv2)
        return (hsv1[hsvIndex] - hsv2[hsvIndex]).absoluteValue
    }

    private fun colorHsvDistance(@ColorInt color1: Int, @ColorInt color2: Int): Float {

        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.colorToHSV(color1, hsv1)
        Color.colorToHSV(color2, hsv2)
        return (hsv1[0] - hsv2[0]).absoluteValue + (hsv1[1] - hsv2[1]).absoluteValue + (hsv1[2] - hsv2[2]).absoluteValue
    }

}

@Composable
private fun TvColorPickerScreen(
    title: String,
    @ColorInt previousColor: Int,
    colors: List<Int>,
    initialSelectedIndex: Int,
    initialSelectedVariantIndex: Int,
    onCancel: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(initialSelectedIndex) }
    var selectedVariantIndex by rememberSaveable { mutableIntStateOf(initialSelectedVariantIndex) }
    var previewColor by rememberSaveable {
        mutableIntStateOf(getVariantColor(colors[initialSelectedIndex], initialSelectedVariantIndex))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VLCThemeDefaults.colors.backgroundDefault)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            color = VLCThemeDefaults.colors.fontDefault,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ColorGrid(
            colors = colors,
            selectedIndex = selectedIndex,
            selectedVariantIndex = selectedVariantIndex,
            focusRequester = focusRequester,
            onColorSelected = { index ->
                selectedIndex = index
                selectedVariantIndex = 9
                previewColor = getVariantColor(colors[index], selectedVariantIndex)
            },
            onVariantSelected = { variant ->
                selectedVariantIndex = variant
                previewColor = getVariantColor(colors[selectedIndex], variant)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ColorPreviewRow(
            previousColor = previousColor,
            newColor = previewColor
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
            Button(
                onClick = { onConfirm(getVariantColor(colors[selectedIndex], selectedVariantIndex)) }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        }
    }
}

@Composable
private fun ColorGrid(
    colors: List<Int>,
    selectedIndex: Int,
    selectedVariantIndex: Int,
    focusRequester: FocusRequester,
    onColorSelected: (Int) -> Unit,
    onVariantSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(20),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
    ) {
        itemsIndexed(colors) { index, color ->
            ColorSwatch(
                color = color,
                selected = index == selectedIndex,
                modifier = if (index == selectedIndex) Modifier.focusRequester(focusRequester) else Modifier,
                onClick = { onColorSelected(index) }
            )
        }
    }
    HorizontalDivider(
        color = colorResource(R.color.grey800),
        modifier = Modifier.padding(vertical = 12.dp)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(20),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        items(20) { index ->
            ColorSwatch(
                color = getVariantColor(colors[selectedIndex], index),
                selected = index == selectedVariantIndex,
                onClick = { onVariantSelected(index) }
            )
        }
    }
}

@Composable
private fun ColorPreviewRow(
    @ColorInt previousColor: Int,
    @ColorInt newColor: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.previous_color),
            color = VLCThemeDefaults.colors.fontDefault
        )
        ColorSwatch(
            color = previousColor,
            selected = false,
            size = 48
        )
        ColorSwatch(
            color = newColor,
            selected = false,
            size = 48
        )
        Text(
            text = stringResource(R.string.new_color),
            color = VLCThemeDefaults.colors.fontDefault
        )
    }
}

@Composable
private fun ColorSwatch(
    @ColorInt color: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 32,
    onClick: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val clickableModifier = if (onClick != null) {
        Modifier
            .clickable(onClick = onClick)
            .focusable()
    } else {
        Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .onFocusChanged { focused = it.isFocused }
            .then(clickableModifier)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) VLCThemeDefaults.colors.primary else colorResource(R.color.grey500),
                shape = CircleShape
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(ComposeColor(color))
    ) {
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size((size - 10).coerceAtLeast(18).dp)
                    .clip(CircleShape)
                    .background(ComposeColor.Black.copy(alpha = 0.5f))
            ) {
                Image(
                    painter = painterResource(VlcR.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size((size - 16).coerceAtLeast(14).dp)
                )
            }
        }
    }
}

@ColorInt
private fun getVariantColor(@ColorInt color: Int, position: Int): Int {
    if (color == Color.GRAY) {
        val value = 1F - (0.05F * position)
        return Color.HSVToColor(floatArrayOf(0F, 0F, value))
    }
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    if (position <= 9) hsv[1] = 0.1F * position
    else hsv[2] = 1F - (0.1F * (position - 9))
    return Color.HSVToColor(hsv)
}
