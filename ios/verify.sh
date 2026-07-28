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
fi
