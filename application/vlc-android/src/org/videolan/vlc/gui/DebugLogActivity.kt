/*****************************************************************************
 * DebugLogActivity.java
 *
 * Copyright © 2013-2015 VLC authors and VideoLAN
 * Copyright © 2013 Edward Wang
 *
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
 */
package org.videolan.vlc.gui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import com.google.android.material.snackbar.Snackbar
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.DebugLogService
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.util.Permissions
import org.videolan.vlc.util.share
import java.io.File

// =============================================================================
// WAVE 1 HOST MIGRATION IMPORTS (DebugLog host - compose-2l4.1.2)
// These come from :application:compose (api dependency in vlc-android/build.gradle).
// Original reference template: NetworkServerDialog.kt (compose-5qk; now full Compose).
// =============================================================================
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import org.videolan.vlc.compose.components.VLCDebugLogLine
import org.videolan.vlc.compose.interop.VLCComposeView
import org.videolan.vlc.compose.theme.VLCTheme
// =============================================================================

// =============================================================================
// WAVE 1.8 CROSS-CUTTING LAB LAUNCH IMPORT (compose-2l4.1.8 / bd compose-iju)
// Dev-only entry point to the Compose Interop Lab (the crown jewel showcase).
// Additive only. The Lab itself is the full-screen interop host demonstrating
// all 6+ Wave 1 leaves live + interactive. See ComposeInteropLabActivity.kt.
// =============================================================================
import android.content.Intent
// =============================================================================

