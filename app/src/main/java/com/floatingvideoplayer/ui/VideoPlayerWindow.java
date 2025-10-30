package com.floatingvideoplayer.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.floatingvideoplayer.R;
import com.floatingvideoplayer.services.Media3ExoPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Advanced video player window with floating layout support,
 * responsive UI, and comprehensive playback controls
 */
public class VideoPlayerWindow extends FrameLayout {
    
    private static final String TAG = "VideoPlayerWindow";
    
    // UI Components
    private ConstraintLayout container;
    private View videoSurface;
    private ImageView thumbnailView;
    private ProgressBar loadingProgress;
    
    // Playback controls
    private LinearLayout controlsContainer;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton volumeButton;
    private ImageButton speedButton;
    private ImageButton formatButton;
    
    // Seek controls
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView durationText;
    
    // Audio-only mode
    private LinearLayout audioInfoContainer;
    private ImageView albumArtView;
    private TextView titleText;
    private TextView artistText;
    
    // File manager integration
    private ImageButton fileManagerButton;
    private boolean isFileManagerVisible = false;
    
    // State variables
    private boolean isControlsVisible = true;
    private boolean isDragging = false;
    private boolean isAudioOnlyMode = false;
    
    // Animations
    private ValueAnimator fadeAnimator;
    private Handler controlsHandler = new Handler(Looper.getMainLooper());
    private static final int HIDE_CONTROLS_DELAY = 3000; // 3 seconds
    
    // Media3 ExoPlayer instance
    private Media3ExoPlayer media3ExoPlayer;
    
    // Position update
    private Handler positionUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable positionUpdateRunnable;
    private static final int POSITION_UPDATE_INTERVAL = 1000; // 1 second
    
    // Background executor for heavy operations
    private final Executor backgroundExecutor = Executors.newFixedThreadPool(2);
    
    // Drag support
    private float lastTouchX, lastTouchY;
    private boolean isMoving = false;
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;
    
    // Callbacks
    private OnFileManagerToggleListener fileManagerToggleListener;
    private OnVideoClickListener videoClickListener;
    
    public interface OnFileManagerToggleListener {
        void onFileManagerToggle(boolean show);
    }
    
    public interface OnVideoClickListener {
        void onVideoClick();
    }
    
    // Constructors
    public VideoPlayerWindow(@NonNull Context context) {
        super(context);
        initializeView(context);
    }
    
