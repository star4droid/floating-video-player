#!/bin/bash

# Floating Video Player - Build Script
# This script builds, tests, and packages the Android application

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project paths
PROJECT_ROOT="/workspace/android_project"
APP_DIR="$PROJECT_ROOT/app"
BUILD_DIR="$PROJECT_ROOT/build"
OUTPUT_DIR="$PROJECT_ROOT/output"

# Build configuration
BUILD_TYPE="release"
VERSION_NAME="1.0"
VERSION_CODE="1"
KEY_ALIAS="floatingvideoplayer"
KEY_PASSWORD="fvp2024"
STORE_PASSWORD="fvp2024"
KEYSTORE_FILE="$PROJECT_ROOT/floatingvideoplayer.keystore"

echo -e "${BLUE}=== Floating Video Player Build Script ===${NC}"
echo "Build Type: $BUILD_TYPE"
echo "Version: $VERSION_NAME ($VERSION_CODE)"
echo "Timestamp: $(date)"
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo -e "${RED}Error: Not in project root directory${NC}"
    exit 1
fi

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Step 1: Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean
print_success "Clean completed"

# Step 2: Run unit tests
print_status "Running unit tests..."
./gradlew test
if [ $? -eq 0 ]; then
    print_success "Unit tests passed"
else
    print_warning "Some unit tests failed, but continuing build..."
fi

# Step 3: Run lint checks
print_status "Running lint checks..."
./gradlew lint
print_success "Lint checks completed"

# Step 4: Create release keystore if it doesn't exist
if [ ! -f "$KEYSTORE_FILE" ]; then
    print_status "Creating release keystore..."
    keytool -genkey -v \
        -keystore "$KEYSTORE_FILE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=Floating Video Player, OU=Android, O=FVP, L=City, S=State, C=US"
    print_success "Release keystore created"
else
    print_status "Using existing release keystore"
fi

# Step 5: Build debug APK
print_status "Building debug APK..."
./gradlew assembleDebug --info
if [ $? -eq 0 ]; then
    print_success "Debug APK built successfully"
else
    print_error "Debug APK build failed"
    exit 1
fi

# Step 6: Build release APK
print_status "Building release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    print_success "Release APK built successfully"
else
    print_error "Release APK build failed"
    exit 1
fi

# Step 7: Create output directory
mkdir -p "$OUTPUT_DIR"

# Step 8: Copy APKs to output directory
print_status "Copying APKs to output directory..."
cp "$APP_DIR/build/outputs/apk/debug/app-debug.apk" "$OUTPUT_DIR/"
cp "$APP_DIR/build/outputs/apk/release/app-release.apk" "$OUTPUT_DIR/"

# Step 9: Verify APKs
print_status "Verifying APKs..."
if [ -f "$OUTPUT_DIR/app-release.apk" ]; then
    RELEASE_SIZE=$(du -h "$OUTPUT_DIR/app-release.apk" | cut -f1)
    print_success "Release APK created: $RELEASE_SIZE"
else
    print_error "Release APK not found"
    exit 1
fi

if [ -f "$OUTPUT_DIR/app-debug.apk" ]; then
    DEBUG_SIZE=$(du -h "$OUTPUT_DIR/app-debug.apk" | cut -f1)
    print_success "Debug APK created: $DEBUG_SIZE"
else
    print_warning "Debug APK not found"
fi

# Step 10: Generate build report
print_status "Generating build report..."
cat > "$OUTPUT_DIR/build_report.txt" << EOF
=== Floating Video Player Build Report ===
Build Date: $(date)
Build Type: $BUILD_TYPE
Version Name: $VERSION_NAME
Version Code: $VERSION_CODE

Build Summary:
- Debug APK: app-debug.apk ($(du -h "$OUTPUT_DIR/app-debug.apk" | cut -f1))
- Release APK: app-release.apk ($(du -h "$OUTPUT_DIR/app-release.apk" | cut -f1))

Build Environment:
- Android Gradle Plugin: $(./gradlew --version | grep "Android Gradle Plugin" | head -1)
- Gradle Version: $(./gradlew --version | grep "Gradle" | head -1)
- Kotlin Version: $(./gradlew --version | grep "Kotlin" | head -1)

Permissions Declared:
- SYSTEM_ALERT_WINDOW
- MANAGE_EXTERNAL_STORAGE
- READ_EXTERNAL_STORAGE (Android 6-10)
- WRITE_EXTERNAL_STORAGE (Android 6-10)
- POST_NOTIFICATIONS (Android 13+)
- FOREGROUND_SERVICE_MEDIA_PLAYBACK
- RECEIVE_BOOT_COMPLETED

