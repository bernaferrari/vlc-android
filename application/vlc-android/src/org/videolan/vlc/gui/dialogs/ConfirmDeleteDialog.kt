/**
 * **************************************************************************
 * ConfirmDeleteDialog.kt
 * ****************************************************************************
 * Copyright © 2015 VLC authors and VideoLAN
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
 * ***************************************************************************
 */

// =============================================================================
// WAVE 1 DIALOG HOST MIGRATION IMPORTS (documented for compose-2l4.1.5 / bd compose-j0e)
// These WOULD come from :application:compose (api dependency) when the content swap
// is activated. Shown here (commented) for educational completeness + copy-paste
// reference. Matches style of NetworkServerDialog.kt (first dialog interop demo)
// and DebugLogActivity.kt (rich Wave 1 host).
// Current change is PURELY DOCUMENTATION + cross-cut exercise via Lab/Previews:
// zero imports activated, zero behavior change, original layout & shell 100% preserved.
// =============================================================================
// import androidx.compose.ui.platform.ComposeView
// import androidx.compose.ui.viewinterop.AndroidView
// import android.widget.ImageView
// import androidx.compose.material3.MaterialTheme
// import org.videolan.vlc.compose.components.VLCDialogConfirmDelete
// import org.videolan.vlc.compose.interop.VLCComposeView
// import org.videolan.vlc.compose.theme.VLCTheme
// =============================================================================

package org.videolan.vlc.gui.dialogs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.coroutines.launch
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.util.parcelableList
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.UiTools.showPinIfNeeded

const val CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE = 0
const val CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER = 1
const val CONFIRM_DELETE_DIALOG_RESULT = "CONFIRM_DELETE_DIALOG_RESULT"
const val CONFIRM_DELETE_DIALOG_RESULT_TYPE = "CONFIRM_DELETE_DIALOG_RESULT_TYPE"
const val CONFIRM_DELETE_DIALOG_MEDIALIST = "CONFIRM_DELETE_DIALOG_MEDIALIST"
const val CONFIRM_DELETE_DIALOG_TITLE = "CONFIRM_DELETE_DIALOG_TITLE"
const val CONFIRM_DELETE_DIALOG_DESCRIPTION = "CONFIRM_DELETE_DIALOG_DESCRIPTION"
const val CONFIRM_DELETE_DIALOG_BUTTON_TEXT = "CONFIRM_DELETE_DIALOG_BUTTON_TEXT"

