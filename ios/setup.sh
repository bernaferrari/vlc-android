#!/bin/bash
#
# iOS Xcode project setup script for VLC KMP.
#
# This script creates the Xcode project and configures it to consume
# the VLCShared Kotlin framework built by the :shared Gradle module.
#
# Prerequisites:
#   - Xcode 15+
#   - Kotlin Multiplatform Mobile plugin (or manual framework linking)
#   - Gradle build completed (./gradlew :shared:linkDebugFrameworkIosSimulatorArm64)
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_DIR="$PROJECT_ROOT/ios"
FRAMEWORK_NAME="VLCShared"
BUILD_DIR="$PROJECT_ROOT/shared/build/bin"

echo "=== VLC KMP iOS Project Setup ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# 1. Verify framework exists
SIM_FRAMEWORK="$BUILD_DIR/iosSimulatorArm64/debugFramework/$FRAMEWORK_NAME.framework"
ARM64_FRAMEWORK="$BUILD_DIR/iosArm64/debugFramework/$FRAMEWORK_NAME.framework"

if [ ! -d "$SIM_FRAMEWORK" ]; then
    echo "Building debug framework for iOS Simulator (arm64)..."
    cd "$PROJECT_ROOT"
    ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon
fi

if [ ! -d "$ARM64_FRAMEWORK" ]; then
    echo "Building debug framework for iOS device (arm64)..."
    cd "$PROJECT_ROOT"
    ./gradlew :shared:linkDebugFrameworkIosArm64 --no-daemon
fi

echo ""
echo "Framework locations:"
echo "  Simulator: $SIM_FRAMEWORK"
echo "  Device:    $ARM64_FRAMEWORK"
echo ""

# 2. Create Xcode project using xcodegen if available, or provide instructions
if command -v xcodegen &> /dev/null; then
    echo "Generating Xcode project with xcodegen..."
    cat > "$IOS_DIR/project.yml" << 'EOF'
name: VLC-iOS
options:
  bundleIdPrefix: org.videolan
  deploymentTarget:
    iOS: "16.0"
targets:
  VLC-iOS:
    type: application
    platform: iOS
    sources:
      - path: App
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: org.videolan.vlc-ios
        INFOPLIST_KEY_UILaunchScreen_Generation: YES
        INFOPLIST_KEY_UIApplicationSceneManifest_Generation: YES
        INFOPLIST_KEY_UISupportedInterfaceOrientations: "UIInterfaceOrientationPortrait UIInterfaceOrientationLandscapeLeft UIInterfaceOrientationLandscapeRight"
    dependencies:
      - framework: ../shared/build/bin/iosSimulatorArm64/debugFramework/VLCShared.framework
        embed: true
EOF
    cd "$IOS_DIR"
    xcodegen generate
    echo "Xcode project generated at: $IOS_DIR/VLC-iOS.xcodeproj"
else
    echo "xcodegen not found. Manual setup instructions:"
    echo ""
    echo "1. Open Xcode → File → New → Project"
    echo "2. Choose iOS → App → SwiftUI"
    echo "3. Name it 'VLC-iOS', Bundle ID: org.videolan.vlc-ios"
    echo "4. Save to: $IOS_DIR/"
    echo "5. Replace ContentView.swift with App/AppDelegate.swift"
    echo "6. Add the VLCShared.framework:"
    echo "   - Go to project → Build Phases → Link Binary With Libraries"
    echo "   - Add Other → Add Files → navigate to:"
    echo "     $SIM_FRAMEWORK"
    echo "   - Also add to 'Embed Frameworks' build phase"
    echo "7. For device builds, switch to the arm64 framework"
    echo ""
    echo "Alternatively, install xcodegen: brew install xcodegen"
    echo "Then re-run this script."
fi

echo ""
echo "=== Setup complete ==="
echo "Open the project in Xcode and run on the iOS Simulator."
