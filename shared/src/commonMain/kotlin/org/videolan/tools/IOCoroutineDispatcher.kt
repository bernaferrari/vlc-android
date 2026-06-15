package org.videolan.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Platform-specific IO dispatcher.
 * On JVM/Android this is [Dispatchers.IO]; on iOS it falls back to [Dispatchers.Default].
 */
internal expect val IOCoroutineDispatcher: CoroutineDispatcher
