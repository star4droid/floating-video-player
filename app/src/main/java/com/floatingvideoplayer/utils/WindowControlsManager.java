package com.floatingvideoplayer.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.floatingvideoplayer.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages window control features like minimize, transparency, lock, floating buttons
 * Provides advanced window control interface
 */
public class WindowControlsManager {
    
    private static final String TAG = "WindowControlsManager";
    
    // Window control types
    public enum ControlType {
        MINIMIZE,
        MAXIMIZE,
        CLOSE,
        TRANSPARENCY,
        LOCK,
        STICKY,
        ALWAYS_ON_TOP,
        TRANSPARENT_BACKGROUND,
        FULLSCREEN,
        RESTORE
    }
    
    // Window states
    public enum WindowState {
        NORMAL,
        MINIMIZED,
        MAXIMIZED,
        TRANSPARENT,
        LOCKED,
        STICKY
    }
    
    /**
     * Window control button information
     */
    public static class ControlButton {
        public ControlType type;
        public int iconResource;
        public String tooltip;
        public boolean isEnabled = true;
        public boolean isVisible = true;
        public float alpha = 1.0f;
        public int position; // 0=left, 1=right, 2=top, 3=bottom
        
        public ControlButton(ControlType type, int iconResource, String tooltip) {
            this.type = type;
            this.iconResource = iconResource;
            this.tooltip = tooltip;
        }
    }
    
    /**
     * Floating action button configuration
     */
    public static class FloatingActionButton {
        public String id;
        public int iconResource;
        public String label;
        public int position; // Gravity position
        public float size = 56.0f; // dp
        public boolean isEnabled = true;
        public View.OnClickListener clickListener;
        public Map<String, Object> customData;
        
        public FloatingActionButton(String id, int iconResource, String label, int position) {
            this.id = id;
            this.iconResource = iconResource;
            this.label = label;
            this.position = position;
            this.customData = new HashMap<>();
        }
    }
    
    /**
     * Window control listener
     */
    public interface WindowControlListener {
        void onControlActivated(ControlType type, String windowId);
        void onControlStateChanged(ControlType type, boolean newState, String windowId);
        void onTransparencyChanged(float newAlpha, String windowId);
        void onFloatingActionButtonClicked(String buttonId, String windowId);
        void onLockStateChanged(boolean isLocked, String windowId);
    }
    
    private Context context;
    private WindowManager windowManager;
    private WindowStateManager windowStateManager;
    private WindowAnimationManager animationManager;
    private Handler mainHandler;
    
    // Control views
    private ConcurrentHashMap<String, View> controlBars;
    private ConcurrentHashMap<String, List<ControlButton>> controlButtons;
    private ConcurrentHashMap<String, List<FloatingActionButton>> floatingButtons;
    private ConcurrentHashMap<String, WindowState> windowStates;
    private ConcurrentHashMap<String, WindowControlListener> controlListeners;
    
    // Control configuration
    private Map<String, ControlButton> defaultButtons;
    private boolean showControls = true;
    private boolean showOnHover = true;
    private boolean autoHide = true;
    private int controlBarHeight = 48; // dp
    private int controlBarTimeout = 3000; // milliseconds
    
    public WindowControlsManager(Context context, WindowStateManager windowStateManager, 
                                WindowAnimationManager animationManager) {
        this.context = context;
        this.windowStateManager = windowStateManager;
        this.animationManager = animationManager;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        this.controlBars = new ConcurrentHashMap<>();
        this.controlButtons = new ConcurrentHashMap<>();
        this.floatingButtons = new ConcurrentHashMap<>();
        this.windowStates = new ConcurrentHashMap<>();
        this.controlListeners = new ConcurrentHashMap<>();
        
        initializeDefaultButtons();
        
        Log.d(TAG, "WindowControlsManager initialized");
    }
    
