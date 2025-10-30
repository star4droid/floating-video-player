# Floating Window Service - Integration Checklist

## Pre-Integration Checklist

### ✅ Core Dependencies

- [ ] AndroidManifest.xml contains OverlayService declaration
- [ ] All required permissions are declared (SYSTEM_ALERT_WINDOW, storage permissions)
- [ ] MainActivity properly initializes permission handling
- [ ] Build.gradle includes Media3 dependencies
- [ ] All drawable resources are present

### ✅ Component Dependencies

- [ ] WindowManagerHelper available in utils package
- [ ] DraggableVideoPlayerWindow extends ViewGroup properly
- [ ] DraggableFileManagerWindow extends ViewGroup properly
- [ ] WindowControls properly implements control interfaces
- [ ] FloatingWindowIntegrationHelper coordinates all components
- [ ] FloatingWindowErrorHandler provides error handling

## Integration Points

### ✅ OverlayService Integration

```java
// Check service declaration in AndroidManifest.xml
<service
    android:name=".services.OverlayService"
    android:foregroundServiceType="mediaPlayback"
    android:stopWithTask="false" />
```

- [ ] Service properly handles ACTION_SHOW_FILE_MANAGER
- [ ] Service properly handles ACTION_SHOW_VIDEO_PLAYER
- [ ] Service properly handles ACTION_TOGGLE_OVERLAY
- [ ] Service handles ACTION_STOP_SERVICE
- [ ] Notification channel created for foreground service
- [ ] Service recovery mechanisms implemented

### ✅ WindowManagerHelper Integration

- [ ] createOverlayLayoutParams() used by all window components
- [ ] checkOverlayPermission() called before window creation
- [ ] constrainWindowToScreen() used for bounds checking
- [ ] resolveWindowConflicts() called when multiple windows active
- [ ] getOptimalWindowPosition() used for initial positioning

### ✅ Video Player Integration

```java
// DraggableVideoPlayerWindow setup
- [ ] ExoPlayer properly initialized with Media3
- [ ] PlayerView connected to ExoPlayer
- [ ] Media controls (play/pause, volume, seek) functional
- [ ] Video loading through loadVideo() method
- [ ] Touch events properly handled for drag/resize
- [ ] Bounds checking prevents window from leaving screen
```

### ✅ File Manager Integration

```java
// DraggableFileManagerWindow setup
- [ ] Directory navigation functional (up, home, refresh)
- [ ] File listing properly displays files and folders
- [ ] Video file detection working (mp4, avi, mkv, etc.)
- [ ] File selection triggers video player loading
- [ ] Touch events properly handled for drag/resize
- [ ] Bounds checking prevents window from leaving screen
```

### ✅ Control Panel Integration

```java
// WindowControls setup
- [ ] Control actions trigger appropriate window operations
- [ ] Button states update based on window visibility
- [ ] Drag functionality for control panel positioning
- [ ] Edge snapping functionality working
- [ ] Auto-hide mechanism functional
```

## Communication Flow

### ✅ File Selection to Video Playback

```
User selects video file → 
DraggableFileManagerWindow.handleFileSelection() → 
Check if video file → 
Call onVideoSelected() callback → 
DraggableVideoPlayerWindow.loadVideo() → 
ExoPlayer plays video
```

- [ ] File selection detection working
- [ ] Video file extension checking functional
- [ ] Callback properly triggers video loading
- [ ] Video player shows and loads selected file

### ✅ Control Actions to Window Management

```
User clicks control button → 
WindowControls.OnControlsActionListener → 
Show/hide appropriate window → 
Update control button states → 
Window visibility state synchronized
```

- [ ] Control button click handling functional
- [ ] Window show/hide operations working
- [ ] Button states update correctly
- [ ] Multiple window coordination working

### ✅ Service Communication

```
MainActivity → 
OverlayService → 
Component initialization → 
Window management → 
User interactions
```

- [ ] Service starts correctly from MainActivity
- [ ] All components initialized in service
- [ ] User actions properly handled by service
- [ ] Window state properly managed

## Error Handling Integration

### ✅ Permission Handling

- [ ] Overlay permission checked before window creation
- [ ] Permission request flow implemented
- [ ] Graceful handling of permission denial
- [ ] Storage permissions handled for file access

### ✅ Window Creation Errors

- [ ] Window creation failures handled gracefully
- [ ] Automatic retry mechanisms in place
- [ ] User feedback for creation failures
- [ ] Service recovery from creation errors

### ✅ Touch Event Errors

- [ ] Invalid touch events handled without crashes
- [ ] Bounds checking prevents invalid operations
- [ ] Event processing errors logged but don't stop operation
- [ ] Recovery from touch event processing errors

