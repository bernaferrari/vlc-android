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
        addActivityOptionsMenu(menu)
        menu.findItem(R.id.ml_menu_filter).isVisible = false
        menu.findItem(R.id.ml_menu_renderers).isVisible = !hideRenderers() && showRenderers && Settings.getInstance(this).getBoolean(KEY_ENABLE_CASTING, true)
        menu.findItem(R.id.ml_menu_renderers).setIcon(if (!PlaybackService.hasRenderer()) R.drawable.ic_renderer else R.drawable.ic_renderer_on)
        return true
    }

    private fun addActivityOptionsMenu(menu: Menu) {
        menu.add(Menu.NONE, R.id.pin_relocked, 0, R.string.lock_with_pin).apply {
            setIcon(R.drawable.ic_pin_lock)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(Menu.NONE, R.id.play_all, 0, R.string.play_all).apply {
            setIcon(R.drawable.ic_play)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(Menu.NONE, R.id.shuffle_all, 0, R.string.shuffle_all_title).apply {
            setIcon(R.drawable.ic_shuffle)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(Menu.NONE, R.id.ml_menu_renderers, 1, R.string.cast_option_title).apply {
            setIcon(R.drawable.ic_renderer)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }
        menu.add(Menu.NONE, R.id.ml_menu_filter, 1, R.string.searchable_hint).apply {
            setIcon(R.drawable.ic_search)
            actionView = SearchView(this@ContentActivity)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }
        menu.add(Menu.NONE, R.id.ml_menu_last_playlist, 2, R.string.resume_playback_short_title).apply {
            setIcon(R.drawable.ic_last_playqueue)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }
        menu.add(Menu.NONE, R.id.ml_menu_display_options, 2, R.string.display_settings).apply {
            setIcon(R.drawable.ic_display_settings)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }
        menu.add(Menu.NONE, R.id.incognito_mode, 2, R.string.incognito_mode).apply {
            isCheckable = true
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.ml_menu_scan, 3, R.string.add_to_scanned).apply {
            setIcon(R.drawable.ic_add_to_scan)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        }
        menu.add(Menu.NONE, R.id.ml_menu_display_list, 3, R.string.display_in_list).apply {
            setIcon(R.drawable.ic_view_list)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.browse_network, 3, R.string.browse_network).apply {
            isCheckable = true
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.ml_menu_display_grid, 3, R.string.display_in_grid).apply {
            setIcon(R.drawable.ic_view_grid)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.addSubMenu(Menu.NONE, R.id.ml_menu_sortby, 2, R.string.sortby).apply {
            item.setIcon(R.drawable.ic_sort)
            item.isVisible = false
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            add(Menu.NONE, R.id.ml_menu_sortby_name, 2, R.string.sortby_name)
            add(Menu.NONE, R.id.ml_menu_sortby_filename, 2, R.string.sortby_filename)
            add(Menu.NONE, R.id.ml_menu_sortby_artist_name, 2, R.string.sortby_artist_name).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_album_name, 2, R.string.sortby_album_name).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_length, 2, R.string.sortby_length).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_date, 2, R.string.sortby_date).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_last_modified, 2, R.string.sortby_last_modified_date).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_insertion_date, 2, R.string.sortby_insertion).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_number, 2, R.string.sortby_number).isVisible = false
            add(Menu.NONE, R.id.ml_menu_sortby_media_number, 2, R.string.sortby_media_number)
        }
        menu.addSubMenu(Menu.NONE, R.id.ml_menu_add_playlist, 3, R.string.add_to_playlist).apply {
            item.isVisible = false
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            add(Menu.NONE, R.id.folder_add_playlist, 2, R.string.this_folder)
            add(Menu.NONE, R.id.subfolders_add_playlist, 2, R.string.all_subfolders)
        }
        menu.add(Menu.NONE, R.id.add_server_favorite, 3, R.string.add_server_favorite).apply {
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.pin_unlock, 4, R.string.unlock_with_pin).apply {
            setIcon(R.drawable.ic_pin_unlock)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.ml_menu_refresh, 4, R.string.refresh).apply {
            setIcon(R.drawable.ic_refresh)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        menu.add(Menu.NONE, R.id.ml_menu_custom_dir, 3, R.string.add_custom_path).apply {
            isVisible = false
        }
        menu.add(Menu.NONE, R.id.rename_group, 5, R.string.rename_group).apply {
            setIcon(R.drawable.ic_edit)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        menu.add(Menu.NONE, R.id.ungroup, 5, R.string.ungroup).apply {
            setIcon(R.drawable.ic_delete)
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
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
