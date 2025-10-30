# Floating Window Service - Complete Implementation

This document describes the complete floating window service implementation that provides draggable, resizable overlay windows for video playback and file management.

## Architecture Overview

The floating window service consists of several interconnected components:

### Core Components

1. **OverlayService** - Main service managing all floating windows
2. **WindowManagerHelper** - Utility class for window management operations
3. **DraggableVideoPlayerWindow** - Video player window with Media3 integration
4. **DraggableFileManagerWindow** - File browser window for video selection
5. **WindowControls** - Floating control panel for window management
6. **FloatingWindowIntegrationHelper** - Coordinates communication between components
7. **FloatingWindowErrorHandler** - Comprehensive error handling and recovery

### Layout Files

1. **floating_player_layout.xml** - Video player window layout
2. **floating_filemanager_layout.xml** - File manager window layout
3. **floating_controls_layout.xml** - Control panel layout
4. **floating_window_base.xml** - Base layout for draggable windows
5. **window_control_buttons.xml** - Control button components

## Component Details

### OverlayService

The `OverlayService` extends Android's `Service` class and runs in the background to manage floating windows. It provides:

- **Window Management**: Creates, shows, hides, and destroys floating windows
- **Service Lifecycle**: Handles onCreate, onStartCommand, and onDestroy events
- **Notification Management**: Runs as foreground service with persistent notification
- **Multi-window Support**: Manages multiple overlay windows simultaneously
- **Error Recovery**: Automatic recovery from service errors

**Key Methods:**
```java
public void loadVideoInPlayer(String videoPath)
public WindowState getWindowStates()
public boolean isServiceInitialized()
```

### WindowManagerHelper

Utility class providing window management utilities:

- **Window Creation**: Creates proper WindowManager.LayoutParams for overlays
- **Position Management**: Handles window positioning and bounds checking
- **Multi-window Support**: Manages multiple windows with conflict resolution
- **Permission Checking**: Validates overlay permissions
- **Size Constraints**: Enforces minimum and maximum window sizes

**Key Methods:**
```java
public WindowManager.LayoutParams createOverlayLayoutParams(int width, int height, int gravity)
public void constrainWindowToScreen(WindowManager.LayoutParams params)
public boolean checkOverlayPermission()
public int[] getOptimalWindowPosition(int width, int height)
```

### DraggableVideoPlayerWindow

Extends `ViewGroup` and provides a fully-featured video player window:

- **ExoPlayer Integration**: Uses Media3 ExoPlayer for video playback
- **Draggable Interface**: Touch-based window dragging
- **Resizable Interface**: Corner and side handles for resizing
- **Media Controls**: Play/pause, volume, seek, fullscreen controls
- **Bounds Checking**: Ensures windows stay within screen bounds

**Key Methods:**
```java
public void loadVideo(String videoUrl)
public void show()
public void hide()
public int[] getWindowBounds()
```

### DraggableFileManagerWindow

File browser window for selecting video files:

- **File System Navigation**: Browse directory structures
- **Video File Detection**: Identifies video files by extension
- **File Selection**: Calls video player when video files are selected
- **Draggable Interface**: Touch-based window dragging
- **Resizable Interface**: Corner and side handles for resizing

**Key Methods:**
```java
public void setOnVideoSelectedListener(OnVideoSelectedListener listener)
public File getCurrentDirectory()
public void loadDirectory(File directory)
```

### WindowControls

Floating control panel for managing windows:

- **Window Toggles**: Show/hide video player and file manager
- **Settings Access**: Quick access to app settings
- **Close All**: Close all windows at once
- **Draggable Interface**: Touch-based control panel dragging

**Key Methods:**
```java
public void setOnControlsActionListener(OnControlsActionListener listener)
public void updateButtonStates(boolean videoPlayerVisible, boolean fileManagerVisible)
public void showAtPosition(int x, int y)
```

## Integration Features

### Component Communication

The `FloatingWindowIntegrationHelper` manages communication between components:

- **File Selection**: Automatically loads selected videos in the player
- **State Synchronization**: Keeps control states in sync with window visibility
- **Broadcast Communication**: Uses Android broadcasts for decoupled communication
- **Error Handling**: Centralized error handling across all components

### Error Handling

The `FloatingWindowErrorHandler` provides comprehensive error handling:

- **Automatic Recovery**: Attempts automatic recovery from recoverable errors
- **User Feedback**: Shows appropriate error messages to users
- **Logging**: Detailed error logging for debugging
- **Permission Management**: Handles permission-related errors gracefully

## Touch Event System

All window components support sophisticated touch event handling:

### Drag Operations

- **Drag Detection**: Recognizes drag gestures vs. control interactions
- **Bounds Constraints**: Prevents windows from being moved off-screen
- **Smooth Movement**: Provides smooth drag experience with proper coordinates

