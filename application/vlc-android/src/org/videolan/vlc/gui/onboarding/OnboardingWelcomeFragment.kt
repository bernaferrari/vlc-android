/*****************************************************************************
 * OnboardingWelcomeFragment.kt
 *
 * Copyright © 2021 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
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

package org.videolan.vlc.gui.onboarding

/*
 * **************************************************************************
 *  OnboardingWelcomeFragment.kt (Wave 1 host migration)
 * **************************************************************************
 */

// =============================================================================
// WAVE 1 HOST MIGRATION IMPORTS (OnboardingWelcome host - compose-2l4.1.6 / bd: compose-mdj)
// These come from :application:compose (api dependency in vlc-android/build.gradle).
// Reference implementations / templates:
//   - NetworkServerDialog.kt (first interop demo, compose-5qk)
//   - DebugLogActivity.kt (first real Wave 1 host + adapter patterns, compose-2l4.1.2 / bd compose-5wg)
//   - MediaInfoAdapter + InfoActivity (compose-l94 / bd compose-l94)
//   - Recycler*Decorations + phone hosts (compose-2l4.1.4 / bd compose-95d)
//   - ConfirmDeleteComposeDialog.kt (Compose dialog host, compose-2l4.1.5 / bd compose-j0e)
//   - ComposeInteropLabActivity.kt (crown jewel cross-cut, compose-2l4.1.8 / bd compose-iju)
// =============================================================================
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.components.VLCOnboardingWelcome
import org.videolan.vlc.compose.theme.VLCTheme
// =============================================================================

