package org.videolan.vlc.repository

import org.mockito.Mockito

/**
 * Keeps the historical repository tests readable while avoiding Mockito's Java-only
 * `mock(Class)` overload in Kotlin source.
 */
inline fun <reified T> mock(): T = Mockito.mock(T::class.java)
