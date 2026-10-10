package org.videolan.vlc.kmp

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.videolan.resources.AppContextProvider
import org.videolan.tools.RESULT_RESCAN
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.RESULT_UPDATE_SEEN_MEDIA
import org.videolan.tools.Settings
import org.videolan.vlc.gui.MainActivity
import org.videolan.vlc.settings.SettingsDeepLinks
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * What the app must do once the user leaves Settings. This replaces the legacy
 * `setResult(RESULT_RESTART…)` contract: settings now live inside MainActivity, so the request is
 * delivered through [AndroidSettingsEffects.events] instead of an activity result. Hosts that still
 * finish with a result (the standalone PreferencesActivity) translate it with [resultCode].
 */
enum class AndroidSettingsEffect(val priority: Int, val resultCode: Int) {
    UPDATE_SEEN_MEDIA(1, RESULT_UPDATE_SEEN_MEDIA),
    RESCAN(2, RESULT_RESCAN),
    RESTART(3, RESULT_RESTART),
    RESTART_APP(4, RESULT_RESTART_APP),
}

object AndroidSettingsEffects {
    private val _events = MutableSharedFlow<AndroidSettingsEffect>(extraBufferCapacity = 4)
    /** Collected by MainActivity while started. */
    val events: SharedFlow<AndroidSettingsEffect> = _events.asSharedFlow()

    @Volatile
    private var pending: AndroidSettingsEffect? = null

    /** Records an effect to apply when Settings closes; the strongest pending effect wins. */
    fun request(effect: AndroidSettingsEffect) {
        val current = pending
        if (current == null || effect.priority > current.priority) pending = effect
    }

    /** Returns and clears the pending effect without dispatching it (standalone host). */
    fun take(): AndroidSettingsEffect? = pending.also { pending = null }

    /** Settings closed inside the shared shell: hand the pending effect to the main activity. */
    fun dispatchPending() {
        take()?.let { _events.tryEmit(it) }
    }
}

/** The foreground activity that can present dialogs and pickers for settings side effects. */
internal val settingsHostActivity: ComponentActivity?
    get() = AppContextProvider.currentActivity as? ComponentActivity

private val resultKeys = AtomicInteger()

/** Launches [intent] for a result from code that does not own a registered launcher. */
internal suspend fun ComponentActivity.awaitActivityResult(intent: Intent): ActivityResult =
    suspendCancellableCoroutine { continuation ->
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activityResultRegistry.register(
            "vlc-settings-${resultKeys.incrementAndGet()}",
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            launcher?.unregister()
            if (continuation.isActive) continuation.resume(result)
        }
        continuation.invokeOnCancellation { launcher.unregister() }
        launcher.launch(intent)
    }

const val EXTRA_SETTINGS_KEY = "extra_settings_key"

/**
 * Opens the shared settings at the section that shows [prefKey]. Phones use the main shell (the
 * request is consumed by the shared Nav3 graph); TV keeps its dedicated settings host.
 */
fun openSettingsAt(context: Context, prefKey: String?) {
    if (Settings.showTvUi) {
        val intent = Intent().setClassName(context, org.videolan.resources.TV_PREFERENCE_ACTIVITY)
            .putExtra(EXTRA_SETTINGS_KEY, prefKey)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return
    }
    SettingsDeepLinks.open(key = prefKey)
    if (context is MainActivity) return
    context.startActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    )
}
