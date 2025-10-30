package com.floatingvideoplayer.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for window management operations with enhanced multi-window support
 */
public class WindowManagerHelper {
    
    private static final String TAG = "WindowManagerHelper";
    
    private Context context;
    private WindowManager windowManager;
    private Handler mainHandler;
    
    // Multi-window management
    private ConcurrentHashMap<String, WindowInfo> activeWindows;
    private int windowZOrder = 1000; // Starting z-order for overlays
    
    // Window types and constraints
    private static final int MIN_WINDOW_WIDTH = 200;
    private static final int MIN_WINDOW_HEIGHT = 150;
    private static final int MAX_WINDOW_WIDTH = 1000;
    private static final int MAX_WINDOW_HEIGHT = 1500;
    
    /**
     * Window information container
     */
    public static class WindowInfo {
        public String windowId;
        public View view;
        public WindowManager.LayoutParams params;
        public long createdTime;
        public boolean isDraggable;
        public boolean isResizable;
        public int minWidth;
        public int minHeight;
        public int maxWidth;
        public int maxHeight;
        
        public WindowInfo(String windowId, View view, WindowManager.LayoutParams params) {
            this.windowId = windowId;
            this.view = view;
            this.params = params;
            this.createdTime = System.currentTimeMillis();
            this.isDraggable = true;
            this.isResizable = true;
            this.minWidth = MIN_WINDOW_WIDTH;
            this.minHeight = MIN_WINDOW_HEIGHT;
            this.maxWidth = MAX_WINDOW_WIDTH;
            this.maxHeight = MAX_WINDOW_HEIGHT;
        }
    }
    
    public WindowManagerHelper(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.activeWindows = new ConcurrentHashMap<>();
    }
    