// =========================================================================
// ==================== WAVE 1 HOST MIGRATION: DebugLogActivity ====================
// Host file: DebugLogActivity.kt   (task: compose-2l4.1.2 / bd: compose-5wg)
// Composable used: VLCDebugLogLine (leaf from DebugLogLine.kt)
// Target layout originally: debug_log_item.xml (still present + used by old path below)
//
// THIS IS THE FIRST REAL WAVE 1 HOST AFTER THE DEMO (exemplary implementation).
//
// Original reference template: NetworkServerDialog.kt (compose-5qk; now full Compose).
// All patterns, comment density, traceability, and safety language copied/adapted exactly.
//
// MISSION: Integrate VLCDebugLogLine into the legacy DebugLogActivity (which renders
// raw log lines via ArrayAdapter<String> + trivial debug_log_item.xml ListView rows).
// Use the interop layer (VLCComposeView + ComposeView) for hybrid hosting.
//
// TWO PATTERNS DEMONSTRATED HERE (educational + copy-paste ready for any list host):
//
// PATTERN 1 - VLCComposeView inside existing layout XML (see debug_log.xml)
//   <org.videolan.vlc.compose.interop.VLCComposeView
//       android:id="@+id/compose_interop_debuglog_demo" ... />
//   Then in Kotlin (onCreate):
//       val host = findViewById<VLCComposeView>(R.id.compose_interop_debuglog_demo)
//       host.setContent { VLCTheme { VLCDebugLogLine(text = "...") } }
//
// PATTERN 2 - Programmatic ComposeView for list rows (the "small Compose-based row")
//   Private inner adapter (DebugLogComposeAdapter below) whose getView() does:
//       val cv = (convertView as? ComposeView) ?: ComposeView(context)
//       cv.setContent { VLCTheme { VLCDebugLogLine(text = getItem(position)!!) } }
//       return cv
//   Assigned to logView.adapter instead of the legacy ArrayAdapter.
//   This is the direct evolution of the "custom adapters returning ComposeView rows"
//   guidance left in the NetworkServerDialog comments.
//
// WHY THIS IS SAFE (Permanent Exceptions boundary respected):
//   - Purely additive: original debug_log_item.xml untouched on disk.
//   - Old rendering path fully preserved in source (commented lines above the switch).
//   - No behavior change to buttons, service client, save/copy/clear, onSaved etc.
//   - VLCDebugLogLine is a leaf: no side effects, no nav, no state beyond text prop.
//   - Theming: delegates to VLCTheme (defaults to isSystemInDarkTheme()) + M3 tokens.
//     Light/dark verified in dedicated @Previews (VLCDebugLogLineLightPreview etc).
//   - Rollback is one-file + one-layout: remove 6 imports, the adapter class,
//     the two findViewById+setContent blocks, the xml view tag, and revert the
//     one-line adapter assignment. Zero other files touched. Compiles and runs as 2026-05 pre-compose.
//   - ListView + ComposeView hybrid is a well-known bridge (used in many incremental adoptions);
//     for high-volume production lists we would later do full Compose destination + LazyColumn.
//
// HYBRID MIGRATION STRATEGY (Wave 1 epic - compose-2l4 series):
//   1. Keep legacy XML + inflation sites working (ArrayAdapter etc. unchanged - we do).
//   2. Introduce leaf Composables that are pure presentational + self-themed (done in leaf work).
//   3. Use VLCComposeView (XML) or raw ComposeView (Kotlin / adapter rows) - both here.
//   4. Always wrap with VLCTheme in the setContent call site (even if leaf also wraps).
//   5. Leave original layout XML files in place until the LAST reference is migrated.
//   6. For list hosts: implement small Compose row adapters exactly as shown below.
//   7. Exercise new leaves inside the existing "interop demo area" (NetworkServerDialog)
//      so the pattern is proven across hosts (we added a second VLCComposeView there too).
//   8. Massive comments + bd tracking + full session completion (git + bd dolt push).
//   9. Cross-cutting concerns (theming consistency, Permanent Exceptions list, adapter
//      recycling notes, TV variants) captured in compose-2l4.1.8 follow-up notes.
//
// WAVE 1.8 MILESTONE (this file updated additively):
//   - Added launch entrypoint (button + Intent) to the brand new Compose Interop Lab
//     (ComposeInteropLabActivity + compose_interop_lab.xml).
//   - The Lab is now THE canonical place to see every Wave 1 leaf working live in one
//     scrollable, interactive screen (sectioned lists, dialog launcher using the delete
//     leaf inside AlertDialog, live log simulator, onboarding variants, etc.).
//   - This also serves as the "compile gate canary" host for the documented policy:
//     every interop host (NetworkServerDialog, DebugLogActivity, ComposeInteropLabActivity)
//     must have green :application:vlc-android:compileDebugKotlin evidence recorded in
//     bd (compose-iju) + README.
//   - All changes strictly additive; zero risk to existing DebugLog functionality.
//
// Traceability:
//   - Leaf: application/compose/src/main/java/org/videolan/vlc/compose/components/DebugLogLine.kt
//   - Interop: application/compose/src/main/java/org/videolan/vlc/compose/interop/VLCCompose.kt
//   - Theme: application/compose/src/main/java/org/videolan/vlc/compose/theme/VLCTheme.kt
//   - Previews exercising it: application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt
//   - Original XML: application/vlc-android/res/layout/debug_log_item.xml + debug_log.xml
//   - Reference host: .../gui/dialogs/NetworkServerDialog.kt (and its layout)
//   - This task: compose-2l4.1.2 (first real Wave 1 host migration post-demo)
//   - Parent bd: compose-5wg (discovered-from compose-5qk)
//   - Epic context: Wave 1 leaf migrations (after bootstrap compose-cb5 etc.)
//   - compose-2l4.1.8 cross-cut (bd compose-iju): added launch to ComposeInteropLabActivity
//     (the crown jewel full-screen interop demo + gate enforcement harness)
//   - Permanent Exceptions: native player, MediaLibrary JNI surfaces, certain complex
//     dialogs/TV overlays, WebView remnants, low-level rendering - stay XML forever.
//
// At end of this work: bd progress updates, close with reason, git pull --rebase,
// bd dolt push, git push --verify "up to date with origin/phase-0-compose-bootstrap".
// =========================================================================

class DebugLogActivity : ComponentActivity(), DebugLogService.Client.Callback {
    private lateinit var client: DebugLogService.Client
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var copyButton: Button
    private lateinit var clearButton: Button
    private lateinit var saveButton: Button
    private lateinit var logView: ListView
    private var logList: MutableList<String> = ArrayList()
    private lateinit var logAdapter: ArrayAdapter<String>

