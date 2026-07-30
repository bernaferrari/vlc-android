package org.videolan.mobile.app.delegates

import android.content.Context
import kotlinx.coroutines.launch
import org.videolan.moviepedia.MediaScraper
import org.videolan.resources.ACTION_CONTENT_INDEXING
import org.videolan.tools.AppScope
import org.videolan.tools.InProcessEvents

internal interface IIndexersDelegate {
    fun Context.setupIndexers()
}

internal class IndexersDelegate : IIndexersDelegate {

    override fun Context.setupIndexers() {
        AppScope.launch {
            InProcessEvents.actions(ACTION_CONTENT_INDEXING).collect {
                MediaScraper.indexListener.onIndexingDone()
            }
        }
    }
}
