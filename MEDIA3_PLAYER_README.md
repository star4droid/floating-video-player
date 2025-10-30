# Media3 ExoPlayer Video Player Implementation

## Overview

This implementation provides a comprehensive Media3-based video player with floating window support, advanced playback controls, and seamless playlist management. The system is built with modern Android architecture patterns and supports both video and audio playback.

## Architecture Components

### 1. Media3ExoPlayer (`Media3ExoPlayer.java`)

**Purpose**: Core playback engine extending ExoPlayer from Media3 library

**Key Features**:
- **Multi-format Support**: MP4, AVI, MOV, WMV, FLV, WEBM, MKV for video; MP3, FLAC, WAV, AAC, OGG, M4A for audio
- **Advanced Controls**: Play/pause, next/previous, seek functionality with position tracking
- **Smart Queue Management**: Auto-play next video when current ends, with repeat mode support
- **Audio Management**: Volume control (0.0-1.0), playback speed adjustment (0.25x-2.0x)
- **Shuffle & Repeat Modes**: Fisher-Yates shuffle algorithm, repeat modes (OFF, ONE, ALL)
- **Event Handling**: Comprehensive listener system for playback state changes and errors

**Usage Example**:
```java
Media3ExoPlayer player = new Media3ExoPlayer(context);

// Load single media item
MediaItem mediaItem = new MediaItem.Builder()
    .setUri(videoUri)
    .build();
player.loadMediaItem(mediaItem);

// Load playlist
List<MediaItem> playlist = /* build playlist */;
player.loadPlaylist(playlist);

// Control playback
player.playPause();
player.next();
player.seekTo(30000); // Seek to 30 seconds
player.setVolume(0.5f); // Set to 50% volume
```

### 2. VideoPlayerWindow (`VideoPlayerWindow.java`)

**Purpose**: Floating window UI with responsive playback controls

**Key Features**:
- **Responsive Layout**: Adapts to different window sizes, works in small floating windows
- **Dual Mode Support**: Video playback with thumbnail overlay, audio-only mode with album art
- **Smart Controls Overlay**: Auto-hide controls during playback, tap to reveal
- **Interactive Elements**: 
  - Main controls: Play/pause, previous, next
  - Secondary controls: Volume, speed, format selection, file manager toggle
  - Seek bar with current time and duration display
- **Touch Support**: Drag for window movement (audio mode), tap to toggle controls
- **Visual Feedback**: Loading states, error notifications, control visibility states

**Usage Example**:
```java
VideoPlayerWindow playerWindow = new VideoPlayerWindow(context);
playerWindow.setMediaPlayer(media3ExoPlayer);

// Set callbacks
playerWindow.setOnFileManagerToggleListener(show -> {
    // Toggle file manager visibility
});

playerWindow.setOnVideoClickListener(() -> {
    // Handle video tap
});
```

### 3. PlaylistManager (`PlaylistManager.java`)

**Purpose**: Advanced playlist and queue management system

**Key Features**:
- **Smart Playlist Creation**: From URIs, MediaStore queries, with duplicate detection
- **Advanced Navigation**: Shuffle order preservation, repeat mode handling
- **Metadata Enrichment**: File information (duration, size, MIME type) for each item
- **Background Operations**: Async loading using thread pools for performance
- **Event System**: Real-time playlist and playback change notifications
- **Repeat Modes**: OFF, ONE, ALL with proper loop handling
- **Shuffle Algorithm**: Fisher-Yates shuffle with position tracking

**Usage Example**:
```java
PlaylistManager playlistManager = new PlaylistManager();

// Create playlist from URIs
List<Uri> videoUris = /* get URIs */;
CompletableFuture<PlaylistResult> result = 
    playlistManager.createPlaylistFromUris(videoUris);

// Set playlist and navigate
playlistManager.setPlaylist(playlistItems);
PlaylistItem current = playlistManager.getCurrentItem();
PlaylistItem next = playlistManager.next();

// Control modes
playlistManager.setShuffleMode(true);
playlistManager.setRepeatMode(PlaylistManager.RepeatMode.ONE);
```

### 4. AudioPlaybackService (`AudioPlaybackService.java`)

**Purpose**: Background audio playback with notification controls and audio focus

**Key Features**:
- **Background Playback**: Continues when app is backgrounded or device is locked
- **Media Session Integration**: System-level playback controls and metadata
- **Notification Controls**: Lock screen and status bar controls with album art
- **Audio Focus Management**: Proper handling of interruptions and volume ducking
- **Service Lifecycle**: Auto-restart, foreground service management
- **Media Controller**: Integration with system media controls

**Usage Example**:
```java
AudioPlaybackService service = new AudioPlaybackService();
service.setMediaPlayer(media3ExoPlayer);
service.setPlaylistManager(playlistManager);

// Start background service
service.startForegroundService();

// Handle audio focus
service.requestAudioFocusIfNeeded();
```

### 5. Media3ErrorHandler (`Media3ErrorHandler.java`)

**Purpose**: Comprehensive error handling and recovery system

**Key Features**:
- **Format Validation**: Pre-playback format checking and validation
- **Error Classification**: Categorized error types (UNSUPPORTED, NETWORK, DRM, etc.)
- **Recovery Strategies**: Configurable recovery methods (RETRY, SKIP, IGNORE, etc.)
- **Format Detection**: MIME type detection and extension-based guessing
- **User Feedback**: Toast notifications and detailed error messages
- **Validation Tools**: MediaExtractor-based file validation
- **Error Reporting**: Comprehensive error report generation

