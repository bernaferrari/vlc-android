/*
 * ************************************************************************
 *  FeedbackActivity.kt
 * *************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
import org.videolan.resources.AppContextProvider
import org.videolan.resources.CRASH_HAPPENED
import org.videolan.resources.CRASH_ML_CTX
import org.videolan.resources.CRASH_ML_MSG
import org.videolan.resources.TV_PREFERENCE_ACTIVITY
import org.videolan.resources.util.applyOverscanMargin
import org.videolan.tools.Settings
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.DebugLogService
import org.videolan.vlc.R
import org.videolan.vlc.compose.components.VLCFeedbackScreen
import org.videolan.vlc.gui.helpers.FeedbackUtil
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.gui.preferences.EXTRA_PREF_END_POINT
import org.videolan.vlc.gui.preferences.PreferencesActivity
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.TextUtils
import org.videolan.vlc.util.openLinkIfPossible
import java.io.File
import java.io.IOException

/**
 * Activity showing the different ways to report some feedback.
 */
class FeedbackActivity : BaseActivity(), DebugLogService.Client.Callback {

    override fun getSnackAnchorView(overAudioPlayer: Boolean) = window.decorView

    private var feedbackTypeEntries by mutableStateOf(emptyList<String>())
    private var selectedFeedbackTypeIndex by mutableStateOf(0)
    private var subject by mutableStateOf("")
    private var message by mutableStateOf("")
    private var messageLabel by mutableStateOf("")
    private var showEmailSupportForm by mutableStateOf(false)
    private var showEmailWarning by mutableStateOf(false)
    private var showIncludes by mutableStateOf(false)
    private var includeMedialibrary by mutableStateOf(false)
    private var includeLogs by mutableStateOf(false)
    private var sendEnabled by mutableStateOf(true)
    private var showForumCard by mutableStateOf(true)
    private var showDocCard by mutableStateOf(true)
    private var installSource by mutableStateOf<Pair<String, String>?>(null)

    private var mlErrorMessage: String? = null
    private var mlErrorContext: String? = null

    // logs
    private var logMessage = ""
    private lateinit var client: DebugLogService.Client
    private lateinit var logcatZipPath: String
    private var snackbarLogs: Snackbar? = null

    override fun onStarted(logList: List<String>) {
        sendEnabled = false
        snackbarLogs = UiTools.snackerMessageInfinite(this, getString(R.string.generating_logs))
        snackbarLogs?.show()
        logMessage = "Starting collecting logs at ${System.currentTimeMillis()}"
        // Initiate a log to wait for before saving, avoiding ANR.
        Log.d("FeedbackActivity", logMessage)
    }

    override fun onStopped() {
    }

    override fun onLog(msg: String) {
        if (msg.contains(logMessage)) {
            if (AndroidUtil.isOOrLater && !Permissions.canWriteStorage())
                Permissions.askWriteStoragePermission(this, false) { client.save() }
            else
                client.save()
        }
    }

