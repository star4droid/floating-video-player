# Floating Video Player for Android

A powerful Android application that enables floating video player functionality with overlay windows that can run over other applications. Built using Android's Media3 ExoPlayer for high-performance video playback and advanced window management.

## 📱 Features

- **Floating Video Player**: Watch videos in a resizable, draggable overlay window
- **File Manager Integration**: Browse and select video files directly from the floating interface
- **Overlay Permissions**: Advanced permission management for Android 11+ all files access
- **Media3 ExoPlayer**: Latest Android media playback framework
- **Cross-App Functionality**: Works over other applications with system-level overlay
- **Multiple Video Formats**: Supports all formats compatible with ExoPlayer
- **Intuitive Controls**: Easy-to-use floating controls for playback management

## 🏗️ Project Structure

```
android_project/
├── app/
│   ├── src/main/
│   │   ├── java/com/floatingvideoplayer/
│   │   │   ├── FloatingVideoPlayerApp.java          # Application class
│   │   │   ├── ui/
│   │   │   │   └── MainActivity.java                # Main UI and permission handling
│   │   │   ├── services/
│   │   │   │   ├── OverlayService.java              # Core overlay window management
│   │   │   │   ├── BootReceiver.java                # Handles device boot events
│   │   │   │   └── NotificationActionReceiver.java  # Manages notification actions
│   │   │   ├── utils/
│   │   │   │   ├── PermissionManager.java           # Permission management utilities
│   │   │   │   ├── StoragePermissionHelper.java     # Storage permission handling
│   │   │   │   └── WindowManagerHelper.java         # Window management utilities
│   │   │   └── models/                              # Data models (for future use)
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml                # Main activity layout
│   │   │   │   ├── floating_player_layout.xml       # Video player overlay layout
│   │   │   │   └── floating_filemanager_layout.xml  # File manager overlay layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml                      # All string resources
│   │   │   │   ├── colors.xml                       # Color definitions
│   │   │   │   └── styles.xml                       # App theme and styles
│   │   │   └── drawable/                            # Icons and graphics
│   │   ├── AndroidManifest.xml                      # App configuration and permissions
│   │   └── build.gradle                             # App-level build configuration
├── build.gradle                                     # Root build configuration
├── settings.gradle                                  # Project settings
└── README.md                                        # This file
```

## 🛠️ Technical Architecture

### Core Components

1. **MainActivity**
   - Permission request handling
   - Service management
   - User interface
   - Permission status monitoring

2. **OverlayService**
   - Foreground service for overlay windows
   - Window creation and management
   - Media3 ExoPlayer integration
   - Notification management

3. **Window Management System**
   - Dynamic overlay window creation
   - Drag and drop functionality
   - Window positioning and sizing
   - Multi-window support

### Media3 ExoPlayer Integration

The app uses Android's Media3 ExoPlayer framework for video playback:

- **High Performance**: Optimized for smooth video playback
- **Format Support**: Wide range of video formats and codecs
- **Streaming Support**: Local files and network streams
- **Customization**: Full control over playback behavior

### Permission Architecture

#### Android 13+ (API 33+)
- `READ_MEDIA_VIDEO` for video file access
- `READ_MEDIA_AUDIO` for audio file access
- `READ_MEDIA_IMAGES` for image file access
- Overlay permission via Settings

#### Android 11-12 (API 30-31)
- `MANAGE_EXTERNAL_STORAGE` for full file system access
- Overlay permission via Settings
- Legacy storage permissions

#### Android 6-10 (API 23-29)
- `READ_EXTERNAL_STORAGE` runtime permission
- `WRITE_EXTERNAL_STORAGE` if needed
- Overlay permission via Settings

## 🚀 Setup Instructions

### Prerequisites

1. **Android Studio**: Latest version (Giraffe or newer)
2. **Android SDK**: API level 26+ (Android 8.0)
3. **Target SDK**: API level 34 (Android 14)
4. **Gradle**: 8.0 or newer
5. **Java/Kotlin**: Java 8+ compatible

### Installation Steps