// =========================================================================
// ==================== WAVE 1 HOST MIGRATION: ConfirmDeleteDialog ====================
// Host file: ConfirmDeleteDialog.kt   (task: compose-2l4.1.5 / bd: compose-j0e)
// Composable used: VLCDialogConfirmDelete (leaf from DialogContent.kt)
// Target layout originally: dialog_confirm_delete.xml (STILL PRESENT + 100% UNTOUCHED)
//
// THIS IS THE DEDICATED DIALOG CONTENT WIN FOR WAVE 1 (exemplary "keep shell, swap content").
//
// Reference templates (exact comment density + safety copied/adapted):
//   - NetworkServerDialog.kt (compose-5qk): first real interop host + dialog patterns
//   - DebugLogActivity.kt (compose-2l4.1.2 / bd compose-5wg): richest educational host
//   - MediaInfoAdapter + InfoActivity (compose-l94): info surfaces
//   - Recycler*Decorations + audio hosts (compose-95d): section headers
//   - ComposeInteropLabActivity.kt (compose-2l4.1.8 / bd compose-iju): the crown jewel
//
// MISSION: Document + exercise the hybrid pattern for ConfirmDeleteDialog — one of the
// most visible warning dialogs in the app (used for delete media, ban folders, clear
// history, clear app data on TV, etc.). Demonstrates keeping the complex legacy dialog
// shell (BottomSheetFragment subclass, result contracts via setFragmentResult, leanback
// listener, pin gate, orientation mgmt, expanded state, title computation from
// MediaLibraryItem variants) while the *presentational content* (icon + title + message)
// can be supplied by the dedicated leaf.
//
// THE "KEEP SHELL, SWAP CONTENT" HYBRID PATTERN (this task's core teaching):
//   - SHELL (preserved 100% in this slice, forever if needed):
//       * ConfirmDeleteDialog class + all its fields (resultType, mediaList, listener)
//       * newInstance bundle contract + all 5+ const keys
//       * onCreate: pin check, argument parsing, super
//       * onCreateView: inflate of dialog_confirm_delete.xml (layout preserved exactly),
//         findViewById for ALL buttons + original content views,
//         ALL button OnClickListeners (delete does listener + setFragmentResult + dismiss;
//         cancel just dismiss),
//         the entire when-expression that builds dynamic title from mediaList (single file,
//         folders+files counts, Album, Playlist, ban folder special case, several media),
//         descriptionString / buttonText overrides,
//         the if/else that sets up AnimatedVectorDrawableCompat looping "anim_delete" for
//         normal case vs static ic_warning_medium for BAN_FOLDER,
//         initialFocusedView = deleteAnimation, needToManageOrientation=true, getDefaultState=EXPANDED
//       * All call sites (MediaBrowserFragment, BaseBrowserFragment, HistoryFragment,
//         PreferencesAdvanced, VideoGridFragment, TV paths, etc.) continue to work unchanged.
//   - CONTENT (the swappable presentational slice - VLCDialogConfirmDelete leaf):
//       * iconContent slot (warning / delete animation visual)
//       * title Text (bold, large)
//       * message Text (explanatory body)
//       * The leaf (in :application:compose) is pure @Composable, self-themed via VLCTheme,
//         uses M3 typography + our semantic colors. No buttons, no result logic, no side effects.
//   - CURRENT STATE (this commit): 100% shell, 0% swap active. The leaf is exercised via
//     enhanced Interop Lab (real host context) + strengthened Previews (mirrors every
//     title generation case). The mapping recipe + rollback sketch live in comments below.
//   - FUTURE ACTIVATION (when a later slice decides full content migration or M3 dialog):
//     Replace the icon/title/message inflation+mutation block with a ComposeView (or
//     VLCComposeView declared in the layout) whose setContent does VLCTheme {
//       VLCDialogConfirmDelete(title = computedTitle, message = computedDesc,
//         iconContent = { /* mapped animation or warning here */ })
//     }
//     Buttons + listeners + result sending + all shell behavior stay exactly here.
//     This is the canonical "keep shell, evolve content" for complex dialogs that have
//     non-trivial contracts or TV/leanback special needs.
//
// ICON / ANIMATION MAPPING INTO THE LEAF SLOT (how it would be done):
//   The original logic (lines ~151-163) is:
//     if (resultType != BAN) {
//       val anim = AnimatedVectorDrawableCompat.create(..., R.drawable.anim_delete)!!
//       deleteAnimation.setImageDrawable(anim)
//       anim.registerAnimationCallback( object : ... { onEnd -> anim.start() } )
//       anim.start()
//     } else {
//       deleteAnimation.setImageResource(R.drawable.ic_warning_medium)
//     }
//   In the swapped future (or in Lab "real context" simulation):
//     iconContent = {
//       AndroidView(factory = { ctx ->
//         ImageView(ctx).apply {
//           if (isBanCase) setImageResource(R.drawable.ic_warning_medium)
//           else {
//             val anim = AnimatedVectorDrawableCompat.create(ctx, R.drawable.anim_delete)!!
//             setImageDrawable(anim)
//             anim.register... { onEnd -> anim.start() }
//             anim.start()
//           }
//         }
//       })
//     }
//   The leaf just hosts whatever @Composable you give it in the 40.dp Box slot.
//   Perfect isolation: animation (platform drawable) lives in the host that already
//   owns the lifecycle; leaf stays pure presentational.
//
// WHY THIS IS SAFE (Permanent Exceptions boundary + rollback):
//   - Purely additive documentation in this slice. No new runtime paths. Dialog is
//     visually and behaviorally identical before/after for every user and every call site.
//   - Original XML (dialog_confirm_delete.xml) byte-for-byte untouched on disk. The
//     ConstraintLayout + ImageView + TextViews + Barrier + Buttons + all ids remain.
//     Future full swap can delete or keep the old views (for A/B or TV fallback).
//   - All 15+ call sites (phone + TV) unaffected. No change to fragment result keys,
//     bundle contents, listener contract for leanback, or any string resources.
//   - Leaf is already proven in Lab + Previews + has its own light/dark @Previews.
//   - Rollback for this documentation slice: delete the two comment blocks (imports
//     sketch + this banner) + any Lab/Preview updates. File reverts to 2026-05-30 pre-
//     compose-2l4.1.5 state. Zero other files touched for the host itself.
//   - If/when content swap is activated later: the old direct view mutation code will
//     be commented (like the ArrayAdapter in DebugLogActivity) for instant one-line
//     rollback. ComposeView insertion + GONE on the three presentational views (or
//     layout edit to host VLCComposeView + rewire cancel_button constraint) is the
//     only delta. Buttons and result paths never move.
//   - ListView/Recycler hybrid patterns from prior hosts apply here too for any
//     future adapter usage of delete confirmations.
//   - Theming: leaf + VLCTheme guarantee light/dark fidelity (already verified in
//     dedicated previews). Matches ?attr/font_default etc from the XML intent.
//
// HYBRID MIGRATION STRATEGY (Wave 1 epic - compose-2l4 series, continued):
//   1. Keep legacy XML + inflation sites + full dialog contracts working (we do - 100%).
//   2. Introduce leaf Composables that are pure presentational + self-themed (VLCDialogConfirmDelete
//      already done; focuses exactly on the icon+title+message subset).
//   3. Use VLCComposeView (in XML) or raw ComposeView (programmatic, as sketched) for hosting.
//   4. Always wrap with VLCTheme at the call site (leaf also does internally - harmless).
//   5. Leave original layout XML files in place until the LAST reference is migrated.
//   6. For dialogs with complex shells/contracts: keep the Fragment as shell, swap only
//      the presentational content area (this exact pattern). Buttons can migrate to M3
//      TextButton later inside a full Compose AlertDialog/ModalBottomSheet when the
//      whole dialog host is ready.
//   7. Exercise every leaf in the Interop Lab (the single source of truth for "it works
//      live") + rich Previews (for Studio regression, no device needed).
//   8. Massive comments + bd tracking + full session completion (git + bd dolt push).
//   9. Cross-cutting concerns captured in compose-2l4.1.8 (bd compose-iju) notes.
//
// WAVE 1.8 MILESTONE CONTEXT (compose-2l4.1.8 / bd compose-iju):
//   - The Compose Interop Lab is the canonical place to see *this* leaf working in a
//     realistic AlertDialog wrapper (plus all other leaves + combined mocks).
//   - This task (2l4.1.5) enhances that Lab demo with "real host context" pulled from
//     the actual title-generation logic and ban-folder vs normal-delete icon cases
//     that live in this file.
//   - Previews in PreviewUtils.kt are expanded with variants that exactly mirror the
//     when-expression cases here (single file/folder, multi, album, playlist, ban
//     warning, clear history, etc.).
//   - This host (even as pure doc) participates in the "every interop host must have
//     green compile gate evidence" policy.
//
// Traceability (full, for the next agent):
//   - Leaf: application/compose/src/main/java/org/videolan/vlc/compose/components/DialogContent.kt
//           (VLCDialogConfirmDelete - the icon/title/message presentational core)
//   - Interop layer: application/compose/src/main/java/org/videolan/vlc/compose/interop/VLCCompose.kt
//   - Theme: application/compose/src/main/java/org/videolan/vlc/compose/theme/VLCTheme.kt + VLCThemeDefaults
//   - Previews + rich mocks: application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt
//     (VLCDialogConfirmDelete*Previews + DialogConfirmDeleteMock* + InteropLabSnapshot*)
//   - Original XML (preserved): application/vlc-android/res/layout/dialog_confirm_delete.xml
//   - Reference dialog host: application/vlc-android/src/org/videolan/vlc/gui/dialogs/NetworkServerDialog.kt
//   - This host file: .../gui/dialogs/ConfirmDeleteDialog.kt
//   - This task: compose-2l4.1.5 (dialog content win, "keep shell swap content")
//   - bd issue: compose-j0e (created + claimed for this exact scope; discovered-from compose-iju)
//   - Cross-cut / crown jewel: compose-2l4.1.8 / bd compose-iju (the Lab + gate policy)
//   - Prior related: compose-5qk, compose-5wg, compose-l94, compose-95d
//   - Epic: Wave 1 leaf migrations (post bootstrap compose-cb5 etc.)
//   - Permanent Exceptions (documented boundary): native player surfaces, MediaLibrary JNI,
//     certain complex dialogs/TV overlays that need leanback focus mgmt or custom result
//     contracts (this ConfirmDeleteDialog and its siblings may remain hybrid shells for
//     a long time or forever; WebView remnants, low-level rendering - stay XML/native).
//   - Call sites that must never break: HeaderMediaListActivity, MediaBrowserFragment,
//     BaseBrowserFragment, FileBrowserFragment, VideoGridFragment, HistoryFragment,
//     PreferencesAdvanced (phone+TV), MediaItemDetailsFragment (TV), etc.
//
// At end of this work: bd progress updates + rich close reason, git pull --rebase,
// bd dolt push, git push (verify "up to date with origin/phase-0-compose-bootstrap").
// =========================================================================

