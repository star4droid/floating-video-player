package com.floatingvideoplayer.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.floatingvideoplayer.R;

/**
 * Window controls for minimizing, maximizing, and closing floating windows
 */
public class WindowControls {
    
    private static final String TAG = "WindowControls";
    
    private static final int CONTROLS_WIDTH = 200;
    private static final int CONTROLS_HEIGHT = 60;
    
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View controlsView;
    
    // Window state
    private boolean isVisible = false;
    private boolean isDragging = false;
    
    // Drag variables
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    // Control callbacks
    private OnControlsActionListener actionListener;
    
    // Control buttons
    private ImageButton videoPlayerBtn;
    private ImageButton fileManagerBtn;
    private ImageButton settingsBtn;
    private ImageButton closeAllBtn;
    
    /**
     * Interface for control action callbacks
     */
    public interface OnControlsActionListener {
        void onShowVideoPlayer();
        void onShowFileManager();
        void onSettings();
        void onCloseAll();
        void onToggleOverlay();
    }
    
    public WindowControls(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    
    /**
     * Set action listener for control buttons
     */
    public void setOnControlsActionListener(OnControlsActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * Create and show the floating controls window
     */
    public void show() {
        if (isVisible) {
            hide();
            return;
        }
        
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            controlsView = inflater.inflate(R.layout.floating_controls_layout, null);
            
            // Initialize window parameters
            layoutParams = createWindowLayoutParams();
            
            // Initialize views
            initializeViews();
            setupControls();
            setupTouchListeners();
            
            // Add to window manager
            windowManager.addView(controlsView, layoutParams);
            isVisible = true;
            
            Log.d(TAG, "Floating controls shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing floating controls", e);
            Toast.makeText(context, "Failed to show controls", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hide the floating controls window
     */
    public void hide() {
        if (!isVisible || controlsView == null) return;
        
        try {
            windowManager.removeView(controlsView);
            controlsView = null;
            isVisible = false;
            
            Log.d(TAG, "Floating controls hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding floating controls", e);
        }
    }
    
    /**
     * Create window layout parameters for floating controls
     */
    private WindowManager.LayoutParams createWindowLayoutParams() {
        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                CONTROLS_WIDTH,
                CONTROLS_HEIGHT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 100;
        
        return params;
    }
    
    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Find control buttons
        videoPlayerBtn = controlsView.findViewById(R.id.btn_controls_video_player);
        fileManagerBtn = controlsView.findViewById(R.id.btn_controls_file_manager);
        settingsBtn = controlsView.findViewById(R.id.btn_controls_settings);
        closeAllBtn = controlsView.findViewById(R.id.btn_controls_close_all);
        
        Log.d(TAG, "Views initialized successfully");
    }
    
    /**
     * Setup control button listeners
     */
    private void setupControls() {
        // Video player button
        if (videoPlayerBtn != null) {
            videoPlayerBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onShowVideoPlayer();
                }
            });
        }
        
        // File manager button
        if (fileManagerBtn != null) {
            fileManagerBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onShowFileManager();
                }
            });
        }
        
        // Settings button
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onSettings();
                }
            });
        }
        
        // Close all button
        if (closeAllBtn != null) {
            closeAllBtn.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCloseAll();
                }
            });
        }
        
        Log.d(TAG, "Controls setup complete");
    }
    
    /**
     * Setup touch listeners for window dragging
     */
    private void setupTouchListeners() {
        controlsView.setOnTouchListener((view, event) -> {
            return handleDragTouchEvent(event);
        });
    }
    
    /**
     * Handle drag touch events
     */
    private boolean handleDragTouchEvent(MotionEvent event) {
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
                    
                    windowManager.updateViewLayout(controlsView, layoutParams);
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
     * Constrain window position to screen bounds
     */
    private void constrainWindowPosition() {
        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        
        // Ensure window stays within screen bounds
        layoutParams.x = Math.max(0, Math.min(layoutParams.x, screenSize.x - CONTROLS_WIDTH));
        layoutParams.y = Math.max(0, Math.min(layoutParams.y, screenSize.y - CONTROLS_HEIGHT));
    }
    
    /**
     * Update button states based on window visibility
     */
    public void updateButtonStates(boolean videoPlayerVisible, boolean fileManagerVisible) {
        if (controlsView == null) return;
        
        // Update video player button state
        if (videoPlayerBtn != null) {
            videoPlayerBtn.setAlpha(videoPlayerVisible ? 1.0f : 0.6f);
        }
        
        // Update file manager button state
        if (fileManagerBtn != null) {
            fileManagerBtn.setAlpha(fileManagerVisible ? 1.0f : 0.6f);
        }
    }
    
    /**
     * Show quick actions popup
     */
    public void showQuickActions() {
        if (!isVisible) {
            show();
        }
        // TODO: Implement quick actions menu
    }
    
    /**
     * Check if controls are currently visible
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Get current window position and size
     */
    public int[] getWindowBounds() {
        return new int[]{layoutParams.x, layoutParams.y, CONTROLS_WIDTH, CONTROLS_HEIGHT};
    }
    
    /**
     * Set window position and size
     */
    public void setWindowBounds(int x, int y, int width, int height) {
        if (layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
            
            if (isVisible && controlsView != null) {
                windowManager.updateViewLayout(controlsView, layoutParams);
            }
        }
    }
    
    /**
     * Auto-hide controls after delay
     */
    public void autoHide(long delayMillis) {
        if (controlsView != null) {
            controlsView.postDelayed(() -> {
                if (isVisible) {
                    hide();
                }
            }, delayMillis);
        }
    }
    
    /**
     * Show controls at specific position
     */
    public void showAtPosition(int x, int y) {
        if (!isVisible) {
            layoutParams.x = x;
            layoutParams.y = y;
            show();
        }
    }
    
    /**
     * Snap controls to edge of screen
     */
    public void snapToEdge(int edge) {
        if (!isVisible) return;
        
        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        
        switch (edge) {
            case Gravity.TOP:
                layoutParams.y = 0;
                break;
            case Gravity.BOTTOM:
                layoutParams.y = screenSize.y - CONTROLS_HEIGHT;
                break;
            case Gravity.START:
                layoutParams.x = 0;
                break;
            case Gravity.END:
                layoutParams.x = screenSize.x - CONTROLS_WIDTH;
                break;
        }
        
        if (controlsView != null) {
            windowManager.updateViewLayout(controlsView, layoutParams);
        }
    }
    
    /**
     * Bring controls to front
     */
    public void bringToFront() {
        if (isVisible && controlsView != null) {
            try {
                windowManager.removeView(controlsView);
                windowManager.addView(controlsView, layoutParams);
            } catch (Exception e) {
                Log.e(TAG, "Error bringing controls to front", e);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        hide();
        actionListener = null;
    }
}