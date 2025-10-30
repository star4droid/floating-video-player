# Floating Video Player - Developer Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Code Structure](#code-structure)
3. [Core Components](#core-components)
4. [Media3 Integration](#media3-integration)
5. [Permission System](#permission-system)
6. [Window Management](#window-management)
7. [Performance Optimization](#performance-optimization)
8. [Testing Framework](#testing-framework)
9. [Build System](#build-system)
10. [Deployment Guide](#deployment-guide)
11. [Future Enhancements](#future-enhancements)
12. [Contributing Guidelines](#contributing-guidelines)

## Architecture Overview

### Design Patterns

The Floating Video Player follows Android best practices and uses several key design patterns:

1. **Service-Oriented Architecture**
   - `OverlayService` handles all overlay functionality
   - Foreground service ensures reliability
   - Separates UI from business logic

2. **Model-View-Controller (MVC)**
   - UI components in `ui/` package
   - Data models in `models/` package
   - Controllers integrated in services

3. **Permission Management Pattern**
   - Centralized permission handling
   - Version-specific permission logic
   - User-friendly permission flows

4. **Factory Pattern**
   - Window creation and management
   - Media3 player initialization
   - Resource allocation

### System Requirements

#### Minimum Requirements
- **Android API**: 26 (Android 8.0)
- **Target API**: 34 (Android 14)
- **Java Version**: 8+
- **Kotlin**: Optional (project uses Java)
- **RAM**: 2GB minimum
- **Storage**: 100MB for app + video files

#### Recommended for Development
- **Android Studio**: Latest stable version
- **SDK Build Tools**: 34.0.0+
- **Gradle**: 8.0+
- **Android Emulator**: API 30+ for testing
- **Physical Device**: For overlay testing

## Code Structure

### Package Organization

```
com.floatingvideoplayer/
├── FloatingVideoPlayerApp.java          # Application class
├── ui/                                   # User interface components
│   ├── MainActivity.java                 # Main activity and entry point
│   ├── VideoPlayerWindow.java            # Video player overlay
│   ├── FileBrowserWindow.java            # File manager overlay
│   ├── DraggableVideoPlayerWindow.java   # Drag and drop functionality
│   ├── DraggableFileManagerWindow.java   # Drag and drop for file manager
│   ├── WindowControls.java               # Window control UI
│   ├── MediaFileAdapter.java             # RecyclerView adapter
│   └── MediaFileGridAdapter.java         # Grid layout adapter
├── services/                             # Background services
│   ├── OverlayService.java               # Core overlay management
│   ├── Media3ExoPlayer.java              # Media player service
│   ├── FileManagerService.java           # File operations
│   ├── AudioPlaybackService.java         # Audio playback handling
│   ├── PlaylistManager.java              # Playlist functionality
│   ├── MediaMetadataExtractor.java       # Video metadata extraction
│   ├── FileManagerPlayerIntegration.java # Integration layer
│   ├── NotificationActionReceiver.java   # Notification handling
│   └── BootReceiver.java                 # Boot completion handling
├── utils/                                # Utility classes
│   ├── PermissionManager.java            # Permission management
│   ├── StoragePermissionHelper.java      # Storage permission handling
│   ├── WindowManagerHelper.java          # Window management
│   ├── FileAccessUtils.java              # File access utilities
│   ├── Media3ErrorHandler.java           # Media3 error handling
│   ├── PerformanceOptimizer.java         # Performance optimization
│   ├── AdvancedWindowManager.java        # Advanced window features
│   ├── MultiWindowManager.java           # Multi-window support
│   ├── WindowControlsManager.java        # Window controls
│   ├── GestureController.java            # Gesture handling
│   ├── WindowAnimationManager.java       # Window animations
│   ├── WindowStateManager.java           # State management
│   ├── SettingsManager.java              # App settings
│   ├── WindowManagementTestSuite.java    # Testing utilities
│   └── WindowManagementIntegrationExample.java # Integration examples
└── models/                               # Data models
    └── MediaFile.java                    # Media file representation
```

### Key Dependencies

#### Android Framework
```gradle
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
androidx.activity:activity-ktx:1.8.2
androidx.fragment:fragment-ktx:1.6.2
```

#### Media Components
```gradle
androidx.media3:media3-exoplayer:1.2.1
androidx.media3:media3-exoplayer-ui:1.2.1
androidx.media3:media3-session:1.2.1
```

#### UI Components
```gradle
com.google.android.material:material:1.10.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.recyclerview:recyclerview:1.3.2
androidx.cardview:cardview:1.0.0
```

## Core Components

### Application Class (FloatingVideoPlayerApp)

The Application class initializes global components and maintains app state:

```java
public class FloatingVideoPlayerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channels
        createNotificationChannels();
        
        // Initialize Media3 components
        initializeMedia3();
        
        // Set up global error handling
        setupGlobalExceptionHandler();
    }
}
```

**Key Responsibilities:**
- Global app initialization
- Notification channel setup
- Media3 library initialization
- Global error handling
- Service lifecycle management

### Main Activity (MainActivity)

Entry point for the application with permission handling:

```java
public class MainActivity extends AppCompatActivity {
    private PermissionManager permissionManager;
    private PerformanceOptimizer performanceOptimizer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize components
        permissionManager = new PermissionManager(this);
        performanceOptimizer = new PerformanceOptimizer(this);
        
        // Check and request permissions
        checkAndRequestPermissions();
    }
}
```

**Key Features:**
- Permission request flow
- Service management UI
- Status monitoring
- Error handling

### Overlay Service (OverlayService)

Core service managing floating windows:

```java
public class OverlayService extends Service {
    private WindowManager windowManager;
    private Media3ExoPlayer mediaPlayer;
    private PerformanceOptimizer performanceOptimizer;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Initialize window manager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        return START_STICKY;
    }
}
```

**Responsibilities:**
- Foreground service management
- Window creation and lifecycle
- Media3 player integration
- Notification management
- Performance optimization

## Media3 Integration

### ExoPlayer Setup

Media3 ExoPlayer is the core media playback engine:

```java
public class Media3ExoPlayer {
    private ExoPlayer exoPlayer;
    private MediaItem mediaItem;
    
    public void initializePlayer(Context context) {
        exoPlayer = new ExoPlayer.Builder(context).build();
        
        // Configure player
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        exoPlayer.setShuffleModeEnabled(false);
        exoPlayer.setVolume(1.0f);
    }
    
    public void playVideo(Uri videoUri) {
        mediaItem = new MediaItem.Builder()
            .setUri(videoUri)
            .setMediaId("current_video")
            .build();
            
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }
}
```

### Media3 Configuration

Key Media3 features implemented:

1. **Format Support**
   - MP4, AVI, MOV, MKV, WEBM
   - Hardware acceleration when available
   - Software fallback for unsupported codecs

2. **Playback Controls**
   - Play/pause functionality
   - Volume control
   - Seek capabilities (future)
   - Repeat modes (future)

3. **Error Handling**
   - Format compatibility errors
   - Network errors (for streaming)
   - Device compatibility issues

4. **Performance Optimization**
   - Buffering optimization
   - Memory management
   - Battery efficiency

### Media Session Integration

For media control integration:

```java
public class MediaSessionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        switch (action) {
            case "android.media.action.MEDIA_BUTTON":
                handleMediaButton(intent);
                break;
            case "com.floatingvideoplayer.ACTION_PLAY_PAUSE":
                togglePlayPause();
                break;
        }
    }
}
```

## Permission System

### Permission Architecture

The app implements a comprehensive permission system supporting Android 6 through 14:

#### Android 6-10 (Runtime Permissions)
```java
// Check runtime permission
if (ContextCompat.checkSelfPermission(context, 
    Manifest.permission.READ_EXTERNAL_STORAGE) 
    != PackageManager.PERMISSION_GRANTED) {
    
    // Request permission
    ActivityCompat.requestPermissions(activity,
        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
        REQUEST_STORAGE_PERMISSION);
}
```

#### Android 11-12 (All Files Access)
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    // Request MANAGE_EXTERNAL_STORAGE
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
    intent.setData(Uri.parse("package:" + context.getPackageName()));
    startActivity(intent);
}
```

#### Android 13+ (Granular Media Permissions)
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    String[] permissions = {
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_IMAGES
    };
    
    ActivityCompat.requestPermissions(activity, permissions, REQUEST_MEDIA_PERMISSIONS);
}
```

### Permission Manager Class

Centralized permission handling:

```java
public class PermissionManager {
    public boolean checkSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    public void requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW);
        }
    }
}
```

### Storage Permission Helper

Handles storage permission variations:

```java
public class StoragePermissionHelper {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    
    public void requestStoragePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ granular permissions
            String[] permissions = {
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            };
            ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 all files access
            requestAllFilesAccess(activity);
        } else {
            // Android 10 and below legacy storage
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST_CODE);
        }
    }
}
```

## Window Management

### Window Manager System

Advanced window management supporting multiple Android versions:

```java
public class WindowManagerHelper {
    public WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }
    
    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }
}
```

### Advanced Window Manager

Enhanced window management features:

```java
public class AdvancedWindowManager {
    private MultiWindowManager multiWindowManager;
    private WindowAnimationManager animationManager;
    private WindowStateManager stateManager;
    
    public void createDraggableWindow(View view, WindowManager.LayoutParams params) {
        // Set up drag listeners
        setUpDragListeners(view);
        
        // Configure animations
        animationManager.addEntryAnimation(view);
        
        // Save window state
        stateManager.saveWindowState(view, params);
    }
    
    private void setUpDragListeners(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private int initialX, initialY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        initialX = params.x;
                        initialY = params.y;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        float deltaY = event.getRawY() - startY;
                        
                        params.x = initialX + (int) deltaX;
                        params.y = initialY + (int) deltaY;
                        
                        windowManager.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });
    }
}
```

### Multi-Window Support

Supporting Android's multi-window and split-screen modes:

```java
public class MultiWindowManager {
    public boolean isInMultiWindowMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ActivityManager activityManager = 
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            return activityManager.isInMultiWindowMode();
        }
        return false;
    }
    
    public void adjustForMultiWindow(WindowManager.LayoutParams params) {
        if (isInMultiWindowMode()) {
            // Adjust window size for multi-window
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }
    }
}
```

## Performance Optimization

### Performance Optimizer Class

Comprehensive performance management:

```java
public class PerformanceOptimizer {
    private LruCache<String, Object> memoryCache;
    private LruCache<String, byte[]> thumbnailCache;
    private ExecutorService backgroundExecutor;
    
    public void initializePerformanceOptimization() {
        // Set up memory management
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        int cacheSize = (int) Math.min(memInfo.totalMem / 1024 / 1024 / 8, 100);
        memoryCache = new LruCache<>(cacheSize);
        thumbnailCache = new LruCache<>(cacheSize / 2);
        
        // Initialize background processing
        backgroundExecutor = Executors.newFixedThreadPool(4);
        
        // Start performance monitoring
        startPerformanceMonitoring();
    }
    
    private void startPerformanceMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(this::performMemoryCleanup, 
                                        5, 5, TimeUnit.SECONDS);
    }
    
    private void performMemoryCleanup() {
        // Monitor memory usage
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        if (memInfo.availMem < LOW_MEMORY_THRESHOLD) {
            // Clear caches
            memoryCache.evictAll();
            thumbnailCache.evictAll();
            
            // Force garbage collection
            System.gc();
        }
    }
}
```

### Memory Management

Best practices for memory usage:

1. **Lazy Loading**: Load resources only when needed
2. **Cache Management**: Use LruCache for frequently accessed data
3. **Resource Cleanup**: Properly release resources when not needed
4. **Background Processing**: Move heavy operations to background threads
5. **Memory Monitoring**: Continuously monitor memory usage

### Battery Optimization

Battery-friendly features:

```java
public class BatteryOptimizationManager {
    public void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    public boolean isBatteryOptimized() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return false;
    }
}
```

## Testing Framework

### Unit Tests

Located in `src/test/java/`:

#### Permission Manager Test
```java
@RunWith(AndroidJUnit4.class)
public class PermissionManagerTest {
    @Test
    public void testSystemAlertWindowPermission() {
        boolean hasPermission = permissionManager.checkSystemAlertWindowPermission();
        assertNotNull("Permission check should not return null", hasPermission);
    }
}
```

#### Window Manager Test
```java
@RunWith(AndroidJUnit4.class)
public class WindowManagerTest {
    @Test
    public void testWindowTypeConfiguration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertTrue("Should support TYPE_APPLICATION_OVERLAY", 
                windowManagerHelper.isWindowTypeSupported(
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
        }
    }
}
```

#### Media3 ExoPlayer Test
```java
@RunWith(AndroidJUnit4.class)
public class Media3ExoPlayerTest {
    @Test
    public void testPlayerInitialization() {
        Media3ExoPlayer player = new Media3ExoPlayer(context);
        assertNotNull("Player should be initialized", player);
    }
}
```

### Instrumentation Tests

For testing real device functionality:

```java
@RunWith(AndroidJUnit4.class)
public class OverlayServiceTest {
    @Test
    public void testServiceStartAndStop() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, OverlayService.class);
        
        // Start service
        context.startService(intent);
        
        // Verify service is running
        assertNotNull("Service should be running", 
            getRunningService(context, OverlayService.class));
        
        // Stop service
        context.stopService(intent);
    }
}
```

### Test Configuration

In `build.gradle`:

```gradle
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

## Build System

### Gradle Configuration

#### Root build.gradle
```gradle
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.4'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

#### App build.gradle
```gradle
android {
    namespace 'com.floatingvideoplayer'
    compileSdk 34

    defaultConfig {
        applicationId "com.floatingvideoplayer"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"
        
        multiDexEnabled true
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                          'proguard-rules.pro'
        }
    }
}
```

### Build Variants

1. **Debug Build**
   - Debugging enabled
   - Logging enabled
   - No code obfuscation
   - Larger APK size

2. **Release Build**
   - Code obfuscation enabled
   - Resource shrinking enabled
   - Optimization enabled
   - Smaller APK size

### ProGuard Rules

Comprehensive ProGuard configuration for:
- Media3 ExoPlayer classes
- Android framework classes
- Custom application classes
- Reflection and serialization

### Build Script

Automated build script (`build.sh`) provides:
- Clean build process
- Unit test execution
- APK generation
- Build artifact creation
- Installation instructions

## Deployment Guide

### Release Build Process

1. **Update Version Information**
   ```gradle
   // In app/build.gradle
   versionCode 2        // Increment for each release
   versionName "1.1"    // Human-readable version
   ```

2. **Configure Signing**
   - Create release keystore
   - Configure signing in build.gradle
   - Store credentials securely

3. **Run Build Script**
   ```bash
   chmod +x build.sh
   ./build.sh
   ```

4. **Verify APK**
   - Check APK size
   - Verify signature
   - Test on multiple devices

### APK Signing

#### Create Keystore
```bash
keytool -genkey -v \
    -keystore floatingvideoplayer.keystore \
    -alias floatingvideoplayer \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
```

#### Configure Signing
```gradle
android {
    signingConfigs {
        release {
            if (project.hasProperty('RELEASE_STORE_FILE')) {
                storeFile file(RELEASE_STORE_FILE)
                storePassword RELEASE_STORE_PASSWORD
                keyAlias RELEASE_KEY_ALIAS
                keyPassword RELEASE_KEY_PASSWORD
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### Distribution Checklist

Before releasing the APK:

- [ ] All permissions properly documented
- [ ] App tested on multiple Android versions
- [ ] Overlay functionality verified
- [ ] Storage permissions working
- [ ] Performance optimized
- [ ] Memory leaks fixed
- [ ] ProGuard rules applied
- [ ] APK signed with release keystore
- [ ] Installation instructions provided
- [ ] User manual completed
- [ ] Build artifacts organized

## Future Enhancements

### Planned Features

#### Version 1.1
- **Resizable Windows**: Allow users to resize floating windows
- **Multiple Windows**: Support multiple floating windows simultaneously
- **Window Snap**: Snap windows to screen edges
- **Playlist Support**: Play multiple videos in sequence

#### Version 1.2
- **Cloud Storage**: Integration with Google Drive, Dropbox
- **Streaming Support**: Play videos from URLs
- **Advanced Controls**: Fast forward, rewind, subtitle support
- **Themes**: Multiple UI themes and customization

#### Version 2.0
- **Audio Focus**: Better audio handling and ducking
- **Picture-in-Picture**: Native PiP mode
- **Widgets**: Home screen widgets for control
- **Accessibility**: Full accessibility support

### Technical Improvements

#### Performance
- **Hardware Acceleration**: Optimize for GPU decoding
- **Memory Pooling**: Reduce GC pressure
- **Smart Caching**: Predictive content caching
- **Background Processing**: Better background task management

#### User Experience
- **Gesture Controls**: Swipe, pinch, tap gestures
- **Voice Control**: Voice commands for hands-free operation
- **Smart Positioning**: AI-powered window positioning
- **Keyboard Shortcuts**: Hotkeys for power users

### Architecture Enhancements

#### Modular Design
- **Plugin System**: Allow third-party extensions
- **Service Abstraction**: Abstract service implementations
- **Event Bus**: Implement event-driven architecture
- **Dependency Injection**: Use Dagger/Hilt for dependency management

#### Modern Android
- **Jetpack Compose**: Migrate UI to Compose
- **Coroutines**: Replace ExecutorService with Kotlin coroutines
- **Flow**: Implement reactive data streams
- **WorkManager**: Use WorkManager for background tasks

## Contributing Guidelines

### Code Style

#### Java Style Guide
```java
// Class declaration
public class ExampleClass {
    private static final String TAG = "ExampleClass";
    private Context context;
    
    // Constructor
    public ExampleClass(Context context) {
        this.context = context;
    }
    
    // Public methods
    public void performAction() {
        // Implementation
    }
    
    // Private methods
    private void helperMethod() {
        // Implementation
    }
}
```

#### Naming Conventions
- **Classes**: PascalCase (e.g., `OverlayService`)
- **Methods**: camelCase (e.g., `startOverlay()`)
- **Constants**: UPPER_CASE (e.g., `MAX_BUFFER_SIZE`)
- **Variables**: camelCase (e.g., `windowManager`)

### Git Workflow

1. **Branch Naming**
   - Feature: `feature/video-controls`
   - Bugfix: `bugfix/overlay-permission`
   - Enhancement: `enhance/performance`

2. **Commit Messages**
   ```
   feat: add video controls to overlay window
   fix: resolve overlay permission issue on Android 12
   docs: update user manual with new features
   test: add unit tests for window manager
   ```

3. **Pull Request Process**
   - Create feature branch
   - Write tests for new code
   - Update documentation
   - Create pull request
   - Code review required

### Code Review Checklist

Before merging code:

- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] No memory leaks introduced
- [ ] Performance impact assessed
- [ ] Documentation updated
- [ ] Error handling implemented
- [ ] Accessibility considered
- [ ] Security implications reviewed

### Development Environment

#### Required Tools
- Android Studio (latest stable)
- Git for version control
- Java Development Kit 8+
- Android SDK (API 26-34)

#### Recommended Plugins
- SonarLint for code quality
- GitToolBox for git integration
- Key Promoter X for shortcuts

---

## Support and Maintenance

### Issue Tracking

1. **Bug Reports**
   - Detailed reproduction steps
   - Device information
   - Android version
   - Logcat output

2. **Feature Requests**
   - Use case description
   - Proposed implementation
   - Alternative solutions

3. **Performance Issues**
   - Memory usage reports
   - CPU profiling data
   - Battery usage statistics

### Maintenance Tasks

#### Regular Maintenance
- Update dependencies monthly
- Test on new Android versions
- Review and update permissions
- Performance monitoring
- Security audits

#### Version Management
- Semantic versioning (MAJOR.MINOR.PATCH)
- Changelog maintenance
- Backward compatibility testing
- Migration guides

This developer documentation provides comprehensive guidance for maintaining and enhancing the Floating Video Player application. For specific technical questions or implementation details, refer to the inline code comments and test cases.

---

**Happy Coding! 🚀**