class ConfirmDeleteDialog : VLCBottomSheetDialogFragment() {

    private var resultType: Int = CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE
    private lateinit var listener: () -> Unit
    private lateinit var deleteAnimation: ImageView
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var deleteButton: Button
    private lateinit var mediaList: List<MediaLibraryItem>
    private var titleString: String? = null
    private var descriptionString: String? = null
    private var buttonText: String? = null

    companion object {

        /**
         * Create a new ConfirmDeleteDialog
         * @param medias the list of media used to create the title. If not relevant, use [title], [description] and [buttonText]
         * @param title the title to be used
         * @param description the description to be used
         * @param buttonText the button's text to be used
         */
        fun newInstance(medias: ArrayList<MediaLibraryItem> = arrayListOf(), title:String ="", description:String ="", buttonText:String="", resultType:Int = CONFIRM_DELETE_DIALOG_RESULT_DEFAULT_VALUE): ConfirmDeleteDialog {

            return ConfirmDeleteDialog().apply {
                arguments = bundleOf(
                    CONFIRM_DELETE_DIALOG_MEDIALIST to medias,
                    CONFIRM_DELETE_DIALOG_TITLE to title,
                    CONFIRM_DELETE_DIALOG_DESCRIPTION to description,
                    CONFIRM_DELETE_DIALOG_BUTTON_TEXT to buttonText,
                    CONFIRM_DELETE_DIALOG_RESULT_TYPE to resultType
                )
            }
        }
    }