1. **Clone/Copy Project**
   ```bash
   # Copy the android_project folder to your development workspace
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the `android_project` folder and select it

3. **Sync Project**
   - Android Studio will automatically prompt to sync the project
   - If not, click "Sync Now" in the notification bar

4. **Build Configuration**
   ```gradle
   // In app/build.gradle, verify these settings:
   compileSdk 34
   minSdk 26
   targetSdk 34
   ```

5. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button or use Shift+F10

## 🔧 Configuration

### Permissions Setup

The app requires multiple permissions that must be granted by the user:

#### 1. Overlay Permission
- **Purpose**: Display windows over other apps
- **How to grant**: Settings → Apps → [App Name] → Special access → Display over other apps
- **API Level**: 23+ (Android 6.0)

#### 2. Storage Permissions
- **Android 13+**: Individual media permissions
- **Android 11-12**: Manage all files permission
- **Android 6-10**: External storage permission
- **Purpose**: Access video files on device

#### 3. Additional Permissions
- `RECEIVE_BOOT_COMPLETED`: Auto-start service after reboot
- `FOREGROUND_SERVICE`: Run overlay as foreground service
- `POST_NOTIFICATIONS`: Show notification controls

### Build Variants

```gradle
// debug - Development build with logging enabled
// release - Production build with optimizations
```

### ProGuard Configuration

The project includes ProGuard rules for:
- Media3 ExoPlayer classes
- Overlay service functionality
- Permission management classes

## 🎯 Usage Guide

### First Launch

1. **Install and Launch**
   - Install the app on your device
   - Open the app from the app drawer

2. **Grant Permissions**
   - Follow the permission prompts
   - Grant all requested permissions for full functionality

3. **Start Overlay**
   - Tap "Start Floating Player"
   - Grant overlay permission when prompted
   - The floating window will appear

### Using the Floating Player

1. **File Manager**
   - Click the file manager button
   - Browse your device's video files
   - Select a video to play

2. **Video Controls**
   - Play/Pause: Tap the play button
   - Volume: Adjust system volume or app volume
   - Close: Tap the X button
   - Drag: Move the window around

3. **Multiple Windows**
   - Open both file manager and video player
   - Position them independently
   - Switch between apps seamlessly

## 🔧 Development

### Adding New Features

1. **Video Formats**
   - ExoPlayer supports most common formats
   - Add codec support in build.gradle
   - Test with various file types

2. **Custom Controls**
   - Modify `floating_player_layout.xml`
   - Add new buttons in OverlayService
   - Implement control logic

3. **File Browser Enhancement**
   - Extend `floating_filemanager_layout.xml`
   - Add filtering options
   - Support cloud storage (future)

### Debugging

1. **Logcat**
   ```bash
   # Filter for app logs
   tag:FloatingVideoPlayer
   ```

2. **Common Issues**
   - Overlay not showing: Check overlay permission
   - Files not loading: Check storage permissions
   - Crashes: Check logcat for exceptions

### Testing

1. **Unit Tests**
   ```bash
   ./gradlew test
   ```

2. **Instrumentation Tests**
   ```bash
   ./gradlew connectedAndroidTest
   ```

## 🔒 Security & Privacy

### Data Handling
- No personal data collection
- Video files remain on device
- No internet connectivity required for basic functionality

### Permissions
- All permissions are essential for core functionality
- No unnecessary permissions requested
- User control over all permission grants

### Security Considerations
- Overlay functionality requires system-level access
- Files are accessed locally only
- No external data transmission

## 📋 Requirements

### System Requirements
- **Android Version**: 8.0 (API 26) or higher
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 50MB for app, additional space for video files
- **Display**: Any size, optimized for phones and tablets

### Dependencies
- AndroidX Core KTX 1.12.0
- Material Components 1.10.0
- Media3 ExoPlayer 1.1.1
- AndroidX Lifecycle Components 2.7.0

## 🐛 Troubleshooting

### Common Issues

1. **Overlay Not Working**
   - Check overlay permission in Settings
   - Restart the app
   - Verify Android version (8.0+)

2. **Videos Won't Play**
   - Check video file format compatibility
   - Ensure storage permissions are granted
   - Try different video files

3. **App Crashes on Start**
   - Check Android version compatibility
   - Verify all permissions are granted
   - Clear app data and restart

4. **Service Keeps Stopping**
   - Ensure device has sufficient memory
   - Check if overlay permission is revoked
   - Restart the service

### Error Messages

- "Overlay permission required": Grant overlay permission in Settings
- "Storage access denied": Grant storage permissions
- "Service unavailable": Restart the app and service

## 📄 License

This project is provided as-is for educational and development purposes. Please ensure compliance with all applicable laws and app store policies when distributing.

## 🤝 Contributing

This is a complete Android project structure for a floating video player app. To contribute:

1. Fork the project structure
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For issues and questions:
1. Check the troubleshooting section
2. Review logcat output for error details
3. Verify all permissions are properly granted
4. Test on different Android versions

## 🔄 Updates

### Version 1.0 Features
- Basic overlay functionality
- Media3 ExoPlayer integration
- File manager interface
- Permission management system

### Future Enhancements
- Cloud storage integration
- Advanced video controls
- Playlist support
- Custom themes
- Multi-language support

---

**Note**: This project requires the OVERLAY permission which may not be available on all Android devices or may be restricted by device manufacturers. Always test on a variety of devices and Android versions.# floating-video-player
