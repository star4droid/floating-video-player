package com.floatingvideoplayer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility class for managing app permissions
 */
public class PermissionManager {
    
    private static final String TAG = "PermissionManager";
    
    private Context context;
    
    public PermissionManager(Context context) {
        this.context = context;
    }
    
    /**
     * Check if overlay permission is granted
     */
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // Not needed for versions below M
        }
        
        boolean hasPermission = Settings.canDrawOverlays(context);
        Log.d(TAG, "Overlay permission: " + hasPermission);
        return hasPermission;
    }
    
    /**
     * Check if storage permissions are granted
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            boolean hasVideoPermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean hasAudioPermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            boolean hasImagesPermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            
            Log.d(TAG, "Storage permissions - Video: " + hasVideoPermission + 
                  ", Audio: " + hasAudioPermission + ", Images: " + hasImagesPermission);
            return hasVideoPermission || hasAudioPermission || hasImagesPermission;
            
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 uses legacy external storage permission
            boolean hasPermission = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "External storage permission: " + hasPermission);
            return hasPermission;
        } else {
            // Android 5.x and below don't need runtime permissions for external storage
            return true;
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    public boolean hasAllRequiredPermissions() {
        boolean hasOverlay = hasOverlayPermission();
        boolean hasStorage = hasStoragePermission();
        boolean hasManageStorage = hasManageStoragePermission();
        
        boolean allGranted = hasOverlay && hasStorage && hasManageStorage;
        Log.d(TAG, "All required permissions: " + allGranted);
        
        return allGranted;
    }
    
    /**
     * Check if manage storage permission is granted (Android 11+)
     */
    public boolean hasManageStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true; // Not needed for versions below R
        }
        
        StoragePermissionHelper helper = new StoragePermissionHelper();
        boolean hasPermission = helper.hasManageStoragePermission(context);
        Log.d(TAG, "Manage storage permission: " + hasPermission);
        return hasPermission;
    }
    
    /**
     * Get list of missing permissions
     */
    public String[] getMissingPermissions() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        
        if (!hasOverlayPermission()) {
            missing.add("Overlay Permission");
        }
        
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missing.add(Manifest.permission.READ_MEDIA_VIDEO);
                missing.add(Manifest.permission.READ_MEDIA_AUDIO);
                missing.add(Manifest.permission.READ_MEDIA_IMAGES);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                missing.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        if (!hasManageStoragePermission()) {
            missing.add("Manage Storage Permission");
        }
        
        return missing.toArray(new String[0]);
    }
    
    /**
     * Request overlay permission
     */
    public void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return; // Not needed for versions below M
        }
        
        if (!hasOverlayPermission()) {
            Log.d(TAG, "Requesting overlay permission");
            // Overlay permission needs to be requested via Settings
            // This should be handled by the calling activity
        }
    }
    
    /**
     * Request storage permissions
     */
    public void requestStoragePermissions(Activity activity) {
        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting storage permissions: " + permissionsToRequest.size());
            ActivityCompat.requestPermissions(activity, 
                permissionsToRequest.toArray(new String[0]), 1001);
        }
    }
    
    /**
     * Check if a specific permission is granted
     */
    public boolean isPermissionGranted(String permission) {
        if (permission.equals("Overlay Permission")) {
            return hasOverlayPermission();
        } else if (permission.equals("Manage Storage Permission")) {
            return hasManageStoragePermission();
        } else {
            return ContextCompat.checkSelfPermission(context, permission) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
}