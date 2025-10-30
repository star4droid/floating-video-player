package com.floatingvideoplayer.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.floatingvideoplayer.services.OverlayService;
import com.floatingvideoplayer.ui.DraggableFileManagerWindow;
import com.floatingvideoplayer.ui.DraggableVideoPlayerWindow;
import com.floatingvideoplayer.ui.WindowControls;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class for integrating all floating window components
 * Manages communication between video player, file manager, and controls
 */
public class FloatingWindowIntegrationHelper {
    
    private static final String TAG = "FloatingWindowIntegrationHelper";
    
    private Context context;
    private OverlayService overlayService;
    private WindowManagerHelper windowManagerHelper;
    
    // Component instances
    private DraggableVideoPlayerWindow videoPlayerWindow;
    private DraggableFileManagerWindow fileManagerWindow;
    private WindowControls windowControls;
    
    // State tracking
    private AtomicBoolean isInitialized = new AtomicBoolean(false);
    private AtomicBoolean isIntegrationEnabled = new AtomicBoolean(true);
    
    // Broadcast receiver for file selection events
    private BroadcastReceiver videoSelectionReceiver;
    
    public FloatingWindowIntegrationHelper(Context context) {
        this.context = context;
        this.windowManagerHelper = new WindowManagerHelper(context);
        initializeComponents();
    }
    
