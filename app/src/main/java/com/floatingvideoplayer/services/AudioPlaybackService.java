package com.floatingvideoplayer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCallback;

import com.floatingvideoplayer.FloatingVideoPlayerApp;
import com.floatingvideoplayer.R;
import com.floatingvideoplayer.ui.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Background audio playback service with notification controls,
 * audio focus management, and session integration
 */
public class AudioPlaybackService extends MediaLibraryService implements AudioManager.OnAudioFocusChangeListener {
    
    private static final String TAG = "AudioPlaybackService";
    
    // Notification constants
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "audio_playback_channel";
    private static final String CHANNEL_NAME = "Audio Playback";
    
    // Audio focus
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean hasAudioFocus = false;
    private boolean wasPlayingBeforeFocusLoss = false;
    
    // Media session components
    private MediaSession mediaSession;
    private MediaController mediaController;
    private AudioPlaybackSessionCallback sessionCallback;
    
    // Player components
    private Media3ExoPlayer media3ExoPlayer;
    private PlaylistManager playlistManager;
    
    // Background operations
    private final Executor backgroundExecutor = Executors.newFixedThreadPool(2);
    
    // State tracking
    private boolean isForegroundService = false;
    private Map<String, Bitmap> albumArtCache = new HashMap<>();
    
    // Binder for client connections
    private final IBinder binder = new AudioPlaybackBinder();
    
    public class AudioPlaybackBinder extends Binder {
        public AudioPlaybackService getService() {
            return AudioPlaybackService.this;
        }
    }
    
    /**
     * Custom media session callback for handling playback controls
     */
    private class AudioPlaybackSessionCallback extends SessionCallback {
        
        private MediaLibraryService.MediaLibrarySession.Builder sessionBuilder;
        
        public AudioPlaybackSessionCallback() {
            super();
            sessionBuilder = new MediaLibraryService.MediaLibrarySession.Builder(
                AudioPlaybackService.this,
                new MediaSession.Callback() {
                    @Override
                    public MediaSession.ConnectionResponse onConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
                        return session.acceptFromController(controller);
                    }
                    
                    @Override
                    public void onDisconnected(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
                        // Handle controller disconnection
                        Log.d(TAG, "Controller disconnected: " + controller);
                    }
                }
            );
        }
        
        @Override
        public void onCommand(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull String command, 
                            android.os.Bundle args, @NonNull SessionCallback.CommandReceiver receiver) {
            
            switch (command) {
                case "play_pause":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.playPause();
                    }
                    break;
                    
                case "previous":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.previous();
                    }
                    break;
                    
