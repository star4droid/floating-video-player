package com.floatingvideoplayer.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.floatingvideoplayer.R;

/**
 * Draggable and resizable video player window with ExoPlayer integration
 */
public class DraggableVideoPlayerWindow {
    
    private static final String TAG = "DraggableVideoPlayerWindow";
    
    private static final int MIN_WINDOW_WIDTH = 200;
    private static final int MIN_WINDOW_HEIGHT = 150;
    private static final int MAX_WINDOW_WIDTH = 800;
    private static final int MAX_WINDOW_HEIGHT = 600;
    
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View playerView;
    private ExoPlayer exoPlayer;
    
    // Window state
    private boolean isVisible = false;
    private boolean isDragging = false;
    private boolean isResizing = false;
    
    // Drag and resize variables
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private int resizeCorner = -1; // 0-7 for different corners/sides
    
    // Resize handles
    private static final int CORNER_TOP_LEFT = 0;
    private static final int CORNER_TOP_RIGHT = 1;
    private static final int CORNER_BOTTOM_LEFT = 2;
    private static final int CORNER_BOTTOM_RIGHT = 3;
    private static final int SIDE_LEFT = 4;
    private static final int SIDE_RIGHT = 5;
    private static final int SIDE_TOP = 6;
    private static final int SIDE_BOTTOM = 7;
    
    // Media3 Player components
    private PlayerView exoPlayerView;
    private SurfaceView videoSurface;
    private View videoPlaceholder;
    private ProgressBar loadingIndicator;
    private ImageButton playPauseBtn;
    private ImageButton volumeBtn;
    private ImageButton settingsBtn;
    private ImageButton fullscreenBtn;
    private ImageButton minimizeBtn;
    private ImageButton closeBtn;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView durationText;
    
    // Control buttons
    private FrameLayout resizeHandleTopLeft;
    private FrameLayout resizeHandleTopRight;
    private FrameLayout resizeHandleBottomLeft;
    private FrameLayout resizeHandleBottomRight;
    private View resizeHandleLeft;
    private View resizeHandleRight;
    private View resizeHandleTop;
    private View resizeHandleBottom;
    
    public DraggableVideoPlayerWindow(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initializePlayer();
    }
    
