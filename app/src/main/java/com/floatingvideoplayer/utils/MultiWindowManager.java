package com.floatingvideoplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages multi-window compatibility for Android split-screen and picture-in-picture modes
 * Handles adaptive layouts and conflict resolution with other floating apps
 */
public class MultiWindowManager {
    
    private static final String TAG = "MultiWindowManager";
    
    // Multi-window states
    public enum MultiWindowState {
        UNKNOWN,
        NOT_SUPPORTED,
        FULLSCREEN,
        SPLIT_SCREEN_PRIMARY,
        SPLIT_SCREEN_SECONDARY,
        PICTURE_IN_PICTURE,
        FREE_FORM
    }
    
    // Screen orientation
    public enum Orientation {
        PORTRAIT,
        LANDSCAPE,
        UNKNOWN
    }
    
    /**
     * Multi-window configuration for a specific window
     */
    public static class MultiWindowConfig {
        public boolean supportsSplitScreen = true;
        public boolean supportsPictureInPicture = true;
        public boolean autoAdjustOnOrientation = true;
        public boolean minimizeOnBackground = false;
        public int minWidth = 200;
        public int minHeight = 150;
        public int preferredWidth = 400;
        public int preferredHeight = 300;
        public boolean allowOverlap = true;
        public boolean respectSystemConstraints = true;
        public boolean adaptiveLayout = true;
        public float maxOpacity = 1.0f;
        public boolean enableGestures = true;
    }
    
    /**
     * Screen information container
     */
    public static class ScreenInfo {
        public int width;
        public int height;
        public int availableWidth;
        public int availableHeight;
        public Orientation orientation;
        public boolean isTablet;
        public boolean hasSystemUI = true;
        public boolean inMultiWindow = false;
        public MultiWindowState multiWindowState = MultiWindowState.UNKNOWN;
        public int statusBarHeight = 0;
        public int navigationBarHeight = 0;
        public float density = 1.0f;
        
        public ScreenInfo(int width, int height, Orientation orientation) {
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.availableWidth = width;
            this.availableHeight = height;
        }
        
        public boolean isLandscape() {
            return orientation == Orientation.LANDSCAPE;
        }
        
        public boolean isPortrait() {
            return orientation == Orientation.PORTRAIT;
        }
        
        public int getSmallestDimension() {
            return Math.min(width, height);
        }
        
        public int getLargestDimension() {
            return Math.max(width, height);
        }
    }
    
    private Context context;
    private WindowManager windowManager;
    private WindowStateManager windowStateManager;
    private ScreenInfo currentScreenInfo;
    private Map<String, MultiWindowConfig> windowConfigs;
    private boolean isInMultiWindow = false;
    private boolean isInPictureInPicture = false;
    private MultiWindowState currentMultiWindowState = MultiWindowState.UNKNOWN;
    
    // Multi-window callbacks
    public interface MultiWindowCallback {
        void onMultiWindowStateChanged(MultiWindowState newState, MultiWindowState oldState);
        void onOrientationChanged(Orientation newOrientation);
        void onScreenSizeChanged(int width, int height);
        void onPictureInPictureModeChanged(boolean isInPictureInPicture);
        void onConfigurationChanged(Configuration newConfig);
    }
    
    private MultiWindowCallback multiWindowCallback;
    
    public MultiWindowManager(Context context, WindowStateManager windowStateManager) {
        this.context = context;
        this.windowStateManager = windowStateManager;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.windowConfigs = new HashMap<>();
        
        updateScreenInfo();
        
        Log.d(TAG, "MultiWindowManager initialized. SDK: " + Build.VERSION.SDK_INT + 
                  ", Multi-window supported: " + isMultiWindowSupported());
    }
    