                case "next":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.next();
                    }
                    break;
                    
                case "seek_to":
                    if (args != null && media3ExoPlayer != null) {
                        long position = args.getLong("position", 0);
                        media3ExoPlayer.seekTo(position);
                    }
                    break;
                    
                default:
                    super.onCommand(session, controller, command, args, receiver);
            }
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AudioPlaybackService onCreate");
        
        initializeComponents();
        setupAudioFocus();
        setupMediaSession();
        setupPlayerListeners();
    }
    
    /**
     * Initialize service components
     */
    private void initializeComponents() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        sessionCallback = new AudioPlaybackSessionCallback();
    }
    
    /**
     * Setup audio focus management
     */
    private void setupAudioFocus() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(this)
                .setWillPauseWhenDucked(true)
                .build();
        }
    }
    
    /**
     * Setup media session
     */
    private void setupMediaSession() {
        mediaSession = sessionCallback.sessionBuilder
            .build();
        
        setSessionToken(mediaSession.getSessionCompatToken());
    }
    
    /**
     * Setup player event listeners
     */
    private void setupPlayerListeners() {
        if (media3ExoPlayer != null) {
            media3ExoPlayer.setOnPlaybackStateChangeListener((state, isPlaying, position, duration) -> {
                updateNotification();
            });
            
            media3ExoPlayer.setOnPlaylistChangeListener(playlist -> {
                updateNotification();
            });
            
            media3ExoPlayer.setOnErrorListener(error -> {
                Log.e(TAG, "Player error in service", error);
                stopForeground(false);
            });
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));
        
        if (intent != null) {
            String action = intent.getAction();
            
            switch (action) {
                case "play_pause":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.playPause();
                    }
                    break;
                    
                case "previous":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.previous();
                    }
                    break;
                    
                case "next":
                    if (media3ExoPlayer != null) {
                        media3ExoPlayer.next();
                    }
                    break;
                    
                case "stop":
                    stopPlayback();
                    break;
            }
        }
        
        // Auto-restart behavior for foreground service
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        releaseAudioFocus();
        
        if (mediaSession != null) {
            mediaSession.release();
        }
        
        if (media3ExoPlayer != null) {
            media3ExoPlayer.onDestroy();
        }
        
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Set the media player
     */
    public void setMediaPlayer(Media3ExoPlayer media3ExoPlayer) {
        this.media3ExoPlayer = media3ExoPlayer;
        setupPlayerListeners();
    }
    
    /**
     * Set the playlist manager
     */
    public void setPlaylistManager(PlaylistManager playlistManager) {
        this.playlistManager = playlistManager;
    }
    
    /**
     * Start foreground service with notification
     */
    public void startForegroundService() {
        if (!isForegroundService) {
            createNotificationChannel();
            
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            isForegroundService = true;
        }
    }
    
    /**
     * Stop foreground service
     */
    public void stopForegroundService() {
        if (isForegroundService) {
            stopForeground(true);
            isForegroundService = false;
        }
    }
    
    /**
     * Update notification
     */
    private void updateNotification() {
        if (isForegroundService) {
            Notification notification = createNotification();
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            
            channel.setDescription("Controls for background audio playback");
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create notification with playback controls
     */
    private Notification createNotification() {
        MediaItem currentItem = media3ExoPlayer != null ? 
            media3ExoPlayer.getCurrentMediaItem() : null;
        
        boolean isPlaying = media3ExoPlayer != null && media3ExoPlayer.isPlaying();
        
        // Get notification content
        String title = "Unknown";
        String subtitle = "No media";
        
        if (currentItem != null) {
            MediaMetadata metadata = currentItem.mediaMetadata;
            if (metadata != null) {
                title = metadata.title != null ? 
                    metadata.title.toString() : "Unknown Title";
                subtitle = metadata.artist != null ? 
                    metadata.artist.toString() : "Media Player";
            }
        }
        
        // Create playback actions
        PendingIntent previousIntent = createPlaybackAction("previous", 0);
        PendingIntent playPauseIntent = createPlaybackAction("play_pause", 1);
        PendingIntent nextIntent = createPlaybackAction("next", 2);
        PendingIntent stopIntent = createPlaybackAction("stop", 3);
        
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(new NotificationCompat.MediaStyle())
            .setShowActionsInCompactView(0, 1, 2)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_previous, "Previous", previousIntent)
            .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", playPauseIntent)
            .addAction(R.drawable.ic_next, "Next", nextIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent);
        
        // Set intent to open main activity
        PendingIntent contentIntent = PendingIntent.getActivity(
            this, 0, new Intent(this, MainActivity.class), 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.setContentIntent(contentIntent);
        
        // Add album art if available
        if (currentItem != null) {
            // Load album art asynchronously
            loadAlbumArt(currentItem).thenAccept(bitmap -> {
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap);
                    updateNotification(); // Refresh notification with album art
                }
            });
        }
        
        return builder.build();
    }
    
    /**
     * Create pending intent for playback actions
     */
    private PendingIntent createPlaybackAction(String action, int requestCode) {
        Intent intent = new Intent(this, AudioPlaybackService.class);
        intent.setAction(action);
        
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    /**
     * Load album art from media metadata
     */
    private CompletableFuture<Bitmap> loadAlbumArt(MediaItem mediaItem) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For now, return null as album art loading requires more complex implementation
                // In a real app, you would extract album art from media metadata
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art", e);
                return null;
            }
        }, backgroundExecutor);
    }
    
    /**
     * Create placeholder album art
     */
    private Bitmap createPlaceholderAlbumArt() {
        // Create a simple colored rectangle as placeholder
        Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        canvas.drawColor(Color.DKGRAY);
        
        return bitmap;
    }
    
    /**
     * Request audio focus
     */
    private boolean requestAudioFocus() {
        if (hasAudioFocus) {
            return true;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            );
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        
        return hasAudioFocus;
    }
    
    /**
     * Release audio focus
     */
    private void releaseAudioFocus() {
        if (hasAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(this);
            }
            hasAudioFocus = false;
        }
    }
    
    /**
     * Handle audio focus changes
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "Audio focus changed: " + focusChange);
        
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // Resume playback if needed
                if (wasPlayingBeforeFocusLoss && media3ExoPlayer != null) {
                    media3ExoPlayer.play();
                    wasPlayingBeforeFocusLoss = false;
                }
                hasAudioFocus = true;
                break;
                
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus permanently, stop playback
                wasPlayingBeforeFocusLoss = false;
                stopPlayback();
                hasAudioFocus = false;
                break;
                
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus temporarily
                wasPlayingBeforeFocusLoss = media3ExoPlayer != null && media3ExoPlayer.isPlaying();
                if (media3ExoPlayer != null) {
                    media3ExoPlayer.pause();
                }
                break;
                
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Ducking - reduce volume
                if (media3ExoPlayer != null) {
                    media3ExoPlayer.setVolume(0.3f);
                }
                break;
        }
    }
    
    /**
     * Stop playback and cleanup
     */
    private void stopPlayback() {
        if (media3ExoPlayer != null) {
            media3ExoPlayer.pause();
        }
        stopForegroundService();
        stopSelf();
    }
    
    /**
     * Get current media controller
     */
    @Nullable
    public MediaController getMediaController() {
        return mediaController;
    }
    
    /**
     * Check if service has audio focus
     */
    public boolean hasAudioFocus() {
        return hasAudioFocus;
    }
    
    /**
     * Request audio focus when needed
     */
    public boolean requestAudioFocusIfNeeded() {
        if (!hasAudioFocus) {
            return requestAudioFocus();
        }
        return true;
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        // App was removed from recent apps, keep service running in background
        super.onTaskRemoved(rootIntent);
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        // Clear album art cache when memory is low
        if (level >= TRIM_MEMORY_MODERATE) {
            albumArtCache.clear();
        }
    }
}
