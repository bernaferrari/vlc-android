/*
 * Copyright © 2026 VLC authors and VideoLAN
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.videolan.tools

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

/** Process-scoped events for work that must stay inside VLC's process. */
object InProcessEvents {
    private val mutableEvents = MutableSharedFlow<Intent>(replay = 0, extraBufferCapacity = 32)
    private val events = mutableEvents.asSharedFlow()

    fun emit(intent: Intent) {
        // Receivers must not be able to mutate the sender's event instance.
        mutableEvents.tryEmit(Intent(intent))
    }

    fun actions(vararg actions: String): Flow<Intent> = events.filter { it.action in actions }
}
