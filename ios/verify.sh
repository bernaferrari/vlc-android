#!/bin/bash
#
# Reproducible unsigned iOS verification for local machines and CI.
# The shared Kotlin tests are run by Gradle; this script proves that Xcode can
# resolve the Swift package, build the simulator host, and archive the device
# host against the matching VLCShared framework variant.
#
set -euo pipefail

MODE="${1:-all}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_DIR="$PROJECT_ROOT/ios"
ARCHIVE_PATH="${IOS_ARCHIVE_PATH:-$PROJECT_ROOT/build/VLC-iOS.xcarchive}"
SPM_CACHE="${IOS_SPM_CACHE:-$PROJECT_ROOT/build/ios-spm}"
EXPECTED_BUNDLE_ID="${IOS_EXPECTED_BUNDLE_ID:-org.videolan.vlc-ios}"
EXPECTED_BUILD="$(
  sed -nE 's/^val versionCode = ([0-9_]+)$/\1/p' "$PROJECT_ROOT/build.gradle.kts" | tr -d '_'
)"
EXPECTED_MARKETING_VERSION="$(
  sed -nE 's/^val versionName = \"([0-9]+([.][0-9]+)+).*/\1/p' "$PROJECT_ROOT/build.gradle.kts"
)"

if [ -z "$EXPECTED_BUILD" ] || [ -z "$EXPECTED_MARKETING_VERSION" ]; then
  echo "Could not derive the release identity from build.gradle.kts" >&2
  exit 1
fi

verify_archive_identity() {
  local app_plist="$ARCHIVE_PATH/Products/Applications/VLC-iOS.app/Info.plist"
  if [ ! -f "$app_plist" ]; then
    echo "Archived VLC Info.plist is missing: $app_plist" >&2
    exit 1
  fi

  local actual_bundle_id actual_marketing_version actual_build actual_display_name
  actual_bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_plist")"
  actual_marketing_version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$app_plist")"
  actual_build="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$app_plist")"
  actual_display_name="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' "$app_plist")"

  [ "$actual_bundle_id" = "$EXPECTED_BUNDLE_ID" ] || {
    echo "Unexpected iOS bundle identifier: $actual_bundle_id" >&2
    exit 1
  }
  [ "$actual_marketing_version" = "$EXPECTED_MARKETING_VERSION" ] || {
    echo "iOS marketing version $actual_marketing_version does not match $EXPECTED_MARKETING_VERSION" >&2
    exit 1
  }
  [ "$actual_build" = "$EXPECTED_BUILD" ] || {
    echo "iOS build number $actual_build does not match $EXPECTED_BUILD" >&2
    exit 1
  }
  [ "$actual_display_name" = "VLC" ] || {
    echo "Unexpected iOS display name: $actual_display_name" >&2
    exit 1
  }

  echo "Verified iOS release identity: $actual_bundle_id $actual_marketing_version ($actual_build)"
}

case "$MODE" in
  simulator|archive|all) ;;
  *)
    echo "Usage: $0 [simulator|archive|all]" >&2
    exit 64
    ;;
esac

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen is required. Install it with: brew install xcodegen" >&2
  exit 1
fi

cd "$IOS_DIR"
xcodegen generate

# Resolve once into a deterministic cache, then keep the actual build pinned
# to Package.resolved. This fails loudly when the checked-in resolution is not
# reproducible instead of silently selecting a new VLCKit revision.
xcodebuild \
  -project VLC-iOS.xcodeproj \
  -scheme VLC-iOS \
  -resolvePackageDependencies \
  -clonedSourcePackagesDirPath "$SPM_CACHE"

if [ "$MODE" = "simulator" ] || [ "$MODE" = "all" ]; then
  xcodebuild \
    -project VLC-iOS.xcodeproj \
    -scheme VLC-iOS \
    -sdk iphonesimulator \
    -configuration Debug \
    -destination 'generic/platform=iOS Simulator' \
    -clonedSourcePackagesDirPath "$SPM_CACHE" \
    -disableAutomaticPackageResolution \
    -onlyUsePackageVersionsFromResolvedFile \
    CODE_SIGNING_ALLOWED=NO \
    build
fi

if [ "$MODE" = "archive" ] || [ "$MODE" = "all" ]; then
  xcodebuild \
    -project VLC-iOS.xcodeproj \
    -scheme VLC-iOS \
    -sdk iphoneos \
    -configuration Release \
    -archivePath "$ARCHIVE_PATH" \
    -clonedSourcePackagesDirPath "$SPM_CACHE" \
    -disableAutomaticPackageResolution \
    -onlyUsePackageVersionsFromResolvedFile \
    CODE_SIGNING_ALLOWED=NO \
    archive
  verify_archive_identity
fi