**Usage Example**:
```java
Media3ErrorHandler errorHandler = new Media3ErrorHandler(context, callback);

// Validate format before playback
if (errorHandler.validateFormatWithDetails(uri, mimeType, true)) {
    // Safe to play
}

// Handle playback errors
errorHandler.setRecoveryStrategy(ErrorType.NETWORK_ERROR, RecoveryStrategy.RETRY);
```

## File Structure

```
android_project/app/src/main/java/com/floatingvideoplayer/
├── services/
│   ├── Media3ExoPlayer.java           # Core playback engine
│   ├── PlaylistManager.java           # Playlist management
│   └── AudioPlaybackService.java      # Background audio service
├── ui/
│   └── VideoPlayerWindow.java         # Floating window UI
└── utils/
    └── Media3ErrorHandler.java        # Error handling utility

android_project/app/src/main/res/
├── layout/
│   └── view_video_player_window.xml   # Video player layout
├── drawable/
│   ├── ic_previous.xml               # Playback control icons
│   ├── ic_next.xml
│   ├── ic_volume_up.xml
│   ├── ic_volume_mute.xml
│   ├── ic_speed.xml
│   ├── ic_file.xml
│   ├── ic_video_file.xml
│   ├── ic_audio_file.xml
│   ├── ic_folder_closed.xml
│   ├── ic_folder_open.xml
│   ├── controls_background.xml       # UI backgrounds
│   ├── button_background.xml
│   ├── play_button_background.xml
│   └── album_art_background.xml
└── values/
    └── colors.xml                     # Color definitions
```

## Integration Steps

### 1. Setup Dependencies
The Media3 dependencies are already configured in `build.gradle`:
- `media3-exoplayer:1.2.1`
- `media3-session:1.2.1`
- `media3-ui:1.2.1`

### 2. Initialize Components
```java
// Create player and managers
Media3ExoPlayer player = new Media3ExoPlayer(context);
PlaylistManager playlistManager = new PlaylistManager();
Media3ErrorHandler errorHandler = new Media3ErrorHandler(context, errorCallback);

// Set up UI
VideoPlayerWindow playerWindow = new VideoPlayerWindow(context);
playerWindow.setMediaPlayer(player);

// Connect components
player.setOnPlaylistChangeListener(playlistManager);
playlistManager.addOnPlaybackChangeListener(item -> {
    // Handle playback changes
});
```

### 3. Handle Media Files
```java
// Load media files
Uri videoUri = Uri.parse("content://media/external/video/media/123");
String mimeType = errorHandler.getMimeTypeFromUri(videoUri);

if (errorHandler.validateFormatWithDetails(videoUri, mimeType, true)) {
    MediaItem mediaItem = new MediaItem.Builder()
        .setUri(videoUri)
        .setMimeType(mimeType)
        .build();
    
    playlistManager.addToPlaylist(new PlaylistManager.PlaylistItem(
        mediaItem, 0, "Video", 0, 0, mimeType, videoUri
    ));
}
```

### 4. Setup Background Service
```java
// In your Activity/Service
Intent serviceIntent = new Intent(context, AudioPlaybackService.class);
context.startService(serviceIntent);

// Bind service for control
ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        AudioPlaybackService.AudioPlaybackBinder binder = 
            (AudioPlaybackService.AudioPlaybackBinder) service;
        AudioPlaybackService audioService = binder.getService();
        audioService.setMediaPlayer(player);
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) { }
};

context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
```

## Key Features

### Advanced Playback
- **Auto-play Queues**: Automatically plays next item when current ends
- **Format Support**: Extensive codec and container support
- **Quality Adaptation**: Automatic quality selection for streaming content
- **Background Playback**: Continues audio when app is backgrounded

### User Experience
- **Floating Window**: Resizable, draggable playback window
- **Responsive Controls**: Touch-friendly interface with visual feedback
- **Smart Navigation**: Gesture-based controls and intuitive navigation
- **Audio Visualization**: Album art and metadata display for audio-only mode

### Technical Excellence
- **Error Recovery**: Comprehensive error handling with recovery strategies
- **Performance**: Background operations and efficient resource management
- **Memory Management**: Proper cleanup and memory leak prevention
- **Accessibility**: Proper content descriptions and touch target sizes

### Integration Ready
- **File Manager**: Seamless integration with file system
- **Permissions**: Proper handling of storage permissions
- **System Integration**: Media session and notification controls
- **Audio Focus**: System-compliant audio focus management

## Error Handling

The system provides robust error handling for:

- **Unsupported Formats**: Graceful fallback with user notification
- **Network Issues**: Retry mechanisms for streaming content
- **DRM Content**: Proper handling of protected content
- **Permission Errors**: Clear permission request flow
- **Resource Constraints**: Memory and CPU optimization
- **Playback Failures**: Automatic recovery and fallback strategies

## Performance Considerations

- **Background Operations**: All heavy operations use background threads
- **Memory Management**: Efficient bitmap caching and resource cleanup
- **Battery Optimization**: Proper lifecycle management and power efficient operations
- **Network Optimization**: Efficient streaming and caching strategies

## Future Enhancements

The architecture supports easy extension for:
- Web streaming protocols (HLS, DASH)
- Advanced audio effects and equalization
- Playlist synchronization across devices
- Cloud storage integration
- Advanced analytics and telemetry

This implementation provides a production-ready foundation for a modern video player with all the requested features and more.