### Resize Operations

- **Corner Resizing**: Resize from all four corners
- **Side Resizing**: Resize from all four sides
- **Size Constraints**: Enforces minimum and maximum window sizes
- **Aspect Ratio**: Maintains reasonable aspect ratios during resize

## Window Management

### Z-Order Management

- **Layer Control**: Proper z-ordering to ensure visibility
- **Conflict Resolution**: Automatically resolves window position conflicts
- **Bring-to-Front**: Windows can be brought to front when interacted with

### Bounds Management

- **Screen Constraints**: Windows stay within screen boundaries
- **Safe Areas**: Consider status bars and navigation areas
- **Multi-Screen Support**: Adapts to different screen sizes and orientations

## Permission Management

### Overlay Permissions

- **Permission Checking**: Validates overlay permission before window creation
- **Permission Requests**: Guides user through permission granting process
- **Graceful Degradation**: Works without overlay permissions where possible

### Storage Permissions

- **File Access**: Handles storage permissions for file manager functionality
- **Media Permissions**: Uses Media3 for efficient media file access

## Service Lifecycle

### Service Creation

1. Initialize all window components
2. Create notification channel
3. Start foreground service
4. Register broadcast receivers

### Service Operation

1. Handle user actions (show/hide windows)
2. Manage window state and positioning
3. Process file selections and video loading
4. Monitor and respond to configuration changes

### Service Cleanup

1. Hide all windows
2. Release player resources
3. Unregister receivers
4. Clean up component references

## Error Recovery

### Automatic Recovery

The system attempts automatic recovery from:

- **Window Creation Failures**: Retries with different parameters
- **Permission Errors**: Re-requests permissions when needed
- **Touch Event Errors**: Continues operation despite individual event failures
- **Service Communication**: Maintains service operation during temporary issues

### User Recovery

When automatic recovery fails:

1. Shows user-friendly error messages
2. Provides guidance for manual recovery
3. Offers to restart service if needed
4. Logs detailed information for debugging

## Usage Examples

### Starting the Service

```java
Intent serviceIntent = new Intent(context, OverlayService.class);
serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_SHOW_FILE_MANAGER);
context.startService(serviceIntent);
```

### Loading a Video

```java
// Through the service
overlayService.loadVideoInPlayer("/sdcard/videos/sample.mp4");

// Through file manager
fileManagerWindow.setOnVideoSelectedListener(path -> {
    videoPlayerWindow.loadVideo(path);
});
```

### Managing Windows

```java
// Toggle windows
windowControls.setOnControlsActionListener(new WindowControls.OnControlsActionListener() {
    @Override
    public void onShowVideoPlayer() {
        videoPlayerWindow.show();
    }
    
    @Override
    public void onShowFileManager() {
        fileManagerWindow.show();
    }
});
```

## Performance Considerations

### Memory Management

- **Lazy Loading**: Window components are created only when needed
- **Resource Cleanup**: Proper cleanup of ExoPlayer and window resources
- **Memory Monitoring**: Handles low memory situations gracefully

### Battery Optimization

- **Foreground Service**: Uses foreground service to prevent background termination
- **Minimal Wake Lock**: Avoids unnecessary CPU usage
- **Efficient Rendering**: Optimized window rendering and touch handling

## Security Considerations

### Permission Validation

- Validates overlay permissions before creating windows
- Checks storage permissions before file access
- Handles permission denial gracefully

### Input Validation

- Validates file paths before loading videos
- Sanitizes user input in file manager
- Prevents injection attacks through proper escaping

## Testing Recommendations

### Unit Testing

- Test window creation and destruction
- Validate touch event handling
- Test permission checking logic

### Integration Testing

- Test component communication
- Validate error recovery mechanisms
- Test multi-window scenarios

### User Testing

- Test drag and resize functionality
- Validate file selection and video loading
- Test error scenarios and recovery

## Future Enhancements

### Planned Features

1. **Multiple Video Players**: Support for multiple video windows
2. **Advanced Controls**: Playlist management, video effects
3. **Network Streaming**: Support for online video streams
4. **Accessibility**: Enhanced accessibility support
5. **Themes**: Customizable window themes and styling

### Technical Improvements

1. **Performance Optimization**: Further optimization for low-end devices
2. **Better Error Handling**: More sophisticated error recovery
3. **Configuration Management**: Better handling of configuration changes
4. **Testing Framework**: Automated testing for all components

## Conclusion

The floating window service provides a complete, robust solution for overlay-based video playback and file management. The modular architecture allows for easy extension and customization, while comprehensive error handling ensures reliable operation across different devices and usage scenarios.