    /**
     * Check if multi-window is supported on this device
     */
    public boolean isMultiWindowSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false; // Multi-window introduced in API 24
        }
        
        // Check if device manufacturer disabled multi-window (some do)
        try {
            android.content.res.Configuration config = context.getResources().getConfiguration();
            return config.isScreenSizeCompat() || config.isLayoutSizeAtLeast(Configuration.SCREEN_SIZE_XLARGE);
        } catch (Exception e) {
            Log.w(TAG, "Error checking multi-window support", e);
            return true; // Assume supported if we can't check
        }
    }
    
    /**
     * Check if picture-in-picture is supported
     */
    public boolean isPictureInPictureSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false; // PIP introduced in API 26
        }
        
        try {
            Activity activity = (Activity) context;
            return activity.getPackageManager().hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
            );
        } catch (Exception e) {
            Log.w(TAG, "Error checking PIP support", e);
            return true; // Assume supported if we can't check
        }
    }
    
    /**
     * Check if device is in multi-window mode
     */
    public boolean isInMultiWindowMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Configuration config = context.getResources().getConfiguration();
            return config.isScreenSizeCompat();
        }
        return false;
    }
    
    /**
     * Check if device is in picture-in-picture mode
     */
    public boolean isInPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Activity activity = (Activity) context;
                return activity.isInPictureInPictureMode();
            } catch (Exception e) {
                Log.w(TAG, "Error checking PIP mode", e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Get current screen information
     */
    public ScreenInfo getCurrentScreenInfo() {
        updateScreenInfo();
        return currentScreenInfo;
    }
    
    /**
     * Update screen information
     */
    private void updateScreenInfo() {
        if (windowManager == null) return;
        
        try {
            android.view.Display display = windowManager.getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            
            Orientation orientation = (size.x > size.y) ? Orientation.LANDSCAPE : Orientation.PORTRAIT;
            ScreenInfo newScreenInfo = new ScreenInfo(size.x, size.y, orientation);
            
            // Check if screen info changed
            boolean screenChanged = currentScreenInfo == null || 
                currentScreenInfo.width != newScreenInfo.width || 
                currentScreenInfo.height != newScreenInfo.height ||
                currentScreenInfo.orientation != newScreenInfo.orientation;
            
            if (screenChanged && multiWindowCallback != null) {
                multiWindowCallback.onScreenSizeChanged(size.x, size.y);
                if (currentScreenInfo != null && currentScreenInfo.orientation != newScreenInfo.orientation) {
                    multiWindowCallback.onOrientationChanged(newScreenInfo.orientation);
                }
            }
            
            // Update multi-window states
            boolean wasInMultiWindow = isInMultiWindow;
            boolean wasInPIP = isInPictureInPicture;
            MultiWindowState oldState = currentMultiWindowState;
            
            isInMultiWindow = isInMultiWindowMode();
            isInPictureInPicture = isInPictureInPictureMode();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentMultiWindowState = calculateMultiWindowState();
            } else {
                currentMultiWindowState = MultiWindowState.FULLSCREEN;
            }
            
            if (oldState != currentMultiWindowState && multiWindowCallback != null) {
                multiWindowCallback.onMultiWindowStateChanged(currentMultiWindowState, oldState);
            }
            
            if (wasInPIP != isInPictureInPicture && multiWindowCallback != null) {
                multiWindowCallback.onPictureInPictureModeChanged(isInPictureInPicture);
            }
            
            // Adjust available dimensions for multi-window
            if (isInMultiWindow) {
                adjustDimensionsForMultiWindow(newScreenInfo);
            }
            
            this.currentScreenInfo = newScreenInfo;
            isInMultiWindow = wasInMultiWindow; // Restore original value
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating screen info", e);
            // Fallback to basic screen info
            this.currentScreenInfo = new ScreenInfo(1920, 1080, Orientation.LANDSCAPE);
        }
    }
    
    /**
     * Calculate current multi-window state
     */
    private MultiWindowState calculateMultiWindowState() {
        if (isInPictureInPictureMode()) {
            return MultiWindowState.PICTURE_IN_PICTURE;
        }
        
        if (isInMultiWindowMode()) {
            // This is a simplified check - in practice you'd need more complex logic
            // to determine if you're primary or secondary in split-screen
            return MultiWindowState.SPLIT_SCREEN_PRIMARY;
        }
        
        return MultiWindowState.FULLSCREEN;
    }
    
    /**
     * Adjust window dimensions for multi-window mode
     */
    public void adjustDimensionsForMultiWindow(ScreenInfo screenInfo) {
        if (screenInfo == null) return;
        
        // In multi-window mode, reduce available space
        int statusBarHeight = getStatusBarHeight();
        int navigationBarHeight = getNavigationBarHeight();
        
        screenInfo.availableHeight = screenInfo.height - statusBarHeight - navigationBarHeight;
        screenInfo.availableWidth = screenInfo.width;
        
        // If in split screen, further reduce available space
        if (currentMultiWindowState == MultiWindowState.SPLIT_SCREEN_PRIMARY ||
            currentMultiWindowState == MultiWindowState.SPLIT_SCREEN_SECONDARY) {
            // Split screen typically halves the available space
            if (screenInfo.orientation == Orientation.PORTRAIT) {
                screenInfo.availableHeight = screenInfo.availableHeight / 2;
            } else {
                screenInfo.availableWidth = screenInfo.availableWidth / 2;
            }
        }
        
        Log.d(TAG, "Adjusted for multi-window: " + screenInfo.availableWidth + "x" + screenInfo.availableHeight);
    }
    
    /**
     * Get status bar height
     */
    private int getStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    
    /**
     * Get navigation bar height
     */
    private int getNavigationBarHeight() {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
    
    /**
     * Calculate optimal window size for current multi-window state
     */
    public int[] calculateOptimalWindowSize(String windowId) {
        updateScreenInfo();
        MultiWindowConfig config = getWindowConfig(windowId);
        
        int optimalWidth = config.preferredWidth;
        int optimalHeight = config.preferredHeight;
        
        // Adjust based on screen info
        if (currentScreenInfo != null) {
            int maxWidth = currentScreenInfo.availableWidth - 50; // Leave some margin
            int maxHeight = currentScreenInfo.availableHeight - 50;
            
            optimalWidth = Math.min(optimalWidth, maxWidth);
            optimalHeight = Math.min(optimalHeight, maxHeight);
            
            // Further adjust for multi-window
            if (isInMultiWindow || isInPictureInPicture) {
                optimalWidth = Math.max(config.minWidth, optimalWidth / 2);
                optimalHeight = Math.max(config.minHeight, optimalHeight / 2);
            }
            
            // Ensure minimum size
            optimalWidth = Math.max(optimalWidth, config.minWidth);
            optimalHeight = Math.max(optimalHeight, config.minHeight);
        }
        
        return new int[]{optimalWidth, optimalHeight};
    }
    
    /**
     * Calculate optimal window position
     */
    public int[] calculateOptimalWindowPosition(String windowId) {
        updateScreenInfo();
        int[] optimalPosition = new int[2]; // x, y
        
        if (currentScreenInfo == null) {
            optimalPosition[0] = 100;
            optimalPosition[1] = 100;
            return optimalPosition;
        }
        
        // Default position
        int margin = 50;
        optimalPosition[0] = margin;
        optimalPosition[1] = margin;
        
        // Adjust for multi-window state
        switch (currentMultiWindowState) {
            case SPLIT_SCREEN_PRIMARY:
                if (currentScreenInfo.isPortrait()) {
                    // In portrait split screen, primary is typically at top
                    optimalPosition[1] = margin;
                } else {
                    // In landscape, primary is typically on left
                    optimalPosition[0] = margin;
                }
                break;
                
            case SPLIT_SCREEN_SECONDARY:
                if (currentScreenInfo.isPortrait()) {
                    // Secondary is at bottom in portrait
                    optimalPosition[1] = currentScreenInfo.availableHeight - margin - 200;
                } else {
                    // Secondary is on right in landscape
                    optimalPosition[0] = currentScreenInfo.availableWidth - margin - 200;
                }
                break;
                
            case PICTURE_IN_PICTURE:
                // PIP typically appears in bottom corner
                optimalPosition[0] = currentScreenInfo.availableWidth - 250;
                optimalPosition[1] = currentScreenInfo.availableHeight - 250;
                break;
                
            default:
                // Fullscreen - position based on window type
                String windowType = getWindowType(windowId);
                if ("player".equals(windowType)) {
                    // Player on left/bottom
                    optimalPosition[0] = margin;
                    optimalPosition[1] = currentScreenInfo.availableHeight - margin - 300;
                } else if ("file_manager".equals(windowType)) {
                    // File manager on right/top
                    optimalPosition[0] = currentScreenInfo.availableWidth - margin - 400;
                    optimalPosition[1] = margin;
                }
                break;
        }
        
        return optimalPosition;
    }
    
    /**
     * Get window type from window ID
     */
    private String getWindowType(String windowId) {
        if (windowId != null) {
            if (windowId.contains("player")) return "player";
            if (windowId.contains("file") || windowId.contains("manager")) return "file_manager";
        }
        return "unknown";
    }
    
    /**
     * Configure window for multi-window compatibility
     */
    public void configureWindowForMultiWindow(String windowId, WindowManager.LayoutParams params) {
        if (params == null) return;
        
        MultiWindowConfig config = getWindowConfig(windowId);
        
        // Set window flags for multi-window compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable multi-window support
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        }
        
        // Adjust alpha for multi-window
        if (isInMultiWindow || isInPictureInPicture) {
            params.alpha = Math.min(params.alpha, config.maxOpacity);
        }
        
        Log.d(TAG, "Configured window for multi-window: " + windowId + 
                  ", State: " + currentMultiWindowState);
    }
    
    /**
     * Handle configuration changes
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "Configuration changed");
        
        boolean orientationChanged = currentScreenInfo != null && 
            ((newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) != 
             currentScreenInfo.isLandscape());
        
        updateScreenInfo();
        
        if (multiWindowCallback != null) {
            multiWindowCallback.onConfigurationChanged(newConfig);
            
            if (orientationChanged) {
                Orientation newOrientation = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) ? 
                    Orientation.LANDSCAPE : Orientation.PORTRAIT;
                multiWindowCallback.onOrientationChanged(newOrientation);
            }
        }
    }
    
    /**
     * Check for conflicts with other floating apps
     */
    public List<String> detectWindowConflicts(String windowId, int x, int y, int width, int height) {
        List<String> conflicts = new ArrayList<>();
        
        // This would require integration with other apps' window information
        // For now, just check against screen bounds
        if (currentScreenInfo != null) {
            if (x < 0 || y < 0 || 
                x + width > currentScreenInfo.availableWidth || 
                y + height > currentScreenInfo.availableHeight) {
                conflicts.add("Screen bounds exceeded");
            }
        }
        
        return conflicts;
    }
    
    /**
     * Suggest alternative position to avoid conflicts
     */
    public int[] suggestAlternativePosition(String windowId, int requestedX, int requestedY, int width, int height) {
        int[] alternative = new int[]{requestedX, requestedY};
        
        List<String> conflicts = detectWindowConflicts(windowId, requestedX, requestedY, width, height);
        if (conflicts.isEmpty()) {
            return alternative; // No conflicts
        }
        
        // Simple conflict resolution: move to next available position
        int margin = 50;
        for (int attempt = 0; attempt < 10; attempt++) {
            alternative[0] = margin + (attempt * 60);
            alternative[1] = margin + (attempt * 60);
            
            if (currentScreenInfo != null) {
                if (alternative[0] + width <= currentScreenInfo.availableWidth &&
                    alternative[1] + height <= currentScreenInfo.availableHeight) {
                    conflicts = detectWindowConflicts(windowId, alternative[0], alternative[1], width, height);
                    if (conflicts.isEmpty()) {
                        Log.d(TAG, "Found alternative position: (" + alternative[0] + ", " + alternative[1] + ")");
                        return alternative;
                    }
                }
            }
        }
        
        Log.w(TAG, "Could not find conflict-free position");
        return alternative;
    }
    
    /**
     * Minimize window when entering background (for multi-window compatibility)
     */
    public void minimizeOnBackground(String windowId) {
        MultiWindowConfig config = getWindowConfig(windowId);
        if (config.minimizeOnBackground) {
            Log.d(TAG, "Minimizing window on background: " + windowId);
            // Implementation would depend on your window management system
            // This might involve reducing size, changing alpha, or hiding temporarily
        }
    }
    
    /**
     * Restore window when coming to foreground
     */
    public void restoreOnForeground(String windowId) {
        Log.d(TAG, "Restoring window on foreground: " + windowId);
        // Implementation would restore the window to its previous state
    }
    
    /**
     * Get or create window configuration
     */
    public MultiWindowConfig getWindowConfig(String windowId) {
        if (!windowConfigs.containsKey(windowId)) {
            windowConfigs.put(windowId, createDefaultWindowConfig(windowId));
        }
        return windowConfigs.get(windowId);
    }
    
    /**
     * Create default window configuration
     */
    private MultiWindowConfig createDefaultWindowConfig(String windowId) {
        MultiWindowConfig config = new MultiWindowConfig();
        
        // Default sizes based on window type
        if (windowId.contains("player")) {
            config.preferredWidth = 400;
            config.preferredHeight = 300;
            config.minWidth = 200;
            config.minHeight = 150;
        } else if (windowId.contains("file") || windowId.contains("manager")) {
            config.preferredWidth = 500;
            config.preferredHeight = 400;
            config.minWidth = 300;
            config.minHeight = 200;
        }
        
        return config;
    }
    
    /**
     * Set multi-window callback
     */
    public void setMultiWindowCallback(MultiWindowCallback callback) {
        this.multiWindowCallback = callback;
    }
    
    /**
     * Get current multi-window state
     */
    public MultiWindowState getCurrentMultiWindowState() {
        return currentMultiWindowState;
    }
    
    /**
     * Check if currently in multi-window mode
     */
    public boolean isCurrentlyInMultiWindow() {
        return isInMultiWindow;
    }
    
    /**
     * Check if currently in picture-in-picture mode
     */
    public boolean isCurrentlyInPictureInPicture() {
        return isInPictureInPicture;
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        windowConfigs.clear();
        currentScreenInfo = null;
        multiWindowCallback = null;
    }
}