    /**
     * Get the appropriate window type based on Android version
     */
    public int getOverlayWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            return WindowManager.LayoutParams.TYPE_TOAST;
        }
    }
    
    /**
     * Create window layout parameters for overlay windows
     */
    public WindowManager.LayoutParams createOverlayLayoutParams(int width, int height, int gravity) {
        int windowType = getOverlayWindowType();
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = gravity;
        params.x = 50;
        params.y = 100;
        
        return params;
    }
    
    /**
     * Create draggable overlay layout parameters
     */
    public WindowManager.LayoutParams createDraggableOverlayLayoutParams(int width, int height, int gravity) {
        int windowType = getOverlayWindowType();
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = gravity;
        params.x = 50;
        params.y = 100;
        
        return params;
    }
    
    /**
     * Update window position
     */
    public void updateWindowPosition(WindowManager.LayoutParams params, int x, int y) {
        params.x = x;
        params.y = y;
        windowManager.updateViewLayout(null, params); // This would need the actual view
    }
    
    /**
     * Get current screen dimensions
     */
    public int[] getScreenDimensions() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        windowManager.getDefaultDisplay().getMetrics(params);
        
        return new int[] {
            windowManager.getDefaultDisplay().getWidth(),
            windowManager.getDefaultDisplay().getHeight()
        };
    }
    
    /**
     * Get display density
     */
    public float getDisplayDensity() {
        return context.getResources().getDisplayMetrics().density;
    }
    
    /**
     * Convert dp to pixels
     */
    public int dpToPx(int dp) {
        return Math.round(dp * getDisplayDensity());
    }
    
    /**
     * Convert pixels to dp
     */
    public int pxToDp(int px) {
        return Math.round(px / getDisplayDensity());
    }
    
    /**
     * Constrain window parameters to screen bounds
     */
    private void constrainToScreen(WindowManager.LayoutParams params) {
        int[] screenSize = getScreenDimensions();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        // Ensure window stays within screen bounds
        params.x = Math.max(0, Math.min(params.x, screenWidth - params.width));
        params.y = Math.max(0, Math.min(params.y, screenHeight - params.height));
    }
    
    /**
     * Get optimal window position based on screen size and existing windows
     */
    public int[] getOptimalWindowPosition(int width, int height) {
        int[] screenSize = getScreenDimensions();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        // Start with default position
        int x = 50;
        int y = 100;
        
        // Adjust if window would be too large for screen
        if (width > screenWidth) {
            x = 0;
            width = screenWidth;
        }
        
        if (height > screenHeight) {
            y = 0;
            height = screenHeight;
        }
        
        // Ensure position doesn't push window off screen
        x = Math.min(x, screenWidth - width);
        y = Math.min(y, screenHeight - height);
        
        return new int[]{x, y};
    }
    
    /**
     * Check if window position is valid
     */
    public boolean isValidWindowPosition(WindowManager.LayoutParams params) {
        if (params == null) return false;
        
        int[] screenSize = getScreenDimensions();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        return params.x >= 0 && params.y >= 0 && 
               params.x <= screenWidth - params.width && 
               params.y <= screenHeight - params.height;
    }
    
    /**
     * Get window position constraints for overlay
     */
    public int[] getOverlayConstraints() {
        int[] screenDimensions = getScreenDimensions();
        int screenWidth = screenDimensions[0];
        int screenHeight = screenDimensions[1];
        
        // Minimum and maximum x/y positions for overlays
        return new int[] {
            0, // minX
            0, // minY
            screenWidth, // maxX
            screenHeight, // maxY
            dpToPx(50) // min margin from edges
        };
    }
    
    /**
     * Create a frame layout for custom window content
     */
    public FrameLayout createWindowFrame() {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        return frameLayout;
    }
    
    /**
     * Validate if a window layout is properly configured
     */
    public boolean validateLayoutParams(WindowManager.LayoutParams params) {
        if (params == null) {
            return false;
        }
        
        if (params.width <= 0 || params.height <= 0) {
            Log.w(TAG, "Invalid window dimensions: " + params.width + "x" + params.height);
            return false;
        }
        
        if (params.type <= 0) {
            Log.w(TAG, "Invalid window type: " + params.type);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the most appropriate gravity for overlay positioning
     */
    public int getOptimalOverlayGravity() {
        int[] screenDimensions = getScreenDimensions();
        int screenWidth = screenDimensions[0];
        
        // For most apps, start with right-top for file manager
        // and left-bottom for video player
        return Gravity.TOP | Gravity.END;
    }
    
    // ==================== Multi-Window Management Methods ====================
    
    /**
     * Register a new window in the window manager
     */
    public boolean registerWindow(String windowId, View view, WindowManager.LayoutParams params) {
        try {
            if (activeWindows.containsKey(windowId)) {
                Log.w(TAG, "Window already registered: " + windowId);
                return false;
            }
            
            WindowInfo windowInfo = new WindowInfo(windowId, view, params);
            activeWindows.put(windowId, windowInfo);
            
            // Increment z-order for next window
            windowZOrder += 100;
            
            Log.d(TAG, "Window registered: " + windowId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error registering window", e);
            return false;
        }
    }
    
    /**
     * Unregister a window from the window manager
     */
    public boolean unregisterWindow(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.remove(windowId);
            if (windowInfo != null) {
                Log.d(TAG, "Window unregistered: " + windowId);
                return true;
            } else {
                Log.w(TAG, "Window not found for unregistration: " + windowId);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering window", e);
            return false;
        }
    }
    
    /**
     * Get all registered window IDs
     */
    public List<String> getRegisteredWindowIds() {
        return new ArrayList<>(activeWindows.keySet());
    }
    
    /**
     * Get window information by ID
     */
    public WindowInfo getWindowInfo(String windowId) {
        return activeWindows.get(windowId);
    }
    
    /**
     * Check if window is registered
     */
    public boolean isWindowRegistered(String windowId) {
        return activeWindows.containsKey(windowId);
    }
    
    /**
     * Update window position
     */
    public boolean updateWindowPosition(String windowId, int x, int y) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                windowInfo.params.x = x;
                windowInfo.params.y = y;
                windowManager.updateViewLayout(windowInfo.view, windowInfo.params);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error updating window position", e);
            return false;
        }
    }
    
    /**
     * Update window size
     */
    public boolean updateWindowSize(String windowId, int width, int height) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                // Apply size constraints
                width = Math.max(windowInfo.minWidth, Math.min(width, windowInfo.maxWidth));
                height = Math.max(windowInfo.minHeight, Math.min(height, windowInfo.maxHeight));
                
                windowInfo.params.width = width;
                windowInfo.params.height = height;
                windowManager.updateViewLayout(windowInfo.view, windowInfo.params);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error updating window size", e);
            return false;
        }
    }
    
    /**
     * Bring window to front
     */
    public boolean bringWindowToFront(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                // Increase z-order
                windowInfo.params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                windowManager.removeView(windowInfo.view);
                windowManager.addView(windowInfo.view, windowInfo.params);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error bringing window to front", e);
            return false;
        }
    }
    
    /**
     * Send window to back
     */
    public boolean sendWindowToBack(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                windowInfo.params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                windowManager.updateViewLayout(windowInfo.view, windowInfo.params);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error sending window to back", e);
            return false;
        }
    }
    
    /**
     * Hide window
     */
    public boolean hideWindow(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                windowInfo.view.setVisibility(View.GONE);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error hiding window", e);
            return false;
        }
    }
    
    /**
     * Show window
     */
    public boolean showWindow(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                windowInfo.view.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error showing window", e);
            return false;
        }
    }
    
    /**
     * Get all visible windows
     */
    public List<String> getVisibleWindows() {
        List<String> visibleWindows = new ArrayList<>();
        for (String windowId : activeWindows.keySet()) {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo != null && windowInfo.view != null && 
                windowInfo.view.getVisibility() == View.VISIBLE) {
                visibleWindows.add(windowId);
            }
        }
        return visibleWindows;
    }
    
    /**
     * Close all windows
     */
    public void closeAllWindows() {
        for (String windowId : new ArrayList<>(activeWindows.keySet())) {
            closeWindow(windowId);
        }
    }
    
    /**
     * Close specific window
     */
    public boolean closeWindow(String windowId) {
        try {
            WindowInfo windowInfo = activeWindows.remove(windowId);
            if (windowInfo != null && windowInfo.view != null) {
                windowManager.removeView(windowInfo.view);
                Log.d(TAG, "Window closed: " + windowId);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error closing window", e);
            return false;
        }
    }
    
    /**
     * Check for window conflicts and resolve them
     */
    public void resolveWindowConflicts() {
        // Implementation for conflict resolution
        // This could check overlapping windows and adjust their positions
        Log.d(TAG, "Resolving window conflicts...");
        
        List<String> windowIds = new ArrayList<>(activeWindows.keySet());
        if (windowIds.size() <= 1) return;
        
        // Simple conflict resolution: offset overlapping windows
        for (int i = 0; i < windowIds.size() - 1; i++) {
            String windowId1 = windowIds.get(i);
            String windowId2 = windowIds.get(i + 1);
            
            WindowInfo window1 = activeWindows.get(windowId1);
            WindowInfo window2 = activeWindows.get(windowId2);
            
            if (window1 != null && window2 != null) {
                if (areWindowsOverlapping(window1.params, window2.params)) {
                    // Offset the second window
                    window2.params.x += 50;
                    window2.params.y += 50;
                    
                    // Ensure it stays within screen bounds
                    constrainToScreen(window2.params);
                    
                    try {
                        windowManager.updateViewLayout(window2.view, window2.params);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating conflicting window", e);
                    }
                }
            }
        }
    }
    
    /**
     * Check if two windows overlap
     */
    private boolean areWindowsOverlapping(WindowManager.LayoutParams params1, 
                                         WindowManager.LayoutParams params2) {
        int[] screenSize = getScreenDimensions();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        // Calculate window bounds
        int left1 = Math.max(0, params1.x);
        int top1 = Math.max(0, params1.y);
        int right1 = Math.min(screenWidth, params1.x + params1.width);
        int bottom1 = Math.min(screenHeight, params1.y + params1.height);
        
        int left2 = Math.max(0, params2.x);
        int top2 = Math.max(0, params2.y);
        int right2 = Math.min(screenWidth, params2.x + params2.width);
        int bottom2 = Math.min(screenHeight, params2.y + params2.height);
        
        // Check for overlap
        return !(right1 <= left2 || right2 <= left1 || bottom1 <= top2 || bottom2 <= top1);
    }
    
    /**
     * Validate and clamp window position to screen bounds
     */
    public void constrainWindowToScreen(WindowManager.LayoutParams params) {
        if (params == null) return;
        
        int[] screenSize = getScreenDimensions();
        int screenWidth = screenSize[0];
        int screenHeight = screenSize[1];
        
        // Ensure window stays within screen bounds
        params.x = Math.max(0, Math.min(params.x, screenWidth - params.width));
        params.y = Math.max(0, Math.min(params.y, screenHeight - params.height));
    }
    
    /**
     * Validate and clamp window size within constraints
     */
    public void constrainWindowSize(WindowManager.LayoutParams params, WindowInfo windowInfo) {
        if (params == null) return;
        
        int minWidth = windowInfo != null ? windowInfo.minWidth : MIN_WINDOW_WIDTH;
        int minHeight = windowInfo != null ? windowInfo.minHeight : MIN_WINDOW_HEIGHT;
        int maxWidth = windowInfo != null ? windowInfo.maxWidth : MAX_WINDOW_WIDTH;
        int maxHeight = windowInfo != null ? windowInfo.maxHeight : MAX_WINDOW_HEIGHT;
        
        params.width = Math.max(minWidth, Math.min(params.width, maxWidth));
        params.height = Math.max(minHeight, Math.min(params.height, maxHeight));
    }
    
    /**
     * Get current active window count
     */
    public int getActiveWindowCount() {
        return activeWindows.size();
    }
    
    /**
     * Clean up orphaned window entries
     */
    public void cleanupOrphanedWindows() {
        List<String> orphanedWindows = new ArrayList<>();
        
        for (String windowId : activeWindows.keySet()) {
            WindowInfo windowInfo = activeWindows.get(windowId);
            if (windowInfo == null || windowInfo.view == null) {
                orphanedWindows.add(windowId);
            }
        }
        
        for (String windowId : orphanedWindows) {
            activeWindows.remove(windowId);
            Log.d(TAG, "Removed orphaned window: " + windowId);
        }
    }
    
    /**
     * Check if app has overlay permission
     */
    public boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // Always allowed on older versions
        }
        
        try {
            return android.provider.Settings.canDrawOverlays(context);
        } catch (Exception e) {
            Log.e(TAG, "Error checking overlay permission", e);
            return false;
        }
    }
    
    /**
     * Check if window can be displayed on top
     */
    public boolean canDrawOverlays() {
        return checkOverlayPermission();
    }
    
    /**
     * Request overlay permission through system settings
     */
    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}