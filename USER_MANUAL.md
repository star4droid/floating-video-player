# Floating Video Player - User Manual

## Table of Contents
1. [Introduction](#introduction)
2. [Installation Guide](#installation-guide)
3. [First Setup](#first-setup)
4. [Permissions Explained](#permissions-explained)
5. [Using the App](#using-the-app)
6. [Floating Window Controls](#floating-window-controls)
7. [File Manager](#file-manager)
8. [Troubleshooting](#troubleshooting)
9. [FAQ](#faq)
10. [Privacy & Security](#privacy--security)

## Introduction

Welcome to the Floating Video Player! This innovative Android application allows you to watch videos in a floating window that can display over other applications. Whether you're taking notes while watching tutorials, monitoring social feeds during videos, or multitasking with media, the floating video player gives you the freedom to work and watch simultaneously.

### Key Features
- **Floating Video Player**: Watch videos in a customizable overlay window
- **Cross-App Functionality**: Use over any other application
- **Intuitive Controls**: Easy-to-use floating interface
- **Multiple Formats**: Support for all ExoPlayer-compatible video formats
- **File Manager Integration**: Browse and select videos directly
- **Performance Optimized**: Smooth playback with minimal resource usage

## Installation Guide

### Download and Install
1. **Download the APK**
   - Get the `app-release.apk` file
   - Ensure it's from a trusted source
   - Check the file size (typically 15-25MB)

2. **Enable Unknown Sources**
   - Go to **Settings** > **Security** (or **Apps & notifications**)
   - Find **Install unknown apps** or **Unknown sources**
   - Enable for your file manager or browser

3. **Install the App**
   - Tap the downloaded APK file
   - Follow the installation prompts
   - Grant any requested permissions during installation

4. **Verify Installation**
   - Look for the app icon in your app drawer
   - App name: "Floating Video Player"

### System Requirements
- **Android Version**: 8.0 (API 26) or higher
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 100MB free space
- **Display**: Any size (optimized for phones and tablets)

## First Setup

### Initial Launch
1. **Open the App**
   - Find the app icon in your app drawer
   - Tap to launch the application

2. **Permission Requests**
   - The app will request multiple permissions
   - Read each permission explanation carefully
   - Grant permissions for full functionality

3. **Tutorial Screen**
   - Follow the on-screen tutorial
   - Learn about floating windows
   - Understand permission requirements

### Quick Start Guide
1. **Grant All Permissions**: Essential for app functionality
2. **Start Floating Player**: Tap the main button
3. **Browse Videos**: Use the file manager to find videos
4. **Enjoy Multitasking**: Use other apps while watching

## Permissions Explained

### Why Each Permission is Required

#### 1. System Alert Window (Overlay)
**Purpose**: Allows the app to display windows over other applications

**Why it's needed**: This is the core functionality - without this, the app cannot create floating windows.

**How to grant**:
- Android 6.0-9.0: Settings > Apps > [App Name] > Special access > Display over other apps
- Android 10+: Settings > Apps > [App Name] > Permissions > Display over other apps

#### 2. Storage Permissions
**Purpose**: Access video files stored on your device

**Why it's needed**: To browse and play videos from your device's storage

**Android 13+ Granular Permissions**:
- **READ_MEDIA_VIDEO**: Access video files specifically
- **READ_MEDIA_AUDIO**: Access audio files
- **READ_MEDIA_IMAGES**: Access image files for thumbnails

**Android 11-12 Permission**:
- **MANAGE_EXTERNAL_STORAGE**: Access all files (with user consent)

**Android 6-10 Legacy Permissions**:
- **READ_EXTERNAL_STORAGE**: Access external storage
- **WRITE_EXTERNAL_STORAGE**: For certain file operations

#### 3. Notification Permissions
**Purpose**: Display media controls and notifications

**Why it's needed**: For playback controls and service status notifications (Android 13+ only)

#### 4. Foreground Service
**Purpose**: Run the app continuously without being killed

**Why it's needed**: Keeps the floating window active and responsive

#### 5. Boot Completion
**Purpose**: Restore the floating window after device restart

**Why it's needed**: Automatically restart the service after reboots

### Permission Best Practices
- **Grant all permissions** for full functionality
- **Review permissions** periodically in Settings
- **Revoke permissions** if you experience issues
- **Check permission status** if features stop working

## Using the App

### Main Interface
The main screen provides:
- **Status indicators** for permissions and service
- **Start/Stop floating player** button
- **File manager access** button
- **Settings and troubleshooting** options

### Starting the Floating Player
1. **Check Permissions**
   - Verify all permissions are granted (green checkmarks)
   - The app will prompt for any missing permissions

2. **Start Service**
   - Tap "Start Floating Player"
   - The app creates a background service
   - A notification appears showing the service is active

3. **Open Floating Window**
   - The floating video player window appears
   - Default size: 300x200 pixels
   - Position: Top-left corner of screen

### Stopping the Floating Player
1. **Tap the X button** on the floating window
2. **Or use the notification** to stop the service
3. **Or use the main app** to stop the service

## Floating Window Controls

### Basic Controls

#### Playback Controls
- **Play/Pause Button**: Center button for playback
- **Volume**: Use device volume buttons or system volume
- **Progress**: Currently under development

#### Window Management
- **Drag Window**: Touch and drag from any area
- **Resize Window**: Pinch to zoom (coming soon)
- **Close Window**: Tap the X button
- **Minimize**: Tap the minimize button (if available)

### Window Positioning
- **Auto-position**: Appears in last used position
- **Manual positioning**: Drag to any location
- **Screen boundaries**: Window stays within screen bounds
- **Z-order**: Always on top of other apps

### Window Size
- **Default Size**: 300x200 pixels
- **Flexible Sizing**: Can be resized by user (future feature)
- **Aspect Ratio**: Maintains video aspect ratio when possible

## File Manager

### Opening the File Manager
1. **From Main App**: Tap "Open File Manager"
2. **From Floating Window**: Tap the folder icon
3. **New Window**: Opens as separate floating window

### Browsing Files
1. **File Categories**
   - Videos: Automatically filtered to video files
   - All Files: Browse all file types
   - Recent: Recently accessed files

2. **Navigation**
   - Tap folders to navigate
   - Use back button to go up
   - Tap files to select

3. **File Information**
   - File name and size
   - File type and format
   - Last modified date

### Selecting Videos
1. **Tap to Select**: Highlight the video file
2. **Play Now**: Selected file immediately starts playing
3. **Multiple Selection**: Support for playlists (coming soon)

### Supported Formats
- **MP4**: Most common format, excellent compatibility
- **AVI**: Widely supported
- **MOV**: Apple QuickTime format
- **MKV**: Matroska video container
- **WEBM**: Web-optimized format
- **3GP**: Mobile-optimized format
- **FLV**: Flash video (limited support)

### File Access Tips
- **Grant storage permissions** for full file access
- **All Files Access** (Android 11+) provides complete access
- **Scoped storage** limits access to app-specific folders
- **SD Card**: Some manufacturers limit SD card access

## Troubleshooting

### Common Issues and Solutions

#### Overlay Window Not Appearing
**Symptoms**: App says service is running, but no floating window shows

**Solutions**:
1. **Check overlay permission**
   - Settings > Apps > Floating Video Player > Special access > Display over other apps
   - Ensure it's enabled

2. **Restart the service**
   - Stop the service in the main app
   - Wait 5 seconds
   - Start the service again

3. **Check manufacturer restrictions**
   - Some OEMs restrict overlay apps
   - Look for "Draw over other apps" in phone settings
   - Contact manufacturer for specific instructions

4. **Try different Android version**
   - Test on another device
   - Some Android versions have different restrictions

#### Videos Won't Play
**Symptoms**: File selected but no video plays

**Solutions**:
1. **Check storage permissions**
   - Ensure all storage permissions are granted
   - Android 13+: Check READ_MEDIA_VIDEO permission

2. **Verify file format**
   - Try different video formats
   - Check if file is corrupted
   - Test with known working files

3. **Restart Media3 Player**
   - Stop and restart the floating service
   - Clear app cache if needed

#### App Crashes Frequently
**Symptoms**: App closes unexpectedly or freezes

**Solutions**:
1. **Check available memory**
   - Close other apps
   - Restart your device
   - Ensure at least 1GB free RAM

2. **Clear app data**
   - Settings > Apps > Floating Video Player
   - Storage > Clear Cache
   - Clear Data (note: will reset settings)

3. **Check Android version**
   - Ensure Android 8.0 or higher
   - Update Android if possible

4. **Reinstall the app**
   - Uninstall completely
   - Clear any residual data
   - Reinstall fresh copy

#### File Manager Empty or Limited Access
**Symptoms**: File manager shows no files or limited folders

**Solutions**:
1. **Grant full storage permissions**
   - Android 11+: Enable "Allow access to all files"
   - Android 10-: Enable storage permissions

2. **Check file location**
   - Try different folders
   - Check if files are in protected locations

3. **Restart file manager**
   - Close file manager window
   - Reopen from main app

#### Service Keeps Stopping
**Symptoms**: Notification shows service stopped, floating window disappears

**Solutions**:
1. **Check memory usage**
   - Close unnecessary apps
   - Restart device
   - Consider factory reset if persistent

2. **Battery optimization**
   - Settings > Battery > Battery optimization
   - Find Floating Video Player
   - Select "Don't optimize"

3. **Background app limits**
   - Settings > Apps > Floating Video Player
   - Battery > Background activity
   - Allow background activity

4. **Manufacturer restrictions**
   - Check OEM-specific battery/app management
   - Some phones aggressively kill background services

### Error Messages and Meanings

#### "Overlay Permission Required"
- **Meaning**: System overlay permission not granted
- **Solution**: Enable in Settings > Apps > [App Name] > Special access

#### "Storage Access Denied"
- **Meaning**: Storage permissions not properly granted
- **Solution**: Grant storage permissions in app settings

#### "Service Unavailable"
- **Meaning**: Background service failed to start
- **Solution**: Restart the app and service

#### "Video Format Not Supported"
- **Meaning**: ExoPlayer cannot decode the video file
- **Solution**: Try a different video format or file

#### "Insufficient Memory"
- **Meaning**: Device doesn't have enough RAM available
- **Solution**: Close other apps and restart device

### Performance Tips

#### Optimize for Smooth Playback
1. **Close unnecessary apps** before starting playback
2. **Use lower quality videos** for background watching
3. **Avoid multitasking** with memory-intensive apps
4. **Keep the floating window** reasonably sized
5. **Restart the device** periodically to clear memory

#### Extend Battery Life
1. **Reduce screen brightness** when possible
2. **Use headphones** instead of speaker
3. **Close the app** when not in use
4. **Disable battery optimization** for this app
5. **Use power saving modes** when available

## FAQ

### General Questions

**Q: What Android versions are supported?**
A: Android 8.0 (API 26) and higher. This includes Android 8.0, 8.1, 9, 10, 11, 12, 13, and 14.

**Q: Can I use this app over any other app?**
A: Yes, the overlay functionality works over most other applications, though some OEM-specific apps may have restrictions.

**Q: Does the app collect my data?**
A: No, the app operates entirely offline and doesn't collect or transmit any personal data.

**Q: Can I watch Netflix or other streaming services?**
A: No, the app only plays local video files. Streaming services use DRM protection that prevents overlay playback.

### Technical Questions

**Q: What video formats are supported?**
A: Any format supported by Android's Media3 ExoPlayer, including MP4, AVI, MOV, MKV, WEBM, and more.

**Q: Can I resize the floating window?**
A: Currently, the window has a fixed size, but resizing functionality is planned for future updates.

**Q: Why do I need so many permissions?**
A: Each permission is essential for the app's core functionality:
- Overlay permission for floating windows
- Storage permission for video file access
- Service permissions for background operation

**Q: Can multiple windows be open at once?**
A: Currently, one floating window is supported, but multiple window support is planned.

### Troubleshooting Questions

**Q: The floating window disappeared suddenly**
A: This usually means the service was stopped. Check the notification panel and restart the service if needed.

**Q: Videos play but there's no sound**
A: Check your device's volume settings and ensure the app has audio permissions.

**Q: The app doesn't work on my phone**
A: Check if your device manufacturer has overlay restrictions. Some OEMs like Xiaomi, Huawei, and OnePlus have specific settings.

**Q: Can I use this on tablets?**
A: Yes, the app works on both phones and tablets. The interface adapts to different screen sizes.

## Privacy & Security

### Data Privacy
- **No data collection**: The app doesn't collect, store, or transmit any personal information
- **Offline operation**: All functionality works without internet connectivity
- **Local files only**: Only accesses video files on your device
- **No tracking**: No analytics or user tracking implemented

### Security Features
- **Signed APK**: The release version is properly signed with a private keystore
- **Permission minimalism**: Only requests necessary permissions
- **Local file access**: No external file access or network operations
- **Service isolation**: Background service operates independently

### Your Privacy
- **You control everything**: All file access and permissions are under your control
- **No cloud sync**: Nothing is uploaded to external servers
- **Permission transparency**: Every permission request explains why it's needed
- **Easy revocation**: You can revoke permissions at any time

### Security Best Practices
1. **Download from trusted sources only**
2. **Keep the app updated** to latest version
3. **Review permissions** periodically
4. **Don't modify the APK** or use unofficial versions
5. **Report suspicious behavior** to the developer

---

## Contact and Support

If you encounter issues not covered in this manual:

1. **Check the troubleshooting section** above
2. **Review the error messages** for specific guidance
3. **Try the common solutions** for your issue
4. **Test on a different device** if possible
5. **Provide detailed information** when reporting issues

Remember: The floating video player is designed to enhance your productivity and entertainment by enabling true multitasking on Android devices. With proper setup and permissions, it provides a seamless experience across all your applications.

**Enjoy your new floating video experience!**
