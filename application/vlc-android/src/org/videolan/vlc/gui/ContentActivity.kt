/*
 * *************************************************************************
 *  ContentActivity.java
 * **************************************************************************
 *  Copyright © 2017 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import org.videolan.resources.AndroidDevices
import org.videolan.tools.KEY_ENABLE_CASTING
import org.videolan.tools.Settings
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.RendererDelegate
import org.videolan.vlc.gui.dialogs.showRenderersComposeDialog
import org.videolan.vlc.gui.helpers.UiTools

open class ContentActivity : AudioPlayerContainerActivity(), SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private var showRenderers = !AndroidDevices.isChromeBook && !RendererDelegate.renderers.value.isNullOrEmpty()
    open fun hideRenderers() = false


    override fun initAudioPlayerContainerActivity() {
        super.initAudioPlayerContainerActivity()
        if (!AndroidDevices.isChromeBook && !AndroidDevices.isAndroidTv
                && Settings.getInstance(this).getBoolean(KEY_ENABLE_CASTING, true)) {
            PlaybackService.renderer.observe(this) {
                val item = toolbar.menu.findItem(R.id.ml_menu_renderers) ?: return@observe
                item.isVisible = !hideRenderers() && showRenderers
                item.setIcon(if (!PlaybackService.hasRenderer()) R.drawable.ic_renderer else R.drawable.ic_renderer_on)
            }
            RendererDelegate.renderers.observe(this) { rendererItems ->
                showRenderers = !rendererItems.isNullOrEmpty()
                val item = toolbar.menu.findItem(R.id.ml_menu_renderers)
                if (item != null) item.isVisible = !hideRenderers() && showRenderers
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        UiTools.setOnDragListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_option, menu)
        menu.findItem(R.id.ml_menu_filter).isVisible = false
        menu.findItem(R.id.ml_menu_renderers).isVisible = !hideRenderers() && showRenderers && Settings.getInstance(this).getBoolean(KEY_ENABLE_CASTING, true)
        menu.findItem(R.id.ml_menu_renderers).setIcon(if (!PlaybackService.hasRenderer()) R.drawable.ic_renderer else R.drawable.ic_renderer_on)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.ml_menu_search -> {
                startActivity(Intent(Intent.ACTION_SEARCH, null, this, SearchActivity::class.java))
                return true
            }
            R.id.ml_menu_renderers -> {
                if (!PlaybackService.hasRenderer() && RendererDelegate.renderers.size == 1) {
                    val renderer = RendererDelegate.renderers.value[0]
                    PlaybackService.renderer.value = renderer
                    UiTools.snacker(this, getString(R.string.casting_connected_renderer, renderer.displayName))
                } else {
                    showRenderersComposeDialog()
                }
                return true
            }
            R.id.ml_menu_filter -> {
                if (!item.isActionViewExpanded) setSearchVisibility(true)
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextChange(@Suppress("UNUSED_PARAMETER") filterQueryString: String) = false

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        setSearchVisibility(true)
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        setSearchVisibility(false)
        restoreCurrentList()
        return true
    }

    override fun onQueryTextSubmit(@Suppress("UNUSED_PARAMETER") query: String) = false

    private fun setSearchVisibility(@Suppress("UNUSED_PARAMETER") visible: Boolean) = Unit

    fun closeSearchView() {
        toolbar.menu?.findItem(R.id.ml_menu_filter)?.collapseActionView()
    }

    fun openSearchView() {
        toolbar.menu?.findItem(R.id.ml_menu_filter)?.expandActionView()
    }

    fun isSearchViewVisible() =
        toolbar.menu?.findItem(R.id.ml_menu_filter)?.isActionViewExpanded == true

    fun getCurrentQuery() = ""

    fun setCurrentQuery(@Suppress("UNUSED_PARAMETER") query:String) = Unit


    private fun restoreCurrentList() = Unit

    companion object {
        const val TAG = "VLC/ContentActivity"
    }
}