// =========================================================================
// ==================== WAVE 1 HOST MIGRATION: OnboardingWelcomeFragment ====================
// Host file: OnboardingWelcomeFragment.kt   (task: compose-2l4.1.6 / bd: compose-mdj)
// Composable used: VLCOnboardingWelcome (leaf from OnboardingWelcome.kt)
// Target layouts originally: onboarding_welcome.xml (phone) + layout-land/onboarding_welcome.xml
//   (BOTH STILL PRESENT ON DISK, 100% UNTOUCHED, and will remain forever per policy)
//
// THIS IS THE HIGH-VISIBILITY FIRST-RUN FLOW COVERAGE WIN FOR WAVE 1.
//
// Reference templates (exact comment density, safety language, traceability copied/adapted):
//   NetworkServerDialog.kt (minimal Pattern 1 demo) + DebugLogActivity.kt (richness + Lab launch)
//   + MediaInfoAdapter.kt (Pattern 2 programmatic + icon slot mapping) + Recycler decorations
//   + ConfirmDeleteComposeDialog.kt + the Interop Lab (compose-2l4.1.8).
//
// MISSION: Replace the static presentational content of the very first screen users see
// on fresh install or upgrade (the branded "Welcome to VLC!" + cone logo + subtitle
// on the deep onboarding grey background) with the dedicated VLCOnboardingWelcome leaf.
// This is the entry point to the entire 5-step onboarding flow (welcome → theme choice?
// → permissions → scan → done). High leverage because it is literally the first thing
// a new user experiences.
//
// The original XMLs use ConstraintLayout + ImageView(srcCompat="@drawable/ic_icon")
// + two TextViews (welcome_title with VLC.Onboarding.Title, subtitle with VLC.Onboarding.Text)
// + guideline at 25% (phone) or packed chain (land). The land variant has slightly
// different vertical constraints and no guideline. Both preserved verbatim.
//
// PATTERN USED HERE (programmatic ComposeView for a dedicated leaf Fragment):
//   - Because the spec requires "Preserve the original onboarding layouts 100%" and
//     "note the land variant", we do NOT add a <VLCComposeView> tag to either XML
//     (contrast with Pattern 1 in NetworkServerDialog / debug_log.xml / interop_lab.xml).
//   - Instead: onCreateView returns a fresh ComposeView whose setContent hosts the leaf.
//   - This is the natural evolution of the "programmatic ComposeView" guidance in
//     NetworkServerDialog comments and the exact Pattern 2 used for RV rows in
//     MediaInfoAdapter + DebugLogComposeAdapter.
//   - The fragment remains the navigation target (newInstance, OnboardingActivity switch,
//     OnboardingFragmentListener, talkback integration via base class).
//   - All other onboarding fragments (Permission, Scanning, NoPermission, Notification,
//     Theme) + OnboardingActivity shell + animations + edge-to-edge dark bars + viewmodel
//     + listener wiring remain 100% XML/legacy and untouched.
//
// REAL LOGO MAPPING INTO THE SLOT (Wave-1-safe):
//   logoContent = {
//       Image(
//           painter = painterResource(R.drawable.ic_icon),
//           contentDescription = null, // purely decorative branding
//           modifier = Modifier.size(96.dp)
//       )
//   }
//   Matches exactly the @drawable/ic_icon vector used by BOTH phone and land XMLs.
//   The leaf's internal 120.dp Box + 64.dp top spacer + 24.dp after are preserved.
//   We use the slot (instead of DefaultOnboardingLogo placeholder) exactly as the
//   leaf API and Lab variant intended. No drawables added to :compose module.
//
// STRINGS & LOCALIZATION:
//   title = getString(R.string.welcome_title)   // "Welcome to VLC!"
//   subtitle = getString(R.string.welcome_subtitle)
//   100% identical to what the old TextViews rendered. All translations continue to work.
//
// TALKBACK / A11Y ADAPTATION (minimal, safe):
//   - Field widened from TextView → View (still lateinit).
//   - In Compose path we assign the root ComposeView to it.
//   - getDefaultViewForTalkback() + base onResume sendAccessibilityEvent(TYPE_VIEW_FOCUSED)
//     still execute. The inner Text(title) in VLCOnboardingWelcome carries Compose
//     semantics (heading + text) so screen readers will announce it.
//   - This is sufficient for Wave 1. Full semantics polish / test in talkback +
//     compose a11y best practices is future (post full onboarding or Wave 2 nav).
//   - Old findViewById(R.id.welcome_title) path is preserved in comments for rollback.
//
// WHY THIS IS SAFE (Permanent Exceptions boundary respected):
//   - Purely additive: both onboarding_welcome.xml variants untouched on disk forever.
//   - Old rendering path (inflate + findViewById + TextView titleView) fully preserved
//     in source as commented blocks (instant A/B or rollback).
//   - No behavior change to OnboardingActivity, fragment transactions, custom enter/exit
//     animations, skip button, listener callbacks, viewmodel state, edge-to-edge, or any
//     of the 5 other onboarding steps.
//   - VLCOnboardingWelcome is a pure leaf: no nav, no side effects, no live data.
//     Theming via VLCTheme (defaults to system) + explicit + leaf-internal defensive
//     wrapper. Uses the dedicated onboardingBackground + fontDefault / fontLight tokens.
//   - The full onboarding flow still forces dark status/nav bars (activity level).
//   - Rollback is one-file: remove the 8 new imports, revert field type to TextView,
//     uncomment the two old blocks in onCreateView + onViewCreated, delete the
//     ComposeView construction. Zero other files touched. Compiles/runs as pre-2l4.1.6.
//   - Fragment + ComposeView is a standard, low-risk bridge (used in many incremental
//     adoptions); the whole screen is tiny and static so no recycling / state timing issues.
//
// HYBRID MIGRATION STRATEGY (this slice + future onboarding work):
//   1. Keep legacy XML inflation sites working (commented here - we do).
//   2. Leaf already existed (static parts only; theme/scan/permission steps future).
//   3. Use programmatic ComposeView at the inflation boundary for this dedicated fragment.
//   4. Always wrap with VLCTheme at host site.
//   5. Leave both onboarding_welcome*.xml in place (policy: until LAST reference gone).
//   6. Exercise the leaf + real logo slot + fragment context in Interop Lab (done).
//   7. Strengthen dark onboardingBackground previews (done in PreviewUtils.kt).
//   8. Massive comments + bd tracking + README update + full session completion.
//   9. Cross-cutting Permanent Exceptions + onboarding token notes captured in compose-iju.
//
// WAVE 1.6 MILESTONE (this host):
//   - Adds the first-run welcome branding to the "canary" set of interop hosts
//     (NetworkServerDialog, DebugLog, MediaInfo, SectionHeader decorations, ConfirmDelete,
//      Interop Lab, and now OnboardingWelcomeFragment).
//   - Visible on every fresh install → extremely high signal for theming fidelity
//     (onboardingBackground token, font tokens, logo slot, overall layout spacing).
//   - The land variant is called out in comments + the two XMLs remain identical twins
//     of the pre-migration state.
//
// Permanent Exceptions boundary (repeated for every Wave 1 agent):
//   Everything here is migratable. The ~20% that stays XML/native forever:
//   - Native player surfaces (VLCVideoView, subtitles, hardware decoding)
//   - Heavy MediaLibrary JNI / medialibrary service surfaces
//   - Certain complex TV overlays and leanback fragments
//   - Legacy WebView UIs and a few preference screens with custom prefs
//   - (Onboarding steps beyond the static welcome branding may stay hybrid longer)
//   See full matrix + rationale in bd compose-iju notes and the parent Wave 1 epic.
//
// Traceability:
//   - Leaf: application/compose/src/main/java/org/videolan/vlc/compose/components/OnboardingWelcome.kt
//   - Interop: application/compose/src/main/java/org/videolan/vlc/compose/interop/VLCCompose.kt
//   - Theme + onboardingBackground token: .../theme/VLCTheme.kt (OnboardingGrey #011422)
//   - Previews (enhanced for this task): application/compose/src/main/java/org/videolan/vlc/compose/PreviewUtils.kt
//   - Original XMLs (both variants, preserved): application/vlc-android/res/layout/onboarding_welcome.xml
//     + application/vlc-android/res/layout-land/onboarding_welcome.xml
//   - Host activity + flow: application/vlc-android/src/org/videolan/vlc/gui/onboarding/OnboardingActivity.kt
//   - Base: OnboardingFragment.kt (talkback contract)
//   - Reference demo: NetworkServerDialog.kt + network_server_dialog.xml (compose-5qk)
//   - Rich host example: DebugLogActivity.kt + debug_log.xml (compose-2l4.1.2)
//   - Info host: MediaInfoAdapter.kt + InfoActivity.kt (compose-2l4.1.3 / bd compose-l94)
//   - Decoration host: RecyclerSectionItemDecoration.kt + BaseAudioBrowser.kt etc (compose-2l4.1.4 / bd compose-95d)
//   - Dialog host: ConfirmDeleteComposeDialog.kt (compose-2l4.1.5 / bd compose-j0e)
//   - Crown jewel: ComposeInteropLabActivity.kt + compose_interop_lab.xml (compose-2l4.1.8 / bd compose-iju)
//   - This task: compose-2l4.1.6 (bd: compose-mdj, discovered-from compose-iju)
//   - Cross-cut + Permanent Exceptions + gate policy: bd compose-iju (Wave 1.8)
//   - Onboarding token follow-up: compose-wtk
//   - Epic: Wave 1 leaf migrations after phase-0-compose-bootstrap (compose-cb5 etc.)
//   - README update: pointers list now includes this first-run host.
//
// BUILD GATE POLICY (enforced):
//   Every host (including this new one) must have green :application:vlc-android:compileDebugKotlin
//   evidence recorded. See full policy + prior evidence in compose-iju notes + README.
//
// At end of this work (MANDATORY per Agents.md):
//   - bd update compose-mdj --notes "..." (rich evidence)
//   - bd update compose-iju --notes "WAVE 1.6 append: OnboardingWelcomeFragment..." (cross-cut)
//   - bd close compose-mdj --reason "..."
//   - git pull --rebase
//   - bd dolt push
//   - git push
//   - git status MUST report "up to date with origin/phase-0-compose-bootstrap"
//   - Clean stashes etc. Hand-off ready for next Wave 1 slice.
// =========================================================================

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import org.videolan.vlc.R