    /**
     * Initialize default control buttons
     */
    private void initializeDefaultButtons() {
        defaultButtons = new HashMap<>();
        
        // Default buttons for video player
        defaultButtons.put("minimize_player", new ControlButton(
            ControlType.MINIMIZE, R.drawable.ic_minimize, "Minimize"));
        defaultButtons.put("maximize_player", new ControlButton(
            ControlType.MAXIMIZE, R.drawable.ic_fullscreen, "Maximize"));
        defaultButtons.put("lock_player", new ControlButton(
            ControlType.LOCK, R.drawable.ic_lock, "Lock"));
        defaultButtons.put("transparency_player", new ControlButton(
            ControlType.TRANSPARENCY, R.drawable.ic_eye, "Transparency"));
        defaultButtons.put("close_player", new ControlButton(
            ControlType.CLOSE, R.drawable.ic_close, "Close"));
        
        // Default buttons for file manager
        defaultButtons.put("minimize_files", new ControlButton(
            ControlType.MINIMIZE, R.drawable.ic_minimize, "Minimize"));
        defaultButtons.put("maximize_files", new ControlButton(
            ControlType.MAXIMIZE, R.drawable.ic_fullscreen, "Maximize"));
        defaultButtons.put("lock_files", new ControlButton(
            ControlType.LOCK, R.drawable.ic_lock, "Lock"));
        defaultButtons.put("sticky_files", new ControlButton(
            ControlType.STICKY, R.drawable.ic_push_pin, "Pin"));
        defaultButtons.put("close_files", new ControlButton(
            ControlType.CLOSE, R.drawable.ic_close, "Close"));
    }
    