    /**
     * Initialize all window components
     */
    private void initializeComponents() {
        try {
            Log.d(TAG, "Initializing floating window components...");
            
            // Get service instance
            overlayService = getOverlayService();
            
            if (overlayService != null) {
                // Get window instances from service
                videoPlayerWindow = overlayService.getVideoPlayerWindow();
                fileManagerWindow = overlayService.getFileManagerWindow();
                windowControls = overlayService.getWindowControls();
                
                // Setup integration
                setupComponentIntegration();
                setupBroadcastReceivers();
                
                isInitialized.set(true);
                Log.d(TAG, "Floating window components initialized successfully");
            } else {
                Log.w(TAG, "Overlay service not available for initialization");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing floating window components", e);
        }
    }
    
    /**
     * Setup integration between components
     */
    private void setupComponentIntegration() {
        if (videoPlayerWindow != null && fileManagerWindow != null) {
            // Setup file manager callback for video selection
            fileManagerWindow.setOnVideoSelectedListener(new DraggableFileManagerWindow.OnVideoSelectedListener() {
                @Override
                public void onVideoSelected(String videoPath) {
                    loadVideoInPlayer(videoPath);
                }
            });
            
            Log.d(TAG, "File manager to video player integration setup complete");
        }
        
        if (windowControls != null) {
            // Setup control callbacks
            windowControls.setOnControlsActionListener(new WindowControls.OnControlsActionListener() {
                @Override
                public void onShowVideoPlayer() {
                    toggleVideoPlayer();
                }
                
                @Override
                public void onShowFileManager() {
                    toggleFileManager();
                }
                
                @Override
                public void onSettings() {
                    openSettings();
                }
                
                @Override
                public void onCloseAll() {
                    closeAllWindows();
                }
                
                @Override
                public void onToggleOverlay() {
                    toggleOverlay();
                }
            });
            
            Log.d(TAG, "Window controls integration setup complete");
        }
    }
    
    /**
     * Setup broadcast receivers for inter-component communication
     */
    private void setupBroadcastReceivers() {
        // Video selection receiver
        videoSelectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.floatingvideoplayer.LOAD_VIDEO".equals(intent.getAction())) {
                    String videoPath = intent.getStringExtra("video_path");
                    if (videoPath != null) {
                        loadVideoInPlayer(videoPath);
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.floatingvideoplayer.LOAD_VIDEO");
        filter.addAction("com.floatingvideoplayer.SHOW_FILE_MANAGER");
        filter.addAction("com.floatingvideoplayer.SHOW_VIDEO_PLAYER");
        
        context.registerReceiver(videoSelectionReceiver, filter);
        Log.d(TAG, "Broadcast receivers setup complete");
    }
    
    /**
     * Load video in the player window
     */
    public void loadVideoInPlayer(String videoPath) {
        try {
            if (videoPlayerWindow != null) {
                // Ensure video player is visible
                if (!videoPlayerWindow.isVisible()) {
                    videoPlayerWindow.show();
                }
                
                // Load the video
                videoPlayerWindow.loadVideo(videoPath);
                
                Log.d(TAG, "Video loaded in player: " + videoPath);
            } else {
                Log.w(TAG, "Video player window not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading video in player", e);
        }
    }
    
    /**
     * Toggle video player window visibility
     */
    public void toggleVideoPlayer() {
        try {
            if (videoPlayerWindow != null) {
                if (videoPlayerWindow.isVisible()) {
                    videoPlayerWindow.hide();
                } else {
                    videoPlayerWindow.show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling video player", e);
        }
    }
    
    /**
     * Toggle file manager window visibility
     */
    public void toggleFileManager() {
        try {
            if (fileManagerWindow != null) {
                if (fileManagerWindow.isVisible()) {
                    fileManagerWindow.hide();
                } else {
                    fileManagerWindow.show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling file manager", e);
        }
    }
    
    /**
     * Show window controls
     */
    public void showWindowControls() {
        try {
            if (windowControls != null) {
                windowControls.show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing window controls", e);
        }
    }
    
    /**
     * Hide window controls
     */
    public void hideWindowControls() {
        try {
            if (windowControls != null) {
                windowControls.hide();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding window controls", e);
        }
    }
    
    /**
     * Close all windows
     */
    public void closeAllWindows() {
        try {
            if (videoPlayerWindow != null) {
                videoPlayerWindow.hide();
            }
            if (fileManagerWindow != null) {
                fileManagerWindow.hide();
            }
            if (windowControls != null) {
                windowControls.hide();
            }
            
            Log.d(TAG, "All windows closed");
        } catch (Exception e) {
            Log.e(TAG, "Error closing all windows", e);
        }
    }
    
    /**
     * Toggle overlay visibility
     */
    public void toggleOverlay() {
        try {
            if (overlayService != null) {
                Intent intent = new Intent(context, OverlayService.class);
                intent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_TOGGLE_OVERLAY);
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling overlay", e);
        }
    }
    
    /**
     * Open settings
     */
    private void openSettings() {
        try {
            // This would typically open a settings activity
            Log.d(TAG, "Opening settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings", e);
        }
    }
    
    /**
     * Get current integration status
     */
    public boolean isIntegrationEnabled() {
        return isIntegrationEnabled.get();
    }
    
    /**
     * Enable or disable integration
     */
    public void setIntegrationEnabled(boolean enabled) {
        isIntegrationEnabled.set(enabled);
        Log.d(TAG, "Integration " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Get component instances
     */
    public DraggableVideoPlayerWindow getVideoPlayerWindow() {
        return videoPlayerWindow;
    }
    
    public DraggableFileManagerWindow getFileManagerWindow() {
        return fileManagerWindow;
    }
    
    public WindowControls getWindowControls() {
        return windowControls;
    }
    
    /**
     * Check if all components are initialized
     */
    public boolean isInitialized() {
        return isInitialized.get() && 
               videoPlayerWindow != null && 
               fileManagerWindow != null && 
               windowControls != null;
    }
    
    /**
     * Get overlay service instance
     */
    private OverlayService getOverlayService() {
        // This is a simplified implementation
        // In a real app, you might want to use a service binding
        return overlayService;
    }
    
    /**
     * Handle configuration changes
     */
    public void onConfigurationChanged() {
        try {
            // Update window positions/sizes if needed
            if (videoPlayerWindow != null && videoPlayerWindow.isVisible()) {
                int[] bounds = videoPlayerWindow.getWindowBounds();
                windowManagerHelper.constrainWindowToScreen(null); // This would need proper params
            }
            
            Log.d(TAG, "Configuration changes handled");
        } catch (Exception e) {
            Log.e(TAG, "Error handling configuration changes", e);
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        try {
            // Unregister broadcast receivers
            if (videoSelectionReceiver != null) {
                context.unregisterReceiver(videoSelectionReceiver);
                videoSelectionReceiver = null;
            }
            
            // Clear references
            videoPlayerWindow = null;
            fileManagerWindow = null;
            windowControls = null;
            overlayService = null;
            
            isInitialized.set(false);
            
            Log.d(TAG, "Floating window integration cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up integration", e);
        }
    }
}
