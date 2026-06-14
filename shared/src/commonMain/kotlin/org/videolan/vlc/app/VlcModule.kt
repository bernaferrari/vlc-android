package org.videolan.vlc.app

import org.koin.core.module.Module

/**
 * Platform-provided Koin module.
 *
 * Each target must supply this with its concrete implementations:
 *   - [DataStore<Preferences>] for preference storage
 *   - MediaRepository, PlaybackService, etc.
 */
expect val platformModule: Module