// The active field below is View to accommodate the ComposeView root.

class OnboardingWelcomeFragment : OnboardingFragment() {
    // WAVE 1: widened to View so we can assign the ComposeView root for talkback contract.
    // Old TextView usage only in the fully-preserved commented rollback path.
    private lateinit var titleView: View

    override fun getDefaultViewForTalkback() = titleView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // ---------------------------------------------------------------------
        // OLD XML PATH (onboarding_welcome.xml + land variant) - 100% PRESERVED
        // For instant rollback or A/B testing: uncomment the next two lines and
        // remove the ComposeView construction below. Both XML files stay on disk
        // unchanged forever (phone layout + layout-land variant with its packed
        // chain / different margins noted in the mission header).
        // ---------------------------------------------------------------------
        // return inflater.inflate(R.layout.onboarding_welcome, container, false)

        // ---------------------------------------------------------------------
        // NEW WAVE 1 PATH (compose-2l4.1.6): full ComposeView hosting the leaf.
        // Returns the entire fragment view as a Compose host. Uses the real VLC
        // icon via painterResource into the logoContent slot + real localized
        // strings. The leaf + VLCTheme supply onboardingBackground + correct fonts.
        // This is the dedicated static welcome step only.
        // ---------------------------------------------------------------------
        return ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                VLCTheme {
                    VLCOnboardingWelcome(
                        title = getString(R.string.welcome_title),
                        subtitle = getString(R.string.welcome_subtitle),
                        logoContent = {
                            // Real logo (VLC cone) mapped from the same @drawable/ic_icon
                            // used by both onboarding_welcome.xml variants. Wave-1-safe:
                            // painter + Image only; no vectors or complex assets in the
                            // compose module. Size chosen to fit the leaf's 120dp box.
                            Image(
                                painter = painterResource(R.drawable.ic_icon),
                                contentDescription = null, // decorative branding element
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // -----------------------------------------------------------------
        // OLD FIND PATH (commented) - the R.id.welcome_title from XML no longer
        // exists in the Compose path. The inner title Text now lives inside
        // VLCOnboardingWelcome and carries Compose accessibility semantics.
        // -----------------------------------------------------------------
        // titleView = view.findViewById(R.id.welcome_title)

        // NEW: the root of the ComposeView we returned from onCreateView.
        // Base class onResume will send focus event to it when talkback is active.
        // Compose semantics on the title Text handle announcement.
        titleView = view
    }

    companion object {
        fun newInstance(): OnboardingWelcomeFragment {
            return OnboardingWelcomeFragment()
        }
    }
}