### ✅ Service Errors

- [ ] Service initialization errors handled
- [ ] Service communication errors recovered
- [ ] Memory pressure situations handled
- [ ] Configuration change handling

## Memory Management

### ✅ Resource Cleanup

- [ ] ExoPlayer properly released when window hidden
- [ ] Window references cleaned up on destroy
- [ ] Broadcast receivers properly registered/unregistered
- [ ] File handles and resources properly closed

### ✅ Low Memory Handling

- [ ] Service responds to onTrimMemory() calls
- [ ] Windows minimized on memory pressure
- [ ] Resources freed when appropriate
- [ ] Service continues operation after memory cleanup

## Testing Checklist

### ✅ Unit Tests

- [ ] Window creation/destruction tested
- [ ] Touch event handling tested
- [ ] Permission checking tested
- [ ] Bounds validation tested
- [ ] Error recovery tested

### ✅ Integration Tests

- [ ] File selection to video playback tested
- [ ] Multi-window scenarios tested
- [ ] Control panel operations tested
- [ ] Service lifecycle tested
- [ ] Error recovery tested

### ✅ User Interface Tests

- [ ] Drag functionality tested on different screen sizes
- [ ] Resize handles tested (corners and sides)
- [ ] Control buttons tested for all functions
- [ ] File browser navigation tested
- [ ] Video playback controls tested

### ✅ Error Scenario Tests

- [ ] Permission denial handling tested
- [ ] Window creation failure recovery tested
- [ ] Service restart scenarios tested
- [ ] Low memory situations tested
- [ ] Invalid file access handling tested

## Performance Checklist

### ✅ Touch Performance

- [ ] Drag operations are smooth (60fps)
- [ ] Resize operations are responsive
- [ ] Touch event processing is efficient
- [ ] No frame drops during interactions

### ✅ Memory Performance

- [ ] No memory leaks detected
- [ ] Memory usage stays within reasonable bounds
- [ ] Resources properly cleaned up
- [ ] Low memory situations handled gracefully

### ✅ Service Performance

- [ ] Service startup time is reasonable
- [ ] Window operations are responsive
- [ ] Background operation doesn't drain battery excessively
- [ ] Foreground service properly managed

## Security Checklist

### ✅ Permission Validation

- [ ] Overlay permission validated before window creation
- [ ] Storage permissions validated before file access
- [ ] Invalid permissions handled gracefully
- [ ] Security exceptions properly caught

### ✅ Input Validation

- [ ] File paths validated before loading
- [ ] User input sanitized in file manager
- [ ] No injection vulnerabilities
- [ ] Proper escaping of user content

## Deployment Checklist

### ✅ Build Configuration

- [ ] All dependencies properly declared
- [ ] Proguard rules configured if needed
- [ ] Manifest properly configured
- [ ] Resources properly organized

### ✅ Testing on Devices

- [ ] Tested on Android 6.0+ (API 23+)
- [ ] Tested on different screen sizes
- [ ] Tested on different Android versions
- [ ] Tested with/without root access
- [ ] Tested in multi-window mode

## Common Issues and Solutions

### ❌ Issue: Windows not appearing
**Solution:** Check overlay permission and service initialization

### ❌ Issue: Touch events not working
**Solution:** Verify WindowManager.LayoutParams flags and touch listener setup

### ❌ Issue: Service crashing on start
**Solution:** Check component initialization and error handling

### ❌ Issue: File manager not loading files
**Solution:** Verify storage permissions and file system access

### ❌ Issue: Video player not playing
**Solution:** Check Media3 initialization and media file access

## Final Verification

### ✅ Complete System Test

1. [ ] Start app and request permissions
2. [ ] Launch overlay service
3. [ ] Show file manager window
4. [ ] Navigate to video files
5. [ ] Select video file
6. [ ] Verify video loads in player
7. [ ] Test drag and resize operations
8. [ ] Test control panel operations
9. [ ] Test close all functionality
10. [ ] Verify service cleanup on exit

### ✅ Performance Verification

- [ ] No memory leaks during extended use
- [ ] Smooth interactions with no lag
- [ ] Proper battery usage
- [ ] Service remains stable during use

### ✅ Error Recovery Verification

- [ ] Service recovers from temporary errors
- [ ] User gets appropriate error messages
- [ ] System recovers from permission denials
- [ ] Touch errors don't crash the app

## Conclusion

This checklist ensures that all components of the floating window service are properly integrated and working together. Following this checklist will help identify and resolve any integration issues before deployment.

Remember to test on multiple devices and Android versions to ensure broad compatibility.
