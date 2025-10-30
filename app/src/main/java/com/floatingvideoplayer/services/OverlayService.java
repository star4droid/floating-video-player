package com.floatingvideoplayer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.floatingvideoplayer.R;
import com.floatingvideoplayer.ui.DraggableFileManagerWindow;
import com.floatingvideoplayer.ui.DraggableVideoPlayerWindow;
import com.floatingvideoplayer.ui.MainActivity;
import com.floatingvideoplayer.ui.WindowControls;
import com.floatingvideoplayer.utils.WindowManagerHelper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing floating overlay windows.
 * Handles both video player and file manager overlays.
 */
public class OverlayService extends Service {
    
    private static final String TAG = "OverlayService";
    
    // Service action constants
    public static final String EXTRA_ACTION = "action";
    public static final int ACTION_SHOW_FILE_MANAGER = 1;
    public static final int ACTION_SHOW_VIDEO_PLAYER = 2;
    public static final int ACTION_STOP_SERVICE = 3;
    public static final int ACTION_TOGGLE_OVERLAY = 4;
    
    // Notification channels
    private static final String CHANNEL_ID = "overlay_service_channel";
    private static final String CHANNEL_NAME = "Floating Video Player Service";
    
    // Window types and dimensions
    private static final int OVERLAY_WINDOW_TYPE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    
    // Window dimensions (in pixels)
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;
    private static final int FILE_MANAGER_WIDTH = 500;
    private static final int FILE_MANAGER_HEIGHT = 600;
    
    // Notification ID
    private static final int NOTIFICATION_ID = 1001;
    
    private WindowManagerHelper windowManagerHelper;
    private WindowManager windowManager;
    private Handler mainHandler;
    
    // Enhanced overlay components
    private DraggableVideoPlayerWindow videoPlayerWindow;
    private DraggableFileManagerWindow fileManagerWindow;
    private WindowControls windowControls;
    
