package org.videolan.vlc.gui

import android.os.Bundle
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCAuthorsScreen

/**
 * Activity showing the different libraries used by VLC for Android and their licenses
 */
class AuthorsActivity : BaseActivity() {

    private var authors by mutableStateOf(emptyList<String>())

    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCAuthorsScreen(
                        title = getString(R.string.authors),
                        authors = authors,
                        closeContentDescription = getString(R.string.close),
                        onClose = ::finish,
                        closeIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_up),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        authorIconContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_author),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        )
        if (AndroidDevices.isTv) {
            applyOverscanMargin(this)
        }

        lifecycleScope.launch {
            authors = loadAuthors()
        }
    }

    /**
     * Load the authors list from the json file in assets and then populate the list
     */
    private suspend fun loadAuthors(): List<String> = withContext(Dispatchers.IO) {
        val jsonData = AppContextProvider.appResources.openRawResource(R.raw.authors).bufferedReader().use {
            it.readText()
        }

        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(MutableList::class.java, String::class.java)

        val jsonAdapter: JsonAdapter<List<String>> = moshi.adapter(type)

        jsonAdapter.fromJson(jsonData)!!
    }
}
