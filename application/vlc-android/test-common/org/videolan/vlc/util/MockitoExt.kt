package org.videolan.vlc.util

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

/** Kotlin-friendly Mockito helpers shared by JVM and instrumentation tests. */
inline fun <reified T> mock(): T = Mockito.mock(T::class.java)

inline fun <reified T> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)

@Suppress("UNCHECKED_CAST")
fun <T> uninitialized(): T = null as T
