package org.videolan.vlc.util

import kotlinx.coroutines.Dispatchers
import org.videolan.tools.CoroutineContextProvider

/** Keeps legacy view-model tests deterministic without relying on Android's main looper. */
class TestCoroutineContextProvider : CoroutineContextProvider() {
    // These models update LiveData after their background query. Using the test
    // main dispatcher retains Android's main-thread invariant while remaining
    // synchronous under Robolectric's test dispatcher.
    override val Default get() = Dispatchers.Main.immediate
    override val IO get() = Dispatchers.Unconfined
    override val Main get() = Dispatchers.Main.immediate
}
