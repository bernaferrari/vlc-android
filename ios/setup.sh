#!/bin/bash
#
# iOS Xcode project setup for VLC KMP + MobileVLCKit (SPM).
#
# Prerequisites:
#   - Xcode 15+
#   - xcodegen (brew install xcodegen)
#   - network access to resolve MobileVLCKit-SPM on first open
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_DIR="$PROJECT_ROOT/ios"
FRAMEWORK_NAME="VLCShared"
BUILD_DIR="$PROJECT_ROOT/shared/build/bin"

echo "=== VLC KMP iOS Project Setup ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# 1. Build frameworks
cd "$PROJECT_ROOT"
echo "Building VLCShared frameworks..."
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 --no-daemon

SIM_FRAMEWORK="$BUILD_DIR/iosSimulatorArm64/debugFramework/$FRAMEWORK_NAME.framework"
ARM64_FRAMEWORK="$BUILD_DIR/iosArm64/debugFramework/$FRAMEWORK_NAME.framework"

echo ""
echo "Framework locations:"
echo "  Simulator: $SIM_FRAMEWORK"
echo "  Device:    $ARM64_FRAMEWORK"
echo ""

# 2. Generate Xcode project
if ! command -v xcodegen &> /dev/null; then
  echo "xcodegen not found. Install with: brew install xcodegen"
  exit 1
fi

cd "$IOS_DIR"
xcodegen generate
echo "Xcode project generated at: $IOS_DIR/VLC-iOS.xcodeproj"
echo ""
echo "MobileVLCKit is declared via SPM in project.yml."
echo "First Xcode open will resolve the package (needs network)."
echo ""
echo "Run:"
echo "  open $IOS_DIR/VLC-iOS.xcodeproj"
echo "  # Product → Run on Simulator"
echo ""
echo "Decode path:"
echo "  AppDelegate sets IosPlaybackService.shared.setBackend(VlcKitBackend.shared)"
echo "  MediaImporter provides Files + Photos intake"
echo "=== Setup complete ==="
