package org.videolan.vlc.compose.icons

/**
 * Material Symbols Rounded icons generated from Google's Kotlin vector endpoint.
 *
 * [Filled] maps to FILL=1 (active/selected); [Outlined] maps to FILL=0
 * (inactive/unselected). These are the Material Symbols FILL axis presets.
 */
object MaterialSymbols {
    object Filled {
        val VideoLibrary: MaterialIcon get() = MaterialIcon(filledVideoLibrary)
        val MusicNote: MaterialIcon get() = MaterialIcon(filledMusicNote)
        val Folder: MaterialIcon get() = MaterialIcon(filledFolder)
        val QueueMusic: MaterialIcon get() = MaterialIcon(filledQueueMusic)
        val MoreVert: MaterialIcon get() = MaterialIcon(filledMoreVert)
        val PlayArrow: MaterialIcon get() = MaterialIcon(filledPlayArrow)
        val Shuffle: MaterialIcon get() = MaterialIcon(filledShuffle)
        val Star: MaterialIcon get() = MaterialIcon(filledStar)
        val ArrowBack: MaterialIcon get() = MaterialIcon(filledArrowBack)
    }

    object Outlined {
        val VideoLibrary: MaterialIcon get() = MaterialIcon(outlinedVideoLibrary)
        val MusicNote: MaterialIcon get() = MaterialIcon(outlinedMusicNote)
        val Folder: MaterialIcon get() = MaterialIcon(outlinedFolder)
        val QueueMusic: MaterialIcon get() = MaterialIcon(outlinedQueueMusic)
        val Star: MaterialIcon get() = MaterialIcon(outlinedStar)
    }

    object AutoMirrored {
        object Filled {
            val ArrowBack: MaterialIcon get() = MaterialIcon(filledArrowBack, autoMirror = true)
        }
    }
}
