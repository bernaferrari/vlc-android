package org.videolan.tools

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.videolan.tools.Settings.audioControlsChangeListener
import org.videolan.tools.Settings.init
import org.videolan.tools.Settings.initPostMigration
import java.io.File

object Settings : SingletonHolder<SharedPreferences, Context>({ init(it.applicationContext) }) {

    var firstRun: Boolean = false
    var showVideoThumbs = true
    var tvUI = false
    var listTitleEllipsize = 0
    var overrideTvUI = false
    var videoHudDelay = 2
    var includeMissing = true
    var showHeaders = true
    var showAudioTrackInfo = false
    var videoJumpDelay = 10
    var videoLongJumpDelay = 20
    var videoDoubleTapJumpDelay = 10
    var audioJumpDelay = 10
    var audioLongJumpDelay = 20
    var audioShowTrackNumbers = MutableLiveData(false)
    var showHiddenFiles = false
    var showTrackNumber = true
    var tvFoldersFirst = true
    var incognitoMode = false
    var safeMode = false
    var remoteAccessEnabled = MutableLiveData(false)
    var fastplaySpeed = 2f
    private var audioControlsChangeListener: (() -> Unit)? = null
    lateinit var device : DeviceInfo
        private set

    fun init(context: Context) : SharedPreferences{
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        showVideoThumbs = prefs.getBoolean(SHOW_VIDEO_THUMBNAILS, true)
        tvUI = prefs.getBoolean(PREF_TV_UI, false)
        listTitleEllipsize = prefs.getString(LIST_TITLE_ELLIPSIZE, "0")?.toInt() ?: 0
        videoHudDelay = prefs.getInt(VIDEO_HUD_TIMEOUT, 4).coerceInOrDefault(1, 15, -1)
        device = DeviceInfo(context)
        includeMissing = prefs.getBoolean(KEY_INCLUDE_MISSING, true)
        showHeaders = prefs.getBoolean(KEY_SHOW_HEADERS, true)
        showAudioTrackInfo = prefs.getBoolean(KEY_SHOW_TRACK_INFO, false)
        videoJumpDelay = prefs.getInt(KEY_VIDEO_JUMP_DELAY, 10)
        videoLongJumpDelay = prefs.getInt(KEY_VIDEO_LONG_JUMP_DELAY, 20)
        videoDoubleTapJumpDelay = prefs.getInt(KEY_VIDEO_DOUBLE_TAP_JUMP_DELAY, 10)
        audioJumpDelay = prefs.getInt(KEY_AUDIO_JUMP_DELAY, 10)
        audioLongJumpDelay = prefs.getInt(KEY_AUDIO_LONG_JUMP_DELAY, 20)
        audioShowTrackNumbers.postValue(prefs.getBoolean(KEY_AUDIO_SHOW_TRACK_NUMBERS, false))
        showHiddenFiles = prefs.getBoolean(BROWSER_SHOW_HIDDEN_FILES, !tvUI)
        showTrackNumber = prefs.getBoolean(ALBUMS_SHOW_TRACK_NUMBER, true)
        tvFoldersFirst = prefs.getBoolean(TV_FOLDERS_FIRST, true)
        incognitoMode = prefs.getBoolean(KEY_INCOGNITO, false)
        safeMode = prefs.getBoolean(KEY_SAFE_MODE, false) && prefs.getString(KEY_SAFE_MODE_PIN, "")?.isNotBlank() == true
        remoteAccessEnabled.postValue(prefs.getBoolean(KEY_ENABLE_REMOTE_ACCESS, false))
        return prefs
    }

    /**
     * Init post migration: it can be useful when we migrate a preference by changing its type in [VersionMigration].
     * When doing so, [init] will be called before the migration is done, resulting in a [ClassCastException].
     * This method is called after the migration is done.
     * Once a preference has been moved from [init] to [initPostMigration], it should never be put back in [init].
     *
     * @param context the context
     */
    fun initPostMigration(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        fastplaySpeed = prefs.getInt(FASTPLAY_SPEED, 20) / 10f
    }

    fun Context.isPinCodeSet() = getInstance(this).getString(KEY_SAFE_MODE_PIN, "")?.isNotBlank() == true


    /**
     * Trigger the [audioControlsChangeListener] to update the UI
     */
    fun onAudioControlsChanged() {
        audioControlsChangeListener?.invoke()
    }

    fun setAudioControlsChangeListener(listener:() -> Unit) {
        audioControlsChangeListener = listener
    }

    fun removeAudioControlsChangeListener() {
        audioControlsChangeListener = null
    }

    /**
     * Get the list of keys to blacklist for the backup/restore process
     *
     */
    fun getRestoreBlacklist() = arrayOf(
        //last playlist
        KEY_CURRENT_AUDIO, KEY_CURRENT_MEDIA, KEY_CURRENT_MEDIA_RESUME, KEY_AUDIO_LAST_PLAYLIST,
        KEY_MEDIA_LAST_PLAYLIST, KEY_MEDIA_LAST_PLAYLIST_RESUME, KEY_CURRENT_AUDIO_RESUME_THUMB,
        POSITION_IN_MEDIA_LIST, POSITION_IN_AUDIO_LIST, POSITION_IN_SONG, POSITION_IN_MEDIA,
        //Remote access
        KEYSTORE_PASSWORD_IV, KEY_COOKIE_ENCRYPT_KEY, KEY_COOKIE_SIGN_KEY, KEYSTORE_PASSWORD, ENCRYPTED_KEY_NAME,
        //Others
        KEY_NAVIGATOR_SCREEN_UNSTABLE, KEY_NAVIGATION_ID, KEY_DEBLOCKING, KEY_LAST_SESSION_CRASHED, KEY_METERED_CONNECTION, KEY_MEDIALIBRARY_SCAN

    )

    val showTvUi : Boolean
        get() = !overrideTvUI && device.isTv || tvUI
}

class DeviceInfo(context: Context) {
    val pm = context.packageManager
    val hasTsp = pm.hasSystemFeature("android.hardware.touchscreen")
    val isAndroidTv = pm.hasSystemFeature("android.software.leanback")
    val isChromeBook = pm.hasSystemFeature("org.chromium.arc.device_management")
    val isTv = isAndroidTv || !isChromeBook && !hasTsp
}

@Suppress("UNCHECKED_CAST")
fun SharedPreferences.putSingle(key: String, value: Any) {
    when(value) {
        is Boolean -> edit { putBoolean(key, value) }
        is Int -> edit { putInt(key, value) }
        is Float -> edit { putFloat(key, value) }
        is Long -> edit { putLong(key, value) }
        is String -> edit { putString(key, value) }
        is List<*> -> edit { putStringSet(key, value.toSet() as Set<String>) }
        is Set<*> -> edit { putStringSet(key, value.toSet() as Set<String>) }
        else -> throw IllegalArgumentException("value $value class is invalid!")
    }
}

fun deleteSharedPreferences(context: Context, name: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return context.deleteSharedPreferences(name)
    } else {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(dir, "$name.xml").delete()
    }
}
