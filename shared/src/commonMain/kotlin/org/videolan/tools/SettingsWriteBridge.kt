package org.videolan.tools

/**
 * Optional platform hook so shared [org.videolan.vlc.viewmodel.SettingsViewModel]
 * writes also update Android SharedPreferences / [HotPlaybackSettings].
 */
object SettingsWriteBridge {
    var onBoolean: ((key: String, value: Boolean) -> Unit)? = null
    var onInt: ((key: String, value: Int) -> Unit)? = null
    var onFloat: ((key: String, value: Float) -> Unit)? = null
    var onString: ((key: String, value: String) -> Unit)? = null
}