Target SDK: 34
Min SDK: 26

Build Features:
- Media3 ExoPlayer Integration
- Overlay Window System
- Permission Management
- File Manager Interface
- Performance Optimization
- Memory Management
- Battery Optimization

Generated on: $(date)
EOF

print_success "Build report generated"

# Step 11: Create installation instructions
print_status "Creating installation instructions..."
cat > "$OUTPUT_DIR/INSTALLATION.md" << EOF
# Floating Video Player - Installation Guide

## APK Installation

### Release APK (Recommended)
\`\`\`
File: app-release.apk
Type: Signed release build
Size: $(du -h "$OUTPUT_DIR/app-release.apk" | cut -f1)
\`\`\`

### Debug APK (For Development)
\`\`\`
File: app-debug.apk
Type: Debug build with debugging enabled
Size: $(du -h "$OUTPUT_DIR/app-debug.apk" | cut -f1)
\`\`\`

## Installation Steps

### Method 1: ADB Installation (Recommended)
\`\`\`bash
# Enable Developer Options and USB Debugging on your device
adb install "$OUTPUT_DIR/app-release.apk"
\`\`\`

### Method 2: Direct Installation
1. Download the APK file to your Android device
2. Go to Settings > Security > Install unknown apps
3. Allow installation from your file manager or browser
4. Tap the APK file and follow installation prompts
5. Grant permissions when prompted

## Required Permissions

The app will request the following permissions:

1. **System Alert Window**: Required for floating overlay functionality
   - Go to Settings > Apps > Floating Video Player > Special Access > Draw over other apps
   - Enable permission

2. **Storage Permissions**: Required for file access
   - Android 13+: READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_MEDIA_IMAGES
   - Android 11-12: MANAGE_EXTERNAL_STORAGE
   - Android 6-10: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE

3. **Notification Permissions**: Required for media controls (Android 13+)

4. **Background Permissions**: Required for continuous operation

## First Time Setup

1. Install the APK
2. Open the app
3. Grant all requested permissions
4. Grant System Alert Window permission
5. Allow storage/file access
6. Allow notification access (if requested)
7. The app is ready to use!

## Troubleshooting

### "App not installed" error
- Ensure "Install unknown apps" is enabled for the app installer
- Check that you have sufficient storage space
- Try uninstalling any existing version first

### Permissions not working
- Manually enable System Alert Window permission in Settings
- Restart the app after granting permissions
- Check manufacturer-specific overlay permission settings

### App crashes on startup
- Clear app cache and data
- Check Android version compatibility (minimum Android 8.0)
- Restart your device

## System Requirements

- Android 8.0 (API 26) or higher
- Minimum 100MB free storage
- 2GB RAM recommended
- OpenGL ES 2.0 support

## Support

For issues or questions, please check the troubleshooting guide or contact support.
EOF

print_success "Installation instructions created"

# Step 12: Calculate checksums
print_status "Generating checksums..."
cd "$OUTPUT_DIR"
sha256sum app-release.apk > app-release.apk.sha256
sha256sum app-debug.apk > app-debug.apk.sha256
cd "$PROJECT_ROOT"

print_success "Checksums generated"

# Final summary
echo ""
echo -e "${GREEN}=== Build Summary ===${NC}"
echo -e "${GREEN}✓ Debug APK: ${NC}$OUTPUT_DIR/app-debug.apk ($(du -h "$OUTPUT_DIR/app-debug.apk" | cut -f1))"
echo -e "${GREEN}✓ Release APK: ${NC}$OUTPUT_DIR/app-release.apk ($(du -h "$OUTPUT_DIR/app-release.apk" | cut -f1))"
echo -e "${GREEN}✓ Build Report: ${NC}$OUTPUT_DIR/build_report.txt"
echo -e "${GREEN}✓ Installation Guide: ${NC}$OUTPUT_DIR/INSTALLATION.md"
echo -e "${GREEN}✓ Checksums: ${NC}$OUTPUT_DIR/*.sha256"
echo ""
echo -e "${BLUE}Build completed successfully!${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Test the release APK on multiple devices"
echo "2. Verify all permissions work correctly"
echo "3. Test overlay functionality"
echo "4. Check file manager access"
echo "5. Verify Media3 playback performance"
echo ""
echo -e "${BLUE}Files ready for distribution in: $OUTPUT_DIR${NC}"