    /**
     * Create control bar for a window
     */
    public View createControlBar(String windowId, WindowManager.LayoutParams layoutParams) {
        try {
            // Check if control bar already exists
            if (controlBars.containsKey(windowId)) {
                Log.w(TAG, "Control bar already exists for window: " + windowId);
                return controlBars.get(windowId);
            }
            
            // Create control bar layout
            LinearLayout controlBar = new LinearLayout(context);
            controlBar.setOrientation(LinearLayout.HORIZONTAL);
            controlBar.setBackgroundColor(0x80000000); // Semi-transparent background
            controlBar.setPadding(16, 8, 16, 8);
            controlBar.setVisibility(View.GONE);
            
            // Set layout parameters for control bar
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(controlBarHeight)
            );
            barParams.gravity = Gravity.TOP;
            controlBar.setLayoutParams(barParams);
            
            // Create control buttons
            List<ControlButton> buttons = createControlButtons(windowId);
            for (ControlButton button : buttons) {
                ImageButton controlButton = createControlButton(button);
                controlBar.addView(controlButton);
            }
            
            // Create window manager layout params for control bar
            WindowManager.LayoutParams controlBarParams = new WindowManager.LayoutParams(
                layoutParams.width - dpToPx(32),
                dpToPx(controlBarHeight),
                layoutParams.type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );
            
            controlBarParams.gravity = Gravity.TOP | Gravity.END;
            controlBarParams.x = layoutParams.x + dpToPx(16);
            controlBarParams.y = layoutParams.y + dpToPx(16);
            
            // Add control bar to window manager
            windowManager.addView(controlBar, controlBarParams);
            
            // Store references
            controlBars.put(windowId, controlBar);
            controlButtons.put(windowId, buttons);
            
            // Setup auto-hide if enabled
            if (autoHide) {
                setupAutoHide(controlBar, windowId);
            }
            
            Log.d(TAG, "Control bar created for window: " + windowId);
            return controlBar;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating control bar for window: " + windowId, e);
            return null;
        }
    }
    
    /**
     * Create control buttons for a window
     */
    private List<ControlButton> createControlButtons(String windowId) {
        List<ControlButton> buttons = new ArrayList<>();
        
        // Get default buttons based on window type
        if (windowId.contains("player")) {
            buttons.add(defaultButtons.get("minimize_player"));
            buttons.add(defaultButtons.get("maximize_player"));
            buttons.add(defaultButtons.get("transparency_player"));
            buttons.add(defaultButtons.get("lock_player"));
            buttons.add(defaultButtons.get("close_player"));
        } else if (windowId.contains("file") || windowId.contains("manager")) {
            buttons.add(defaultButtons.get("minimize_files"));
            buttons.add(defaultButtons.get("maximize_files"));
            buttons.add(defaultButtons.get("sticky_files"));
            buttons.add(defaultButtons.get("lock_files"));
            buttons.add(defaultButtons.get("close_files"));
        }
        
        return buttons;
    }
    
    /**
     * Create individual control button
     */
    private ImageButton createControlButton(ControlButton buttonConfig) {
        ImageButton button = new ImageButton(context);
        
        // Set button properties
        button.setImageResource(buttonConfig.iconResource);
        button.setContentDescription(buttonConfig.tooltip);
        button.setBackgroundColor(0x00000000); // Transparent background
        
        // Set button size
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(40), dpToPx(40)
        );
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        button.setLayoutParams(params);
        
        // Set click listener
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleControlButtonClick(buttonConfig.type);
            }
        });
        
        // Set long click listener for context menu
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showControlContextMenu(buttonConfig);
                return true;
            }
        });
        
        return button;
    }
    
    /**
     * Handle control button click
     */
    private void handleControlButtonClick(ControlType controlType) {
        // Find which window this button belongs to
        String windowId = findWindowForControl(controlType);
        if (windowId != null) {
            executeControlAction(controlType, windowId);
        }
    }
    
    /**
     * Execute control action
     */
    public void executeControlAction(ControlType controlType, String windowId) {
        Log.d(TAG, "Executing control action: " + controlType + " for window: " + windowId);
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onControlActivated(controlType, windowId);
        }
        
        switch (controlType) {
            case MINIMIZE:
                minimizeWindow(windowId);
                break;
            case MAXIMIZE:
                maximizeWindow(windowId);
                break;
            case CLOSE:
                closeWindow(windowId);
                break;
            case TRANSPARENCY:
                toggleTransparency(windowId);
                break;
            case LOCK:
                toggleLock(windowId);
                break;
            case STICKY:
                toggleSticky(windowId);
                break;
            default:
                Log.w(TAG, "Unknown control type: " + controlType);
        }
    }
    
    /**
     * Minimize window
     */
    public void minimizeWindow(String windowId) {
        View controlBar = controlBars.get(windowId);
        if (controlBar == null) return;
        
        WindowState state = getWindowState(windowId);
        state.isMinimized = true;
        state.isMaximized = false;
        
        // Hide control bar
        hideControlBar(windowId);
        
        // Animate minimization
        animationManager.animateWindowVisibility("minimize_" + windowId, controlBar, false, 
            new WindowAnimationManager.AnimationListener() {
                @Override
                public void onAnimationStart(String animationId) {
                    Log.d(TAG, "Minimization started for window: " + windowId);
                }
                
                @Override
                public void onAnimationEnd(String animationId, boolean completed) {
                    if (controlListeners.containsKey(windowId)) {
                        controlListeners.get(windowId).onControlStateChanged(
                            ControlType.MINIMIZE, true, windowId);
                    }
                }
                
                @Override
                public void onAnimationCancel(String animationId) {}
            });
        
        Log.d(TAG, "Window minimized: " + windowId);
    }
    
    /**
     * Maximize window
     */
    public void maximizeWindow(String windowId) {
        View controlBar = controlBars.get(windowId);
        if (controlBar == null) return;
        
        WindowState state = getWindowState(windowId);
        state.isMinimized = false;
        state.isMaximized = true;
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onControlStateChanged(
                ControlType.MAXIMIZE, true, windowId);
        }
        
        Log.d(TAG, "Window maximized: " + windowId);
    }
    
    /**
     * Close window
     */
    public void closeWindow(String windowId) {
        Log.d(TAG, "Closing window: " + windowId);
        
        // Remove control bar
        removeControlBar(windowId);
        
        // Remove floating buttons
        removeFloatingButtons(windowId);
        
        // Clear states
        windowStates.remove(windowId);
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onControlActivated(ControlType.CLOSE, windowId);
        }
    }
    
    /**
     * Toggle window transparency
     */
    public void toggleTransparency(String windowId) {
        WindowState state = getWindowState(windowId);
        boolean newTransparent = !state.isTransparent;
        
        state.isTransparent = newTransparent;
        float newAlpha = newTransparent ? 0.7f : 1.0f;
        
        // Apply transparency through animation
        // Note: This would need access to the actual window layout params
        animationManager.animateWindowOpacity("transparency_" + windowId, 
            null, newAlpha, null); // Would need actual layout params
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onTransparencyChanged(newAlpha, windowId);
            controlListeners.get(windowId).onControlStateChanged(
                ControlType.TRANSPARENCY, newTransparent, windowId);
        }
        
        Log.d(TAG, "Window transparency toggled: " + newTransparent + " for window: " + windowId);
    }
    
    /**
     * Toggle window lock
     */
    public void toggleLock(String windowId) {
        WindowState state = getWindowState(windowId);
        boolean newLocked = !state.isLocked;
        
        state.isLocked = newLocked;
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onLockStateChanged(newLocked, windowId);
            controlListeners.get(windowId).onControlStateChanged(
                ControlType.LOCK, newLocked, windowId);
        }
        
        Log.d(TAG, "Window lock toggled: " + newLocked + " for window: " + windowId);
    }
    
    /**
     * Toggle window sticky (always on top)
     */
    public void toggleSticky(String windowId) {
        WindowState state = getWindowState(windowId);
        boolean newSticky = !state.isSticky;
        
        state.isSticky = newSticky;
        
        if (controlListeners.containsKey(windowId)) {
            controlListeners.get(windowId).onControlStateChanged(
                ControlType.STICKY, newSticky, windowId);
        }
        
        Log.d(TAG, "Window sticky toggled: " + newSticky + " for window: " + windowId);
    }
    
    /**
     * Create floating action buttons for a window
     */
    public List<View> createFloatingActionButtons(String windowId, WindowManager.LayoutParams layoutParams) {
        List<View> buttonViews = new ArrayList<>();
        
        List<FloatingActionButton> fabs = floatingButtons.get(windowId);
        if (fabs == null || fabs.isEmpty()) {
            return buttonViews;
        }
        
        for (FloatingActionButton fab : fabs) {
            if (!fab.isEnabled) continue;
            
            Button button = new Button(context);
            button.setText(fab.label);
            button.setBackgroundResource(R.drawable.button_background);
            
            // Set button properties
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            button.setLayoutParams(params);
            
            // Set click listener
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fab.clickListener != null) {
                        fab.clickListener.onClick(v);
                    }
                    if (controlListeners.containsKey(windowId)) {
                        controlListeners.get(windowId).onFloatingActionButtonClicked(fab.id, windowId);
                    }
                }
            });
            
            // Create layout params for window manager
            WindowManager.LayoutParams buttonParams = new WindowManager.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                layoutParams.type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );
            
            buttonParams.gravity = fab.position;
            buttonParams.x = layoutParams.x + dpToPx(16);
            buttonParams.y = layoutParams.y + dpToPx(16);
            
            // Add to window manager
            windowManager.addView(button, buttonParams);
            buttonViews.add(button);
        }
        
        return buttonViews;
    }
    
    /**
     * Show control bar
     */
    public void showControlBar(String windowId) {
        View controlBar = controlBars.get(windowId);
        if (controlBar == null || !showControls) return;
        
        controlBar.setVisibility(View.VISIBLE);
        
        if (showOnHover) {
            // Auto-hide after timeout
            mainHandler.postDelayed(() -> hideControlBar(windowId), controlBarTimeout);
        }
    }
    
    /**
     * Hide control bar
     */
    public void hideControlBar(String windowId) {
        View controlBar = controlBars.get(windowId);
        if (controlBar == null) return;
        
        controlBar.setVisibility(View.GONE);
    }
    
    /**
     * Remove control bar
     */
    public void removeControlBar(String windowId) {
        View controlBar = controlBars.remove(windowId);
        if (controlBar != null) {
            windowManager.removeView(controlBar);
        }
        
        controlButtons.remove(windowId);
    }
    
    /**
     * Remove floating buttons
     */
    public void removeFloatingButtons(String windowId) {
        List<FloatingActionButton> fabs = floatingButtons.remove(windowId);
        if (fabs != null) {
            // Would need to remove actual views from window manager
            Log.d(TAG, "Removed floating buttons for window: " + windowId);
        }
    }
    
    /**
     * Setup auto-hide for control bar
     */
    private void setupAutoHide(View controlBar, String windowId) {
        controlBar.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                    showControlBar(windowId);
                } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                    mainHandler.postDelayed(() -> hideControlBar(windowId), 1000);
                }
                return false;
            }
        });
    }
    
    /**
     * Add floating action button
     */
    public void addFloatingActionButton(String windowId, FloatingActionButton fab) {
        List<FloatingActionButton> fabs = floatingButtons.computeIfAbsent(windowId, k -> new ArrayList<>());
        fabs.add(fab);
    }
    
    /**
     * Remove floating action button
     */
    public void removeFloatingActionButton(String windowId, String buttonId) {
        List<FloatingActionButton> fabs = floatingButtons.get(windowId);
        if (fabs != null) {
            fabs.removeIf(fab -> fab.id.equals(buttonId));
        }
    }
    
    /**
     * Show control context menu
     */
    private void showControlContextMenu(ControlButton button) {
        // Simple toast for now - could be enhanced with a proper context menu
        Toast.makeText(context, button.tooltip + " - " + 
            (button.isEnabled ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Find window for control button
     */
    private String findWindowForControl(ControlType controlType) {
        // This is a simplified implementation
        // In practice, you'd track which control belongs to which window more carefully
        for (String windowId : controlBars.keySet()) {
            if (controlBars.get(windowId) != null) {
                return windowId;
            }
        }
        return null;
    }
    
    /**
     * Get window state
     */
    private WindowState getWindowState(String windowId) {
        return windowStates.computeIfAbsent(windowId, k -> new WindowState());
    }
    
    /**
     * Set control listener for window
     */
    public void setControlListener(String windowId, WindowControlListener listener) {
        controlListeners.put(windowId, listener);
    }
    
    /**
     * Remove control listener
     */
    public void removeControlListener(String windowId) {
        controlListeners.remove(windowId);
    }
    
    /**
     * Show/hide controls globally
     */
    public void setShowControls(boolean show) {
        this.showControls = show;
        if (!show) {
            // Hide all control bars
            for (String windowId : controlBars.keySet()) {
                hideControlBar(windowId);
            }
        }
    }
    
    /**
     * Set auto-hide behavior
     */
    public void setAutoHide(boolean autoHide, int timeoutMs) {
        this.autoHide = autoHide;
        this.controlBarTimeout = timeoutMs;
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
    
    /**
     * Clean up all resources
     */
    public void cleanup() {
        // Remove all control bars
        for (String windowId : new ArrayList<>(controlBars.keySet())) {
            removeControlBar(windowId);
        }
        
        // Remove all floating buttons
        for (String windowId : new ArrayList<>(floatingButtons.keySet())) {
            removeFloatingButtons(windowId);
        }
        
        // Clear all data
        controlBars.clear();
        controlButtons.clear();
        floatingButtons.clear();
        windowStates.clear();
        controlListeners.clear();
        
        Log.d(TAG, "WindowControlsManager cleaned up");
    }
    
    /**
     * Simplified WindowState class for internal use
     */
    private static class WindowState {
        public boolean isMinimized = false;
        public boolean isMaximized = false;
        public boolean isTransparent = false;
        public boolean isLocked = false;
        public boolean isSticky = false;
    }
}