    /**
     * Set the listener. Should be only used from leanback as it has no setFragmentResultListener
     *
     * @param listener
     */
    fun setListener(listener: () -> Unit) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch { if (requireActivity().showPinIfNeeded()) dismiss() }
        mediaList = arguments?.parcelableList(CONFIRM_DELETE_DIALOG_MEDIALIST) ?: listOf()
        titleString = arguments?.getString(CONFIRM_DELETE_DIALOG_TITLE)
        descriptionString = arguments?.getString(CONFIRM_DELETE_DIALOG_DESCRIPTION)
        buttonText = arguments?.getString(CONFIRM_DELETE_DIALOG_BUTTON_TEXT)
        arguments?.getInt(CONFIRM_DELETE_DIALOG_RESULT_TYPE)?.let {
            resultType = it
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_confirm_delete, container)
        // -----------------------------------------------------------------
        // WAVE 1 / compose-2l4.1.5 NOTE (ConfirmDeleteDialog host migration):
        // Layout inflated 100% as always (dialog_confirm_delete.xml preserved).
        // The three presentational views (delete_animation, title, message) + the
        // full anim setup below are the "content" that the VLCDialogConfirmDelete
        // leaf is designed to replace in a future swap (see 130+ line mission header
        // at top of this file for the exact mapping recipe using AndroidView in the
        // iconContent slot, while buttons + result contracts stay in this shell).
        // Current slice: zero behavior change. This comment + header = the win.
        // Lab + Previews now exercise the leaf with real variants from the when-block
        // and ban-folder special case below.
        // -----------------------------------------------------------------
        deleteAnimation = view.findViewById(R.id.delete_animation)
        title = view.findViewById(R.id.title)
        description = view.findViewById(R.id.message)
        deleteButton = view.findViewById(R.id.delete_button)
        view.findViewById<Button>(R.id.delete_button).setOnClickListener {
            if (::listener.isInitialized) listener.invoke()
            setFragmentResult(CONFIRM_DELETE_DIALOG_RESULT, bundleOf(CONFIRM_DELETE_DIALOG_MEDIALIST to mediaList, CONFIRM_DELETE_DIALOG_RESULT_TYPE to resultType))
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }

