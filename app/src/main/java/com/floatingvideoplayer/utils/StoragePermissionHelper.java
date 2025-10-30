package com.floatingvideoplayer.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * Helper class for managing storage permissions, especially for Android 11+
 */
public class StoragePermissionHelper {
    
    private static final String TAG = "StoragePermissionHelper";
    
    /**
     * Check if app has MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public boolean hasManageStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true; // Not applicable for versions below R
        }
        
        try {
            boolean hasPermission = Environment.isExternalStorageManager();
            Log.d(TAG, "Manage storage permission: " + hasPermission);
            return hasPermission;
        } catch (Exception e) {
            Log.e(TAG, "Error checking manage storage permission", e);
            return false;
        }
    }
    
    /**
     * Check if app has legacy external storage read permission (Android 6-10)
     */
    public boolean hasLegacyExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // No runtime permissions needed
        }
        
        try {
            String permission = "android.permission.READ_EXTERNAL_STORAGE";
            return context.checkSelfPermission(permission) 
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking legacy storage permission", e);
            return false;
        }
    }
    
    /**
     * Check if app has granular media permissions (Android 13+)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public boolean hasGranularMediaPermissions(Context context) {
        try {
            boolean hasVideo = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) 
                              == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean hasAudio = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) 
                              == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean hasImages = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) 
                               == android.content.pm.PackageManager.PERMISSION_GRANTED;
            
            Log.d(TAG, "Granular media permissions - Video: " + hasVideo + 
                  ", Audio: " + hasAudio + ", Images: " + hasImages);
            return hasVideo || hasAudio || hasImages;
        } catch (Exception e) {
            Log.e(TAG, "Error checking granular media permissions", e);
            return false;
        }
    }
    
    /**
     * Check if app can access external storage (any form)
     */
    public boolean canAccessExternalStorage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasGranularMediaPermissions(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return hasManageStoragePermission(context) || hasLegacyExternalStoragePermission(context);
        } else {
            return hasLegacyExternalStoragePermission(context);
        }
    }
    
    /**
     * Get the appropriate storage permission intent for the current Android version
     */
    public Intent getStoragePermissionIntent(Context context) {
        String packageName = context.getPackageName();
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent intent = null;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use MANAGE_EXTERNAL_STORAGE
            intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(packageUri);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 - Use MANAGE_EXTERNAL_STORAGE permission doesn't exist,
            // so we use the general app settings
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(packageUri);
        }
        
        return intent;
    }
    
    /**
     * Get storage access status description
     */
    public String getStorageAccessStatus(Context context) {
        StringBuilder status = new StringBuilder();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            status.append("Android 13+ (Granular Media Permissions)\n");
            status.append("READ_MEDIA_VIDEO: ").append(
                hasGranularMediaPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) ? "✓" : "✗").append("\n");
            status.append("READ_MEDIA_AUDIO: ").append(
                hasGranularMediaPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) ? "✓" : "✗").append("\n");
            status.append("READ_MEDIA_IMAGES: ").append(
                hasGranularMediaPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) ? "✓" : "✗").append("\n");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            status.append("Android 11+ (All Files Access)\n");
            status.append("MANAGE_EXTERNAL_STORAGE: ").append(
                hasManageStoragePermission(context) ? "✓" : "✗").append("\n");
            status.append("Legacy External Storage: ").append(
                hasLegacyExternalStoragePermission(context) ? "✓" : "✗").append("\n");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            status.append("Android 6-10 (Legacy External Storage)\n");
            status.append("READ_EXTERNAL_STORAGE: ").append(
                hasLegacyExternalStoragePermission(context) ? "✓" : "✗").append("\n");
        } else {
            status.append("Android 5.x and below (No runtime permissions needed)\n");
        }
        
        return status.toString();
    }
    
    /**
     * Check specific granular media permission (Android 13+)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private boolean hasGranularMediaPermission(Context context, String permission) {
        try {
            return context.checkSelfPermission(permission) 
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking granular permission: " + permission, e);
            return false;
        }
    }
}