    /**
     * Initialize ExoPlayer with Media3
     */
    private void initializePlayer() {
        try {
            exoPlayer = new ExoPlayer.Builder(context).build();
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    handlePlaybackStateChange(playbackState);
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePlayPauseButton(isPlaying);
                }
            });
            
            Log.d(TAG, "ExoPlayer initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ExoPlayer", e);
            Toast.makeText(context, "Failed to initialize video player", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Create and show the floating video player window
     */
    public void show() {
        if (isVisible) {
            hide();
            return;
        }
        
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            playerView = inflater.inflate(R.layout.floating_player_layout, null);
            
            // Initialize window parameters
            layoutParams = createWindowLayoutParams();
            
            // Find and setup views
            initializeViews();
            setupControls();
            setupResizeHandles();
            setupTouchListeners();
            
            // Add to window manager
            windowManager.addView(playerView, layoutParams);
            isVisible = true;
            
            Log.d(TAG, "Draggable video player window shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing video player window", e);
            Toast.makeText(context, "Failed to show video player", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hide the floating video player window
     */
    public void hide() {
        if (!isVisible || playerView == null) return;
        
        try {
            // Release player resources
            if (exoPlayer != null) {
                exoPlayer.release();
            }
            
            // Remove from window manager
            windowManager.removeView(playerView);
            playerView = null;
            isVisible = false;
            
            Log.d(TAG, "Draggable video player window hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding video player window", e);
        }
    }
    
    /**
     * Create window layout parameters for draggable/resizable window
     */
    private WindowManager.LayoutParams createWindowLayoutParams() {
        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                400, // Default width
                300, // Default height
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;
        
        return params;
    }
    
    /**
     * Initialize all views and Media3 components
     */
    private void initializeViews() {
        // Main views
        exoPlayerView = playerView.findViewById(R.id.video_surface);
        videoPlaceholder = playerView.findViewById(R.id.video_placeholder);
        loadingIndicator = playerView.findViewById(R.id.progress_loading);
        
        // Control buttons
        playPauseBtn = playerView.findViewById(R.id.btn_play_pause);
        volumeBtn = playerView.findViewById(R.id.btn_volume);
        settingsBtn = playerView.findViewById(R.id.btn_settings);
        fullscreenBtn = playerView.findViewById(R.id.btn_fullscreen);
        minimizeBtn = playerView.findViewById(R.id.btn_minimize_player);
        closeBtn = playerView.findViewById(R.id.btn_close_player);
        
        // Progress controls
        seekBar = playerView.findViewById(R.id.seek_bar);
        currentTimeText = playerView.findViewById(R.id.tv_current_time);
        durationText = playerView.findViewById(R.id.tv_duration);
        
        // Resize handles
        resizeHandleTopLeft = playerView.findViewById(R.id.resize_handle_top_left);
        resizeHandleTopRight = playerView.findViewById(R.id.resize_handle_top_right);
        resizeHandleBottomLeft = playerView.findViewById(R.id.resize_handle_bottom_left);
        resizeHandleBottomRight = playerView.findViewById(R.id.resize_handle_bottom_right);
        resizeHandleLeft = playerView.findViewById(R.id.resize_handle_left);
        resizeHandleRight = playerView.findViewById(R.id.resize_handle_right);
        resizeHandleTop = playerView.findViewById(R.id.resize_handle_top);
        resizeHandleBottom = playerView.findViewById(R.id.resize_handle_bottom);
        
        // Setup ExoPlayer with PlayerView
        if (exoPlayerView != null) {
            exoPlayerView.setPlayer(exoPlayer);
            exoPlayerView.setUseController(true);
            exoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        }
        
        Log.d(TAG, "Views initialized successfully");
    }
    
    /**
     * Setup control button listeners
     */
    private void setupControls() {
        // Play/Pause button
        if (playPauseBtn != null) {
            playPauseBtn.setOnClickListener(v -> togglePlayPause());
        }
        
        // Volume button
        if (volumeBtn != null) {
            volumeBtn.setOnClickListener(v -> toggleVolume());
        }
        
        // Settings button
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> openSettings());
        }
        
        // Fullscreen button
        if (fullscreenBtn != null) {
            fullscreenBtn.setOnClickListener(v -> toggleFullscreen());
        }
        
        // Minimize button
        if (minimizeBtn != null) {
            minimizeBtn.setOnClickListener(v -> minimize());
        }
        
        // Close button
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> hide());
        }
        
        // Seek bar
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && exoPlayer != null && exoPlayer.getDuration() != C.TIME_UNSET) {
                        long newPosition = (long) (progress / 100.0f * exoPlayer.getDuration());
                        exoPlayer.seekTo(newPosition);
                    }
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        Log.d(TAG, "Controls setup complete");
    }
    
    /**
     * Setup resize handles for window resizing
     */
    private void setupResizeHandles() {
        View.OnTouchListener resizeTouchListener = (view, event) -> {
            handleResizeTouchEvent(event, (Integer) view.getTag());
            return true;
        };
        
        // Setup all resize handles
        if (resizeHandleTopLeft != null) {
            resizeHandleTopLeft.setTag(CORNER_TOP_LEFT);
            resizeHandleTopLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleTopRight != null) {
            resizeHandleTopRight.setTag(CORNER_TOP_RIGHT);
            resizeHandleTopRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottomLeft != null) {
            resizeHandleBottomLeft.setTag(CORNER_BOTTOM_LEFT);
            resizeHandleBottomLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottomRight != null) {
            resizeHandleBottomRight.setTag(CORNER_BOTTOM_RIGHT);
            resizeHandleBottomRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleLeft != null) {
            resizeHandleLeft.setTag(SIDE_LEFT);
            resizeHandleLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleRight != null) {
            resizeHandleRight.setTag(SIDE_RIGHT);
            resizeHandleRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleTop != null) {
            resizeHandleTop.setTag(SIDE_TOP);
            resizeHandleTop.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottom != null) {
            resizeHandleBottom.setTag(SIDE_BOTTOM);
            resizeHandleBottom.setOnTouchListener(resizeTouchListener);
        }
        
        Log.d(TAG, "Resize handles setup complete");
    }
    
    /**
     * Setup touch listeners for window dragging
     */
    private void setupTouchListeners() {
        playerView.setOnTouchListener((view, event) -> {
            return handleDragTouchEvent(event);
        });
    }
    
    /**
     * Handle drag touch events
     */
    private boolean handleDragTouchEvent(MotionEvent event) {
        if (isResizing) return true;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    
                    layoutParams.x = initialX + deltaX;
                    layoutParams.y = initialY + deltaY;
                    
                    // Constrain to screen bounds
                    constrainWindowPosition();
                    
                    windowManager.updateViewLayout(playerView, layoutParams);
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        
        return false;
    }
    
    /**
     * Handle resize touch events
     */
    private void handleResizeTouchEvent(MotionEvent event, int corner) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isResizing = true;
                resizeCorner = corner;
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    
                    applyResize(corner, deltaX, deltaY);
                    windowManager.updateViewLayout(playerView, layoutParams);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isResizing = false;
                resizeCorner = -1;
                break;
        }
    }
    
    /**
     * Apply resize based on corner/side being dragged
     */
    private void applyResize(int corner, int deltaX, int deltaY) {
        int newWidth = layoutParams.width;
        int newHeight = layoutParams.height;
        int newX = layoutParams.x;
        int newY = layoutParams.y;
        
        switch (corner) {
            case CORNER_TOP_LEFT:
                newWidth -= deltaX;
                newHeight -= deltaY;
                newX += deltaX;
                newY += deltaY;
                break;
                
            case CORNER_TOP_RIGHT:
                newWidth += deltaX;
                newHeight -= deltaY;
                newY += deltaY;
                break;
                
            case CORNER_BOTTOM_LEFT:
                newWidth -= deltaX;
                newHeight += deltaY;
                newX += deltaX;
                break;
                
            case CORNER_BOTTOM_RIGHT:
                newWidth += deltaX;
                newHeight += deltaY;
                break;
                
            case SIDE_LEFT:
                newWidth -= deltaX;
                newX += deltaX;
                break;
                
            case SIDE_RIGHT:
                newWidth += deltaX;
                break;
                
            case SIDE_TOP:
                newHeight -= deltaY;
                newY += deltaY;
                break;
                
            case SIDE_BOTTOM:
                newHeight += deltaY;
                break;
        }
        
        // Apply size constraints
        newWidth = Math.max(MIN_WINDOW_WIDTH, Math.min(newWidth, MAX_WINDOW_WIDTH));
        newHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(newHeight, MAX_WINDOW_HEIGHT));
        
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        layoutParams.x = newX;
        layoutParams.y = newY;
        
        constrainWindowPosition();
    }
    
    /**
     * Constrain window position to screen bounds
     */
    private void constrainWindowPosition() {
        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        
        // Ensure window stays within screen bounds
        layoutParams.x = Math.max(0, Math.min(layoutParams.x, screenSize.x - layoutParams.width));
        layoutParams.y = Math.max(0, Math.min(layoutParams.y, screenSize.y - layoutParams.height));
    }
    
    /**
     * Control methods
     */
    private void togglePlayPause() {
        if (exoPlayer != null) {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
            } else {
                exoPlayer.play();
            }
        }
    }
    
    private void toggleVolume() {
        if (exoPlayer != null) {
            float currentVolume = exoPlayer.getVolume();
            if (currentVolume > 0) {
                exoPlayer.setVolume(0f);
                if (volumeBtn != null) {
                    volumeBtn.setImageResource(R.drawable.ic_volume_off);
                }
            } else {
                exoPlayer.setVolume(1f);
                if (volumeBtn != null) {
                    volumeBtn.setImageResource(R.drawable.ic_volume_up);
                }
            }
        }
    }
    
    private void openSettings() {
        Toast.makeText(context, "Settings clicked", Toast.LENGTH_SHORT).show();
        // TODO: Implement settings dialog
    }
    
    private void toggleFullscreen() {
        Toast.makeText(context, "Fullscreen clicked", Toast.LENGTH_SHORT).show();
        // TODO: Implement fullscreen toggle
    }
    
    private void minimize() {
        hide();
        // TODO: Show minimized controls
    }
    
    /**
     * Media3 Player event handlers
     */
    private void handlePlaybackStateChange(int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                loadingIndicator.setVisibility(View.GONE);
                break;
            case Player.STATE_BUFFERING:
                loadingIndicator.setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                loadingIndicator.setVisibility(View.GONE);
                videoPlaceholder.setVisibility(View.GONE);
                if (exoPlayerView != null) {
                    exoPlayerView.setVisibility(View.VISIBLE);
                }
                break;
            case Player.STATE_ENDED:
                // Handle video end
                break;
        }
    }
    
    private void updatePlayPauseButton(boolean isPlaying) {
        if (playPauseBtn != null) {
            playPauseBtn.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
    
    /**
     * Load and play a video URL
     */
    public void loadVideo(String videoUrl) {
        if (exoPlayer == null) return;
        
        try {
            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
            
            Log.d(TAG, "Video loaded: " + videoUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error loading video", e);
            Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check if window is currently visible
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Get current window position and size
     */
    public int[] getWindowBounds() {
        return new int[]{layoutParams.x, layoutParams.y, layoutParams.width, layoutParams.height};
    }
    
    /**
     * Set window position and size
     */
    public void setWindowBounds(int x, int y, int width, int height) {
        if (layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
            layoutParams.width = width;
            layoutParams.height = height;
            
            if (isVisible && playerView != null) {
                windowManager.updateViewLayout(playerView, layoutParams);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        hide();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}