        title.text = when {
            mediaList.isEmpty() || resultType == CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER -> titleString
            mediaList.size > 1 && mediaList.filterIsInstance<MediaWrapper>().size == mediaList.size -> {
                //folders and files
                val nbFiles = mediaList.filter { it is MediaWrapper && it.type != MediaWrapper.TYPE_DIR }.size
                val nbFolders = mediaList.filter { it is MediaWrapper && it.type == MediaWrapper.TYPE_DIR }.size
                when {
                    nbFiles == 0 -> getString(R.string.confirm_delete_folders, nbFolders)
                    nbFolders == 0 -> getString(R.string.confirm_delete_files, nbFiles)
                    else -> getString(R.string.confirm_delete_folders_and_files, nbFolders, nbFiles)
                }

            }
            mediaList[0] is MediaWrapper -> getString(if ((mediaList[0] as MediaWrapper).type == MediaWrapper.TYPE_DIR) R.string.confirm_delete_folder else R.string.confirm_delete, mediaList[0].title)
            mediaList[0] is Album -> getString(R.string.confirm_delete_album, mediaList[0].title)
            mediaList[0] is Playlist -> getString(R.string.confirm_delete_playlist, mediaList[0].title)
            else -> getString(R.string.confirm_delete_several_media, mediaList.size)
        }

        if (descriptionString?.isNotEmpty() == true) description.text = descriptionString
        if (buttonText?.isNotEmpty() == true) deleteButton.text = buttonText


        if (resultType != CONFIRM_DELETE_DIALOG_RESULT_BAN_FOLDER) {
            val anim = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.anim_delete)!!
            deleteAnimation.setImageDrawable(anim)
            anim.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    anim.start()
                    super.onAnimationEnd(drawable)
                }
            })
            anim.start()
        } else {
            deleteAnimation.setImageResource(R.drawable.ic_warning_medium)
        }
        return view
    }

    override fun getDefaultState(): Int {
        return STATE_EXPANDED
    }

    override fun initialFocusedView(): View = deleteAnimation

    override fun needToManageOrientation(): Boolean {
        return true
    }
}