    override fun onSaved(success: Boolean, path: String) {
        if (!success) {
            sendEnabled = true
            Snackbar.make(window.decorView, R.string.dump_logcat_failure, Snackbar.LENGTH_LONG).show()
            client.stop()
            return
        }
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val zipReady = withContext(Dispatchers.IO) {
                client.stop()
                snackbarLogs?.dismiss()
                if (!::logcatZipPath.isInitialized) {
                    val externalPath = AppContextProvider.appContext.getExternalFilesDir(null)?.absolutePath
                        ?: return@withContext false
                    logcatZipPath = "$externalPath/logcat.zip"
                }
                val filesToAdd = mutableListOf(path)
                try {
                    AppContextProvider.appContext.getExternalFilesDir(null)?.absolutePath?.let { folder ->
                        File(folder).listFiles()?.forEach {
                            if (it.isFile && (it.name.contains("crash_") || it.name.contains("logcat_"))) filesToAdd.add(it.path)
                        }
                    }
                } catch (exception: IOException) {
                    return@withContext false
                }

                if (!FileUtils.zip(filesToAdd.toTypedArray(), logcatZipPath)) return@withContext false

                try {
                    filesToAdd.forEach { FileUtils.deleteFile(it) }
                } catch (exception: IOException) {
                    return@withContext false
                }

                true
            }

            if (zipReady) sendEmail(true)
            else {
                sendEnabled = true
                Snackbar.make(window.decorView, R.string.dump_logcat_failure, Snackbar.LENGTH_LONG).show()
                client.stop()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = DebugLogService.Client(this, this)
        feedbackTypeEntries = resources.getTextArray(R.array.feedback_entries).map { it.toString() }
        messageLabel = getString(R.string.body)
        installSource = FeedbackUtil.getInstallSource(this)

        mlErrorMessage = intent.extras?.getString(CRASH_ML_MSG)
        mlErrorContext = intent.extras?.getString(CRASH_ML_CTX)
        val isCrashFromML = isCrashFromML()

        if (intent.extras?.getBoolean(CRASH_HAPPENED) == true) {
            showForumCard = false
            showDocCard = false
            installSource = null
            selectedFeedbackTypeIndex = 3
            messageLabel = getString(R.string.describe_crash)
            if (isMailClientPresent()) {
                showEmailSupportForm = true
                updateFormIncludesVisibility()
            } else {
                showEmailWarning = true
            }
        }

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    VLCFeedbackScreen(
                        title = getString(R.string.send_feedback),
                        feedbackForumTitle = getString(R.string.feedback_forum),
                        feedbackForumSummary = TextUtils.separatedString(arrayOf(getString(R.string.send_feedback), getString(R.string.get_help))),
                        readDocTitle = getString(R.string.read_doc),
                        readDocSummary = TextUtils.separatedString(arrayOf(getString(R.string.get_help))),
                        emailSupportTitle = getString(R.string.email_support),
                        emailSupportSummary = TextUtils.separatedString(arrayOf(getString(R.string.send_feedback), getString(R.string.get_help), getString(R.string.report_a_bug))),
                        rateTitle = getString(R.string.rate_us),
                        rateSummary = installSource?.second.orEmpty(),
                        feedbackTypeLabel = getString(R.string.feedback_type),
                        feedbackTypeEntries = feedbackTypeEntries,
                        selectedFeedbackTypeIndex = selectedFeedbackTypeIndex,
                        subject = subject,
                        subjectLabel = getString(R.string.subject),
                        message = message,
                        messageLabel = messageLabel,
                        showForumCard = showForumCard,
                        showDocCard = showDocCard,
                        showRateCard = installSource != null,
                        showEmailSupportForm = showEmailSupportForm,
                        showEmailWarning = showEmailWarning,
                        emailWarningTitle = getString(R.string.feedback_email_warning),
                        emailWarningExplanation = getString(R.string.feedback_email_warning_explanation, getString(R.string.remote_access), getString(R.string.send_feedback)),
                        tryAnywayText = getString(R.string.feedback_email_warning_try_anyway),
                        openSettingsText = getString(R.string.feedback_email_warning_remote_action),
                        showIncludes = showIncludes,
                        includeMedialibrary = includeMedialibrary,
                        includeMedialibraryText = getString(R.string.include_medialib),
                        medialibraryWarning = getString(R.string.ml_database_warning),
                        includeLogs = includeLogs,
                        includeLogsText = getString(R.string.include_logs),
                        logsWarning = getString(R.string.logs_warning),
                        sendText = getString(R.string.send),
                        sendEnabled = sendEnabled,
                        closeContentDescription = getString(R.string.close),
                        onClose = ::finish,
                        onFeedbackForum = { openLinkIfPossible(getString(R.string.forum_url)) },
                        onReadDoc = { openLinkIfPossible(getString(R.string.doc_url)) },
                        onEmailSupport = ::onEmailSupportClicked,
                        onRate = { installSource?.first?.let { openLinkIfPossible(it) } },
                        onTryAnyway = {
                            showEmailWarning = false
                            showEmailSupportForm = true
                            updateFormIncludesVisibility()
                        },
                        onOpenSettings = ::openRemoteAccessSettings,
                        onFeedbackTypeSelected = {
                            selectedFeedbackTypeIndex = it
                            updateFormIncludesVisibility()
                        },
                        onSubjectChange = { subject = it },
                        onMessageChange = { message = it },
                        onIncludeMedialibraryChange = { includeMedialibrary = it },
                        onIncludeLogsChange = { includeLogs = it },
                        onSend = {
                            if (includeLogs) client.start()
                            else sendEmail()
                        },
                        closeIconContent = { FeedbackIcon(R.drawable.ic_close_up) },
                        forumIconContent = { FeedbackIcon(R.drawable.ic_forum) },
                        docIconContent = { FeedbackIcon(R.drawable.ic_documentation) },
                        emailIconContent = { FeedbackIcon(R.drawable.ic_email) },
                        rateIconContent = { FeedbackIcon(R.drawable.ic_rate) },
                        warningIconContent = { FeedbackIcon(R.drawable.ic_warning_small) }
                    )
                }
            }
        )

        if (AndroidDevices.isTv) {
            applyOverscanMargin(this)
        }

        if (isCrashFromML) UiTools.snackerMessageInfinite(this, getString(R.string.ml_crash_send))?.show()
    }

    private fun onEmailSupportClicked() {
        if (!isMailClientPresent()) {
            switchNoEmailVisibility()
        } else {
            switchFormVisibility()
            updateFormIncludesVisibility()
        }
    }

    private fun switchFormVisibility(forceHide: Boolean = false) {
        showEmailSupportForm = !(forceHide || showEmailSupportForm)
        if (!showEmailSupportForm) {
            showIncludes = false
            includeMedialibrary = false
            includeLogs = false
        }
    }

    private fun switchNoEmailVisibility() {
        switchFormVisibility(true)
        showEmailWarning = !showEmailWarning
    }

    private fun openRemoteAccessSettings() {
        lifecycleScope.launch {
            if (Settings.tvUI) {
                val intent = Intent(Intent.ACTION_VIEW).setClassName(this@FeedbackActivity, TV_PREFERENCE_ACTIVITY)
                intent.putExtra(EXTRA_PREF_END_POINT, "remote_access_category")
                startActivity(intent)
            } else {
                PreferencesActivity.launchWithPref(this@FeedbackActivity, "enable_remote_access")
            }
        }
    }

    fun isMailClientPresent(): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        val unsupportedActions = arrayOf("com.android.tv.frameworkpackagestubs", "com.google.android.tv.frameworkpackagestubs", "com.android.fallback")
        val resolved = try {
            intent.resolveActivity(packageManager)
        } catch (e: Exception) {
            return false
        }
        return resolved != null && resolved.packageName !in unsupportedActions
    }

    private fun sendEmail(includeLogs: Boolean = false) {
        val isCrashFromML = isCrashFromML()
        val mail = if (BuildConfig.BETA && selectedFeedbackTypeIndex > 2) FeedbackUtil.SupportType.CRASH_REPORT_EMAIL else FeedbackUtil.SupportType.SUPPORT_EMAIL
        lifecycleScope.launch {
            val emailMessage = if (isCrashFromML)
                buildString {
                    append(message)
                    append("<br /><br />")
                    append("____________________________<br />")
                    append("ML Crash!<br />")
                    append("____________________________<br />")
                    append("ML Context: $mlErrorContext<br />ML error message: $mlErrorMessage")
                }
            else message
            if (!FeedbackUtil.sendEmail(
                    this@FeedbackActivity,
                    mail,
                    showIncludes && includeMedialibrary,
                    emailMessage,
                    subject,
                    if (isCrashFromML) 100 else selectedFeedbackTypeIndex,
                    if (includeLogs) logcatZipPath else null
                )
            ) {
                sendEnabled = true
                UiTools.snacker(this@FeedbackActivity, R.string.feedback_email_warning)
                switchNoEmailVisibility()
            } else {
                finish()
            }
        }
    }

    /**
     * Update the visibility for the form includes section.
     */
    private fun updateFormIncludesVisibility() {
        if (!showEmailSupportForm) {
            showIncludes = false
            return
        }
        showIncludes = selectedFeedbackTypeIndex != 1
        if (selectedFeedbackTypeIndex == 2 || selectedFeedbackTypeIndex == 3) {
            includeMedialibrary = true
            includeLogs = true
        } else {
            includeMedialibrary = false
            includeLogs = false
        }
    }

    private fun isCrashFromML() = !mlErrorContext.isNullOrEmpty() || !mlErrorMessage.isNullOrEmpty()

    override fun onDestroy() {
        job?.complete()
        job = null
        if (::client.isInitialized) client.release()
        super.onDestroy()
    }

    companion object {
        var job: CompletableJob? = null
    }
}

@androidx.compose.runtime.Composable
private fun FeedbackIcon(drawable: Int) {
    Icon(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(24.dp)
    )
}