    // State tracking
    private AtomicBoolean isVideoPlayerVisible = new AtomicBoolean(false);
    private AtomicBoolean isFileManagerVisible = new AtomicBoolean(false);
    private AtomicBoolean isControlsVisible = new AtomicBoolean(false);
    private AtomicBoolean isServiceRunning = new AtomicBoolean(false);
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "OverlayService created");
        
        initializeComponents();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        isServiceRunning.set(true);
    }
    
    /**
     * Initialize service components
     */
    private void initializeComponents() {
        windowManagerHelper = new WindowManagerHelper(this);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize window components
        initializeWindowComponents();
        
        Log.d(TAG, "Components initialized");
    }
    
    /**
     * Initialize window components
     */
    private void initializeWindowComponents() {
        try {
            // Initialize video player window
            videoPlayerWindow = new DraggableVideoPlayerWindow(this);
            
            // Initialize file manager window
            fileManagerWindow = new DraggableFileManagerWindow(this);
            
            // Initialize window controls
            windowControls = new WindowControls(this);
            windowControls.setOnControlsActionListener(new WindowControls.OnControlsActionListener() {
                @Override
                public void onShowVideoPlayer() {
                    showVideoPlayer();
                }
                
                @Override
                public void onShowFileManager() {
                    showFileManager();
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
            
            Log.d(TAG, "Window components initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing window components", e);
        }
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            
            channel.setDescription("Manages floating video player overlay");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
                ? PendingIntent.FLAG_IMMUTABLE 
                : 0
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Video Player")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        
        if (intent != null) {
            int action = intent.getIntExtra(EXTRA_ACTION, ACTION_SHOW_FILE_MANAGER);
            handleAction(action);
        }
        
        return START_STICKY;
    }
    
    /**
     * Handle service actions
     */
    private void handleAction(int action) {
        Log.d(TAG, "Handling action: " + action);
        
        switch (action) {
            case ACTION_SHOW_FILE_MANAGER:
                showFileManager();
                break;
            case ACTION_SHOW_VIDEO_PLAYER:
                showVideoPlayer();
                break;
            case ACTION_TOGGLE_OVERLAY:
                toggleOverlay();
                break;
            case ACTION_STOP_SERVICE:
                stopSelf();
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                break;
        }
    }
    
    /**
     * Show file manager overlay
     */
    private void showFileManager() {
        mainHandler.post(() -> {
            if (fileManagerWindow == null) {
                Log.e(TAG, "File manager window not initialized");
                return;
            }
            
            try {
                if (isFileManagerVisible.get()) {
                    fileManagerWindow.hide();
                    isFileManagerVisible.set(false);
                    updateControlsState();
                } else {
                    fileManagerWindow.show();
                    isFileManagerVisible.set(true);
                    
                    Log.d(TAG, "File manager overlay shown");
                    updateControlsState();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing file manager", e);
                Toast.makeText(this, "Error showing file manager", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Show video player overlay
     */
    private void showVideoPlayer() {
        mainHandler.post(() -> {
            if (videoPlayerWindow == null) {
                Log.e(TAG, "Video player window not initialized");
                return;
            }
            
            try {
                if (isVideoPlayerVisible.get()) {
                    videoPlayerWindow.hide();
                    isVideoPlayerVisible.set(false);
                    updateControlsState();
                } else {
                    videoPlayerWindow.show();
                    isVideoPlayerVisible.set(true);
                    
                    Log.d(TAG, "Video player overlay shown");
                    updateControlsState();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing video player", e);
                Toast.makeText(this, "Error showing video player", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Update controls button states
     */
    private void updateControlsState() {
        if (windowControls != null && isControlsVisible.get()) {
            windowControls.updateButtonStates(
                isVideoPlayerVisible.get(), 
                isFileManagerVisible.get()
            );
        }
    }
    
    /**
     * Show floating controls window
     */
    private void showFloatingControls() {
        mainHandler.post(() -> {
            if (windowControls == null) {
                Log.w(TAG, "Window controls not initialized");
                return;
            }
            
            try {
                if (isControlsVisible.get()) {
                    windowControls.hide();
                    isControlsVisible.set(false);
                } else {
                    windowControls.show();
                    isControlsVisible.set(true);
                    updateControlsState();
                }
                
                Log.d(TAG, "Floating controls shown");
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing floating controls", e);
            }
        });
    }
    
    /**
     * Hide file manager overlay
     */
    private void hideFileManager() {
        mainHandler.post(() -> {
            if (fileManagerWindow != null && isFileManagerVisible.get()) {
                try {
                    fileManagerWindow.hide();
                    isFileManagerVisible.set(false);
                    updateControlsState();
                    Log.d(TAG, "File manager overlay hidden");
                } catch (Exception e) {
                    Log.e(TAG, "Error hiding file manager", e);
                }
            }
        });
    }
    
    /**
     * Hide video player overlay
     */
    private void hideVideoPlayer() {
        mainHandler.post(() -> {
            if (videoPlayerWindow != null && isVideoPlayerVisible.get()) {
                try {
                    videoPlayerWindow.hide();
                    isVideoPlayerVisible.set(false);
                    updateControlsState();
                    Log.d(TAG, "Video player overlay hidden");
                } catch (Exception e) {
                    Log.e(TAG, "Error hiding video player", e);
                }
            }
        });
    }
    
    /**
     * Hide floating controls
     */
    private void hideFloatingControls() {
        mainHandler.post(() -> {
            if (windowControls != null && isControlsVisible.get()) {
                try {
                    windowControls.hide();
                    isControlsVisible.set(false);
                    Log.d(TAG, "Floating controls hidden");
                } catch (Exception e) {
                    Log.e(TAG, "Error hiding floating controls", e);
                }
            }
        });
    }
    
    /**
     * Toggle overlay visibility
     */
    private void toggleOverlay() {
        mainHandler.post(() -> {
            if (isVideoPlayerVisible.get()) {
                hideVideoPlayer();
            } else if (isFileManagerVisible.get()) {
                hideFileManager();
            } else {
                showFileManager();
            }
        });
    }
    
    /**
     * Close all windows
     */
    private void closeAllWindows() {
        mainHandler.post(() -> {
            hideVideoPlayer();
            hideFileManager();
            hideFloatingControls();
            
            Log.d(TAG, "All windows closed");
            updateNotification();
        });
    }
    
    /**
     * Open settings
     */
    private void openSettings() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            
            Toast.makeText(this, "Settings opened", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings", e);
            Toast.makeText(this, "Error opening settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Load video in player
     */
    public void loadVideoInPlayer(String videoPath) {
        mainHandler.post(() -> {
            try {
                if (videoPlayerWindow != null) {
                    if (!isVideoPlayerVisible.get()) {
                        showVideoPlayer();
                    }
                    videoPlayerWindow.loadVideo(videoPath);
                    Log.d(TAG, "Video loaded in player: " + videoPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading video in player", e);
                Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Get current window states
     */
    public WindowState getWindowStates() {
        return new WindowState(
            isVideoPlayerVisible.get(),
            isFileManagerVisible.get(),
            isControlsVisible.get()
        );
    }
    
    /**
     * Get video player window instance
     */
    public DraggableVideoPlayerWindow getVideoPlayerWindow() {
        return videoPlayerWindow;
    }
    
    /**
     * Get file manager window instance
     */
    public DraggableFileManagerWindow getFileManagerWindow() {
        return fileManagerWindow;
    }
    
    /**
     * Get window controls instance
     */
    public WindowControls getWindowControls() {
        return windowControls;
    }
    
    /**
     * Check if service is properly initialized
     */
    public boolean isServiceInitialized() {
        return videoPlayerWindow != null && fileManagerWindow != null && windowControls != null;
    }
    
    /**
     * Handle service initialization errors
     */
    private void handleServiceInitializationError(String component) {
        Log.e(TAG, "Failed to initialize " + component);
        mainHandler.post(() -> {
            Toast.makeText(this, "Failed to initialize " + component, Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Window state container
     */
    public static class WindowState {
        public final boolean videoPlayerVisible;
        public final boolean fileManagerVisible;
        public final boolean controlsVisible;
        
        public WindowState(boolean videoPlayerVisible, boolean fileManagerVisible, boolean controlsVisible) {
            this.videoPlayerVisible = videoPlayerVisible;
            this.fileManagerVisible = fileManagerVisible;
            this.controlsVisible = controlsVisible;
        }
    }
    
    /**
     * Update notification
     */
    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }
    
    /**
     * Check if service is running
     */
    public static boolean isServiceRunning(Context context) {
        // This would typically check if the service is running using ActivityManager
        // For simplicity, we'll use a static approach
        return context != null; // Simplified check
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "OverlayService destroyed");
        
        // Clean up all overlays with enhanced lifecycle management
        cleanupServiceComponents();
        
        isServiceRunning.set(false);
        
        super.onDestroy();
    }
    
    /**
     * Clean up all service components
     */
    private void cleanupServiceComponents() {
        try {
            // Hide and destroy video player window
            if (videoPlayerWindow != null) {
                videoPlayerWindow.destroy();
                videoPlayerWindow = null;
            }
            
            // Hide and destroy file manager window
            if (fileManagerWindow != null) {
                fileManagerWindow.destroy();
                fileManagerWindow = null;
            }
            
            // Hide and destroy window controls
            if (windowControls != null) {
                windowControls.destroy();
                windowControls = null;
            }
            
            // Clean up window manager helper
            if (windowManagerHelper != null) {
                windowManagerHelper.closeAllWindows();
            }
            
            Log.d(TAG, "Service components cleaned up successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up service components", e);
        }
    }
    
    /**
     * Handle service restart
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed, service restarting...");
        
        // Optionally restart the service
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        
        super.onTaskRemoved(rootIntent);
    }
    
    /**
     * Handle low memory situations
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        Log.d(TAG, "Memory trim requested, level: " + level);
        
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // Reduce memory usage but keep service running
                minimizeAllWindows();
                break;
            case TRIM_MEMORY_COMPLETE:
                // Cleanup as much as possible
                closeAllWindows();
                break;
        }
    }
    
    /**
     * Minimize all windows to free memory
     */
    private void minimizeAllWindows() {
        if (isVideoPlayerVisible.get()) {
            hideVideoPlayer();
        }
        if (isFileManagerVisible.get()) {
            hideFileManager();
        }
        if (isControlsVisible.get()) {
            hideFloatingControls();
        }
        Log.d(TAG, "All windows minimized to free memory");
    }
    
    /**
     * Handle service error recovery
     */
    private void handleServiceError(Exception e, String operation) {
        Log.e(TAG, "Service error during " + operation, e);
        
        // Attempt to recover by cleaning up and reinitializing
        try {
            cleanupServiceComponents();
            initializeComponents();
            Log.d(TAG, "Service recovered from error");
        } catch (Exception recoveryError) {
            Log.e(TAG, "Failed to recover from service error", recoveryError);
            stopSelf();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is not a bound service
    }
}