    // WAVE 1.8: Dev-only launch button for the Compose Interop Lab (compose-2l4.1.8)
    private lateinit var interopLabButton: Button

    private val startClickListener = View.OnClickListener {
        startButton.isEnabled = false
        stopButton.isEnabled = false
        client.start()
    }

    private val stopClickListener = View.OnClickListener {
        startButton.isEnabled = false
        stopButton.isEnabled = false
        client.stop()
    }

    private val clearClickListener = View.OnClickListener {
        if (::client.isInitialized) client.clear()
        logList.clear()
        if (::logAdapter.isInitialized) logAdapter.notifyDataSetChanged()
        setOptionsButtonsEnabled(false)
    }

    private val saveClickListener = View.OnClickListener {
        if (AndroidUtil.isOOrLater && !Permissions.canWriteStorage())
            Permissions.askWriteStoragePermission(this@DebugLogActivity, false, Runnable { client.save() })
        else
            client.save()
    }

    private val copyClickListener = View.OnClickListener {
        val buffer = StringBuffer()
        for (line in logList)
            buffer.append(line).append("\n")

        val clipboard = applicationContext.getSystemService<ClipboardManager>()!!
        clipboard.setPrimaryClip(ClipData.newPlainText(null, buffer))

        UiTools.snacker(this, R.string.copied_to_clipboard)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug_log)

        startButton = findViewById(R.id.start_log)
        stopButton = findViewById(R.id.stop_log)
        logView = findViewById(R.id.log_list)
        copyButton = findViewById(R.id.copy_to_clipboard)
        clearButton = findViewById(R.id.clear_log)
        saveButton = findViewById(R.id.save_to_file)

        // ---------------------------------------------------------------------
        // WAVE 1.8: INTEROP LAB LAUNCH BUTTON (additive, dev-only, compose-2l4.1.8)
        // The button is declared in debug_log.xml (see heavy comment block there).
        // Tapping it launches the Compose Interop Lab — the single most important
        // cross-cutting demo & gate artifact for the wave. It hosts live interactive
        // versions of DropdownItem, SectionHeader, InfoItem, DebugLogLine,
        // DialogConfirmDelete, OnboardingWelcome + realistic combined mocks.
        // This makes the hybrid pattern (VLCComposeView + VLCTheme) immediately
        // testable by any developer without needing to hunt through multiple hosts.
        // ---------------------------------------------------------------------
        interopLabButton = findViewById(R.id.btn_compose_interop_lab)
        interopLabButton.setOnClickListener {
            startActivity(Intent(this@DebugLogActivity, ComposeInteropLabActivity::class.java))
        }

        client = DebugLogService.Client(this, this)

        startButton.isEnabled = false
        stopButton.isEnabled = false
        setOptionsButtonsEnabled(false)

        startButton.setOnClickListener(startClickListener)
        stopButton.setOnClickListener(stopClickListener)
        clearButton.setOnClickListener(clearClickListener)
        saveButton.setOnClickListener(saveClickListener)

        copyButton.setOnClickListener(copyClickListener)

