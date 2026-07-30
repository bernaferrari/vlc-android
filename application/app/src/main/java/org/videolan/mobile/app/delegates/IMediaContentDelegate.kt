package org.videolan.mobile.app.delegates

import android.content.Context
import kotlinx.coroutines.launch
import org.videolan.moviepedia.provider.MediaScrapingTvshowProvider
import org.videolan.resources.ACTION_OPEN_CONTENT
import org.videolan.resources.EXTRA_CONTENT_ID
import org.videolan.tools.AppScope
import org.videolan.tools.InProcessEvents
import org.videolan.vlc.media.MediaUtils


internal interface IMediaContentDelegate {
    fun Context.setupContentResolvers()
}

internal class MediaContentDelegate : IMediaContentDelegate {
    override fun Context.setupContentResolvers() {
        AppScope.launch {
            InProcessEvents.actions(ACTION_OPEN_CONTENT).collect { intent ->
                val id = intent.getStringExtra(EXTRA_CONTENT_ID) ?: return@collect
                val provider = MediaScrapingTvshowProvider.getProviders().firstOrNull { id.startsWith(it.prefix) } ?: return@collect
                provider.getList(this@setupContentResolvers, id)?.let { results ->
                    MediaUtils.openList(this@setupContentResolvers, results.first, results.second)
                }
            }
        }
    }
}
