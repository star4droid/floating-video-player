package com.floatingvideoplayer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Application class for Floating Video Player
 */
public class FloatingVideoPlayerApp extends Application {
    
    private static final String TAG = "FloatingVideoPlayerApp";
    private static FloatingVideoPlayerApp instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        instance = this;
        
        Log.d(TAG, "Floating Video Player App starting");
        
        // Initialize Media3 library
        initializeMedia3();
        
        // Create notification channels
        createNotificationChannels();
        
        // Initialize any other app-level components
        initializeComponents();
        
        Log.d(TAG, "App initialization complete");
    }
    
    /**
     * Get the application instance
     */
    public static FloatingVideoPlayerApp getInstance() {
        return instance;
    }
    
    /**
     * Get application context
     */
    public static Context getAppContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }
    
    /**
     * Initialize Media3 library and check compatibility
     */
    private void initializeMedia3() {
        try {
            // This forces Media3 to initialize and ensures all dependencies are ready
            String version = MediaLibraryInfo.VERSION;
            Log.d(TAG, "Media3 library version: " + version);
            
            // Initialize ExoPlayer factory to verify Media3 is working
            ExoPlayer.Factory factory = ExoPlayer.Factory.getInstance();
            Log.d(TAG, "Media3 ExoPlayer initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Media3 library", e);
            // Don't crash the app, but log the error
        }
    }
    
    /**
     * Create notification channels required by the app
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.w(TAG, "NotificationManager not available");
                return;
            }
            
            // Service channel for foreground service
            NotificationChannel serviceChannel = new NotificationChannel(
                "overlay_service_channel",
                "Floating Video Player Service",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Manages floating video player overlay");
            serviceChannel.enableLights(true);
            serviceChannel.setLightColor(android.graphics.Color.BLUE);
            serviceChannel.enableVibration(false);
            serviceChannel.setSound(null, null);
            
            // Notification channel for media controls
            NotificationChannel mediaChannel = new NotificationChannel(
                "media_controls_channel", 
                "Media Controls",
                NotificationManager.IMPORTANCE_LOW
            );
            mediaChannel.setDescription("Media playback controls");
            mediaChannel.enableVibration(false);
            mediaChannel.setSound(null, null);
            
            // Create all channels
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(mediaChannel);
            
            Log.d(TAG, "Notification channels created");
        }
    }
    
    /**
     * Initialize additional app components
     */
    private void initializeComponents() {
        // Initialize any additional components here
        // For example: database helpers, preference managers, etc.
        
        Log.d(TAG, "Additional components initialized");
    }
    
    @Override
    public void onTerminate() {
        Log.d(TAG, "App terminating");
        super.onTerminate();
    }
    
    @Override
    public void onLowMemory() {
        Log.d(TAG, "Low memory warning received");
        super.onLowMemory();
    }
    
    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "Memory trim requested: " + level);
        super.onTrimMemory(level);
    }
}