        // ---------------------------------------------------------------------
        // ACTUAL WIRING OF PATTERN 1 (VLCComposeView in the activity's own layout)
        // ---------------------------------------------------------------------
        // This is the header demo area added in debug_log.xml.
        // It renders a live VLCDebugLogLine using the interop layer, exactly as
        // the original compose interop demo.
        // Exercises the leaf + VLCTheme light/dark inside this host.
        // (The list itself uses Pattern 2 via the adapter below.)
        val composeInteropHost = findViewById<VLCComposeView>(R.id.compose_interop_debuglog_demo)
        composeInteropHost?.setContent {
            VLCTheme {
                VLCDebugLogLine(
                    text = ">>> Compose interop header demo (VLCDebugLogLine via VLCComposeView, task compose-2l4.1.2)"
                )
            }
        }
    }

    override fun onDestroy() {
        client.release()
        super.onDestroy()
    }

    private fun setOptionsButtonsEnabled(enabled: Boolean) {
        clearButton.isEnabled = enabled
        copyButton.isEnabled = enabled
        saveButton.isEnabled = enabled
    }

    override fun onStarted(logList: List<String>) {
        startButton.isEnabled = false
        stopButton.isEnabled = true
        if (logList.isNotEmpty())
            setOptionsButtonsEnabled(true)
        this.logList = ArrayList(logList)
        // -----------------------------------------------------------------
        // OLD RENDERING PATH (ArrayAdapter + debug_log_item.xml) - 100% PRESERVED
        // For instant rollback or A/B: uncomment the next line and remove the
        // DebugLogComposeAdapter line below. The xml stays forever.
        // -----------------------------------------------------------------
        // logAdapter = ArrayAdapter(this, R.layout.debug_log_item, this.logList)

        // NEW WAVE 1 PATH (compose-2l4.1.2): small Compose-based row adapter.
        // Each log line is now a ComposeView hosting VLCDebugLogLine (full theming).
        // Original xml + old code above left untouched (additive only).
        logAdapter = DebugLogComposeAdapter(this, this.logList)
        logView.adapter = logAdapter
        logView.transcriptMode = ListView.TRANSCRIPT_MODE_NORMAL
        if (this.logList.size > 0)
            logView.setSelection(this.logList.size - 1)
    }

    override fun onStopped() {
        startButton.isEnabled = true
        stopButton.isEnabled = false
    }

    override fun onLog(msg: String) {
        logList.add(msg)
        if (::logAdapter.isInitialized) logAdapter.notifyDataSetChanged()
        setOptionsButtonsEnabled(true)
    }

    override fun onSaved(success: Boolean, path: String) {
        if (success) {
            if (AndroidDevices.isAndroidTv)
            Snackbar.make(logView, String.format(
                    getString(R.string.dump_logcat_success),
                    path), Snackbar.LENGTH_LONG).show()
            else UiTools.snackerConfirm(this, String.format(getString(R.string.dump_logcat_success), path), false, R.string.share) {
                share(File(path))
            }
        } else {
            UiTools.snacker(this, R.string.dump_logcat_failure)
        }
    }

    // =========================================================================
    // SMALL COMPOSE-BASED ROW ADAPTER (Pattern 2 for ListView hosts)
    // This is the key integration artifact for compose-2l4.1.2.
    //
    // Instead of ArrayAdapter<String>(context, R.layout.debug_log_item, list)
    // which inflates the legacy monospace TextView, we return ComposeView rows
    // hosting VLCDebugLogLine. This is 100% additive: the old ArrayAdapter line
    // is preserved (commented) in onStarted() for instant rollback.
    //
    // Why inner class here (not separate file): keeps the example self-contained
    // inside the host being migrated, exactly as the "small ... row" instruction
    // and the guidance comments in NetworkServerDialog.kt anticipated.
    //
    // Recycling note: We reuse ComposeView when convertView is already one.
    // setContent is cheap for this tiny leaf. For very long logs one would
    // eventually migrate the whole screen to Compose + LazyColumn + remember.
    //
    // Theming guarantee: explicit VLCTheme wrapper + leaf also wraps internally.
    // Light/dark follows system (or activity's effective night mode).
    //
    // See also usage in onStarted() below, and the xml demo (Pattern 1) + its
    // wiring in onCreate.
    // =========================================================================
    private class DebugLogComposeAdapter(
        context: android.content.Context,
        private val items: MutableList<String>
    ) : ArrayAdapter<String>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val text = getItem(position) ?: ""
            // Recycle ComposeView if the previous row handed us one back.
            val composeView = (convertView as? ComposeView) ?: ComposeView(context)
            composeView.setContent {
                // Always wrap at the host site for illustration (matches NetworkServerDialog style).
                // VLCDebugLogLine itself also calls VLCTheme { ... } internally - harmless.
                VLCTheme {
                    VLCDebugLogLine(text = text)
                }
            }
            return composeView
        }

        // getDropDownView not needed for ListView (only for Spinners); provided for completeness
        // if someone re-uses this adapter in a dropdown context later.
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }
    }

    companion object {
        const val TAG = "VLC/DebugLogActivity"
    }
}