    public VideoPlayerWindow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeView(context);
    }
    
    public VideoPlayerWindow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView(context);
    }
    
    /**
     * Initialize the video player window view
     */
    private void initializeView(Context context) {
        setClipChildren(false);
        setClipToPadding(false);
        
        // Inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_video_player_window, this, true);
        
        initializeComponents(view);
        setupListeners();
        setupDragSupport();
        setupAnimations();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeComponents(View view) {
        container = view.findViewById(R.id.container);
        videoSurface = view.findViewById(R.id.video_surface);
        thumbnailView = view.findViewById(R.id.thumbnail_view);
        loadingProgress = view.findViewById(R.id.loading_progress);
        
        // Playback controls
        controlsContainer = view.findViewById(R.id.controls_container);
        playPauseButton = view.findViewById(R.id.btn_play_pause);
        previousButton = view.findViewById(R.id.btn_previous);
        nextButton = view.findViewById(R.id.btn_next);
        volumeButton = view.findViewById(R.id.btn_volume);
        speedButton = view.findViewById(R.id.btn_speed);
        formatButton = view.findViewById(R.id.btn_format);
        fileManagerButton = view.findViewById(R.id.btn_file_manager);
        
        // Seek controls
        seekBar = view.findViewById(R.id.seek_bar);
        currentTimeText = view.findViewById(R.id.text_current_time);
        durationText = view.findViewById(R.id.text_duration);
        
        // Audio-only mode
        audioInfoContainer = view.findViewById(R.id.audio_info_container);
        albumArtView = view.findViewById(R.id.album_art);
        titleText = view.findViewById(R.id.text_title);
        artistText = view.findViewById(R.id.text_artist);
    }
    
    /**
     * Setup all click listeners and event handlers
     */
    private void setupListeners() {
        // Playback controls
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        previousButton.setOnClickListener(v -> seekToPrevious());
        nextButton.setOnClickListener(v -> seekToNext());
        volumeButton.setOnClickListener(v -> toggleVolume());
        speedButton.setOnClickListener(v -> showSpeedOptions());
        formatButton.setOnClickListener(v -> showFormatOptions());
        fileManagerButton.setOnClickListener(v -> toggleFileManager());
        
        // Seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && media3ExoPlayer != null) {
                    long duration = media3ExoPlayer.getDuration();
                    long newPosition = (duration * progress) / 1000;
                    media3ExoPlayer.seekTo(newPosition);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isDragging = true;
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isDragging = false;
            }
        });
        
        // Container click to toggle controls
        container.setOnClickListener(v -> toggleControls());
        videoSurface.setOnClickListener(v -> onVideoClick());
        
        // Video surface touch for dragging
        videoSurface.setOnTouchListener((view, event) -> {
            handleVideoSurfaceTouch(event);
            return true;
        });
    }
    
    /**
     * Setup drag support for floating window
     */
    private void setupDragSupport() {
        setOnTouchListener((view, event) -> {
            return handleGlobalTouch(event);
        });
    }
    
    /**
     * Setup fade animations
     */
    private void setupAnimations() {
        // Initial state - controls visible
        updateControlsVisibility(true);
    }
    
    /**
     * Set the Media3ExoPlayer instance
     */
    public void setMediaPlayer(Media3ExoPlayer media3ExoPlayer) {
        this.media3ExoPlayer = media3ExoPlayer;
        
        if (media3ExoPlayer != null) {
            setupPlayerListeners();
            updatePlaybackState();
            startPositionUpdates();
        }
    }
    
    /**
     * Setup player event listeners
     */
    private void setupPlayerListeners() {
        if (media3ExoPlayer == null) return;
        
        media3ExoPlayer.setOnPlaybackStateChangeListener((state, isPlaying, position, duration) -> {
            updatePlaybackState();
            updateSeekBar(position, duration);
            
            // Handle audio-only mode
            isAudioOnlyMode = media3ExoPlayer.isCurrentItemAudioOnly();
            updateModeDisplay();
            
            // Update loading state
            updateLoadingState(state == Player.STATE_BUFFERING);
        });
        
        media3ExoPlayer.setOnErrorListener(error -> {
            Log.e(TAG, "Player error", error);
            showError("Playback error: " + error.getMessage());
        });
        
        media3ExoPlayer.setOnPlaylistChangeListener(playlist -> {
            // Update UI when playlist changes
            updateMediaInfo();
        });
    }
    
    /**
     * Start position update loop
     */
    private void startPositionUpdates() {
        if (positionUpdateRunnable != null) {
            positionUpdateHandler.removeCallbacks(positionUpdateRunnable);
        }
        
        positionUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (media3ExoPlayer != null && !isDragging) {
                    updateSeekBar(media3ExoPlayer.getCurrentPosition(), 
                                 media3ExoPlayer.getDuration());
                }
                positionUpdateHandler.postDelayed(this, POSITION_UPDATE_INTERVAL);
            }
        };
        
        positionUpdateHandler.post(positionUpdateRunnable);
    }
    
    /**
     * Stop position updates
     */
    private void stopPositionUpdates() {
        if (positionUpdateRunnable != null) {
            positionUpdateHandler.removeCallbacks(positionUpdateRunnable);
        }
    }
    
    // Playback control methods
    
    private void togglePlayPause() {
        if (media3ExoPlayer != null) {
            media3ExoPlayer.playPause();
        }
    }
    
    private void seekToPrevious() {
        if (media3ExoPlayer != null) {
            media3ExoPlayer.previous();
        }
    }
    
    private void seekToNext() {
        if (media3ExoPlayer != null) {
            media3ExoPlayer.next();
        }
    }
    
    private void toggleVolume() {
        if (media3ExoPlayer != null) {
            float currentVolume = media3ExoPlayer.getVolume();
            float newVolume = currentVolume > 0 ? 0.0f : 1.0f;
            media3ExoPlayer.setVolume(newVolume);
            updateVolumeButton(newVolume);
        }
    }
    
    private void showSpeedOptions() {
        // TODO: Show speed selection dialog
        Toast.makeText(getContext(), "Speed control - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void showFormatOptions() {
        // TODO: Show format selection dialog
        Toast.makeText(getContext(), "Format selection - Coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void toggleFileManager() {
        isFileManagerVisible = !isFileManagerVisible;
        updateFileManagerButton();
        
        if (fileManagerToggleListener != null) {
            fileManagerToggleListener.onFileManagerToggle(isFileManagerVisible);
        }
    }
    
    private void toggleControls() {
        updateControlsVisibility(!isControlsVisible);
    }
    
    private void onVideoClick() {
        if (videoClickListener != null) {
            videoClickListener.onVideoClick();
        }
    }
    
    // UI update methods
    
    private void updatePlaybackState() {
        if (media3ExoPlayer == null) return;
        
        boolean isPlaying = media3ExoPlayer.isPlaying();
        int resId = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        playPauseButton.setImageResource(resId);
        
        // Update format button based on current item
        updateFormatButton();
    }
    
    private void updateSeekBar(long position, long duration) {
        if (duration > 0) {
            int progress = (int) ((position * 1000) / duration);
            seekBar.setProgress(progress);
        }
        
        currentTimeText.setText(formatTime(position));
        durationText.setText(formatTime(duration));
    }
    
    private void updateLoadingState(boolean isLoading) {
        loadingProgress.setVisibility(isLoading ? VISIBLE : GONE);
        videoSurface.setVisibility(isLoading ? INVISIBLE : VISIBLE);
    }
    
    private void updateModeDisplay() {
        if (isAudioOnlyMode) {
            // Show audio info, hide video surface
            videoSurface.setVisibility(GONE);
            thumbnailView.setVisibility(GONE);
            audioInfoContainer.setVisibility(VISIBLE);
        } else {
            // Show video surface, hide audio info
            videoSurface.setVisibility(VISIBLE);
            thumbnailView.setVisibility(VISIBLE);
            audioInfoContainer.setVisibility(GONE);
        }
    }
    
    private void updateVolumeButton(float volume) {
        if (volume > 0) {
            volumeButton.setImageResource(R.drawable.ic_volume_up);
            volumeButton.setAlpha(1.0f);
        } else {
            volumeButton.setImageResource(R.drawable.ic_volume_mute);
            volumeButton.setAlpha(0.5f);
        }
    }
    
    private void updateFormatButton() {
        if (media3ExoPlayer == null) return;
        
        MediaItem currentItem = media3ExoPlayer.getCurrentMediaItem();
        if (currentItem != null && currentItem.localConfiguration != null) {
            String mimeType = currentItem.localConfiguration.mimeType;
            if (mimeType != null) {
                if (mimeType.startsWith("audio/")) {
                    formatButton.setImageResource(R.drawable.ic_audio_file);
                } else if (mimeType.startsWith("video/")) {
                    formatButton.setImageResource(R.drawable.ic_video_file);
                } else {
                    formatButton.setImageResource(R.drawable.ic_file);
                }
            }
        }
    }
    
    private void updateFileManagerButton() {
        int resId = isFileManagerVisible ? R.drawable.ic_folder_open : R.drawable.ic_folder_closed;
        fileManagerButton.setImageResource(resId);
    }
    
    private void updateMediaInfo() {
        if (media3ExoPlayer == null) return;
        
        MediaItem currentItem = media3ExoPlayer.getCurrentMediaItem();
        if (currentItem != null) {
            MediaMetadata metadata = currentItem.mediaMetadata;
            
            if (metadata != null) {
                titleText.setText(metadata.title != null ? 
                                metadata.title.toString() : "Unknown Title");
                artistText.setText(metadata.artist != null ? 
                                 metadata.artist.toString() : "Unknown Artist");
            } else {
                titleText.setText("Unknown Title");
                artistText.setText("Unknown Artist");
            }
        }
    }
    
    private void updateControlsVisibility(boolean visible) {
        isControlsVisible = visible;
        
        if (fadeAnimator != null) {
            fadeAnimator.cancel();
        }
        
        float targetAlpha = visible ? 1.0f : 0.0f;
        fadeAnimator = ValueAnimator.ofFloat(controlsContainer.getAlpha(), targetAlpha);
        fadeAnimator.setDuration(300);
        fadeAnimator.addUpdateListener(animator -> {
            float alpha = (float) animator.getAnimatedValue();
            controlsContainer.setAlpha(alpha);
        });
        fadeAnimator.start();
        
        if (visible) {
            scheduleControlsHide();
        }
    }
    
    private void scheduleControlsHide() {
        controlsHandler.removeCallbacksAndMessages(null);
        controlsHandler.postDelayed(this::hideControls, HIDE_CONTROLS_DELAY);
    }
    
    private void hideControls() {
        if (isControlsVisible && media3ExoPlayer != null && media3ExoPlayer.isPlaying()) {
            updateControlsVisibility(false);
        }
    }
    
    // Touch handling
    
    private boolean handleVideoSurfaceTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getRawX();
                lastTouchY = event.getRawY();
                return false; // Allow default video controls
                
            case MotionEvent.ACTION_MOVE:
                // Don't intercept moves for now
                return false;
                
            case MotionEvent.ACTION_UP:
                // Single tap to show controls
                if (Math.abs(event.getRawX() - lastTouchX) < 10 && 
                    Math.abs(event.getRawY() - lastTouchY) < 10) {
                    toggleControls();
                }
                return false;
        }
        return false;
    }
    
    private boolean handleGlobalTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isAudioOnlyMode) {
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    isMoving = false;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isAudioOnlyMode && (getParent() != null)) {
                    float deltaX = event.getRawX() - lastTouchX;
                    float deltaY = event.getRawY() - lastTouchY;
                    
                    if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                        isMoving = true;
                        // TODO: Implement window dragging logic
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
                if (isAudioOnlyMode && isMoving) {
                    // Handle drop position
                    isMoving = false;
                    return true;
                } else if (isAudioOnlyMode && !isMoving) {
                    toggleControls();
                }
                break;
        }
        return false;
    }
    
    // Utility methods
    
    private String formatTime(long milliseconds) {
        if (milliseconds == C.TIME_UNSET) {
            return "0:00";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }
    
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
    
    // Public methods
    
    public void setOnFileManagerToggleListener(OnFileManagerToggleListener listener) {
        this.fileManagerToggleListener = listener;
    }
    
    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.videoClickListener = listener;
    }
    
    public void showControlsTemporary() {
        updateControlsVisibility(true);
        scheduleControlsHide();
    }
    
    public boolean isControlsVisible() {
        return isControlsVisible;
    }
    
    public boolean isAudioOnlyMode() {
        return isAudioOnlyMode;
    }
    
    public void updateWindowLayout(int width, int height) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) {
            params.width = width;
            params.height = height;
            setLayoutParams(params);
        }
    }
    
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        
        if (visibility == VISIBLE) {
            if (media3ExoPlayer != null) {
                startPositionUpdates();
            }
        } else {
            stopPositionUpdates();
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPositionUpdates();
        controlsHandler.removeCallbacksAndMessages(null);
    }
}
