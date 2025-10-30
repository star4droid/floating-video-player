package com.floatingvideoplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for comprehensive file access across different Android versions
 * Handles MANAGE_EXTERNAL_STORAGE permission, SAF integration, and URI management
 */
public class FileAccessUtils {
    
    private static final String TAG = "FileAccessUtils";
    
    /**
     * Check if app has required permissions for file access
     */
    public static boolean hasRequiredPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, 
                "android.permission.READ_EXTERNAL_STORAGE") == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // No permissions needed for older versions
    }
    
    /**
     * Request storage permissions based on Android version
     */
    public static Intent getPermissionRequestIntent(Context context) {
        String packageName = context.getPackageName();
        Uri packageUri = Uri.parse("package:" + packageName);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use MANAGE_EXTERNAL_STORAGE
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(packageUri);
            return intent;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 - Use app settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(packageUri);
            return intent;
        }
        
        return null; // No permission request needed for older versions
    }
    
    /**
     * Get accessible external storage directories
     */
    public static List<File> getAccessibleStorageDirectories() {
        List<File> directories = new ArrayList<>();
        
        try {
            // Primary external storage
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File primary = Environment.getExternalStorageDirectory();
                if (primary != null && primary.exists()) {
                    directories.add(primary);
                }
            }
            
            // Secondary external storage (if available)
            File[] secondary = ContextCompat.getExternalFilesDirs(null, null);
            if (secondary != null) {
                for (File dir : secondary) {
                    if (dir != null && dir.exists()) {
                        File parent = dir.getParentFile();
                        if (parent != null && !directories.contains(parent)) {
                            directories.add(parent);
                        }
                    }
                }
            }
            
            // Public directories
            addPublicDirectories(directories);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting accessible directories", e);
        }
        
        return directories;
    }
    
    /**
     * Add public directories to the list
     */
    private static void addPublicDirectories(List<File> directories) {
        try {
            // Common public directories
            File[] publicDirs = {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            };
            
            for (File dir : publicDirs) {
                if (dir != null && dir.exists() && !directories.contains(dir)) {
                    directories.add(dir);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding public directories", e);
        }
    }
    
    /**
     * Convert file path to content URI
     */
    public static Uri getContentUriFromPath(Context context, String filePath) {
        if (filePath == null) return null;
        
        File file = new File(filePath);
        if (!file.exists()) return null;
        
        try {
            // Try MediaStore first (more reliable for media files)
            Uri contentUri = getContentUriFromMediaStore(context, file);
            if (contentUri != null) {
                return contentUri;
            }
            
            // Fallback to file URI
            return Uri.fromFile(file);
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting path to content URI: " + filePath, e);
            return Uri.fromFile(file); // Fallback to file URI
        }
    }
    
    /**
     * Get content URI from MediaStore
     */
    private static Uri getContentUriFromMediaStore(Context context, File file) {
        String[] projection = {MediaStore.Files.FileColumns._ID};
        String selection = MediaStore.Files.FileColumns.DATA + "=?";
        String[] selectionArgs = {file.getAbsolutePath()};
        
        try (Cursor cursor = context.getContentResolver().query(
            MediaStore.Files.CONTENT_EXTERNAL_URI, 
            projection, 
            selection, 
            selectionArgs, 
            null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                long id = cursor.getLong(idColumnIndex);
                return ContentUris.withAppendedId(MediaStore.Files.CONTENT_EXTERNAL_URI, id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore for: " + file.getAbsolutePath(), e);
        }
        
        return null;
    }
    
    /**
     * Convert content URI to file path
     */
    public static String getPathFromContentUri(Context context, Uri contentUri) {
        if (contentUri == null) return null;
        
        String path = null;
        
        // Handle file:// URIs
        if ("file".equals(contentUri.getScheme())) {
            path = contentUri.getPath();
        } else if ("content".equals(contentUri.getScheme())) {
            // Handle content:// URIs
            path = getPathFromContentUriMediaStore(context, contentUri);
            
            if (path == null) {
                // Fallback to manual query
                path = getPathFromContentUriManual(context, contentUri);
            }
        }
        
        return path;
    }
    
    /**
     * Get path from content URI using MediaStore
     */
    private static String getPathFromContentUriMediaStore(Context context, Uri contentUri) {
        try (Cursor cursor = context.getContentResolver().query(
            contentUri,
            new String[]{MediaStore.Files.FileColumns.DATA},
            null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                return cursor.getString(dataColumnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from content URI via MediaStore", e);
        }
        
        return null;
    }
    
    /**
     * Get path from content URI manually
     */
    private static String getPathFromContentUriManual(Context context, Uri contentUri) {
        try (Cursor cursor = context.getContentResolver().query(
            contentUri,
            null, null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                // Look for _data column
                int dataIndex = cursor.getColumnIndex("_data");
                if (dataIndex != -1) {
                    return cursor.getString(dataIndex);
                }
                
                // Look for data column
                dataIndex = cursor.getColumnIndex("data");
                if (dataIndex != -1) {
                    return cursor.getString(dataIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from content URI manually", e);
        }
        
        return null;
    }
    
    /**
     * Check if file is accessible
     */
    public static boolean isFileAccessible(String filePath) {
        if (filePath == null) return false;
        
        try {
            File file = new File(filePath);
            if (!file.exists()) return false;
            
            // Check if we can read the file
            if (!file.canRead()) return false;
            
            // For directories, check if we can list contents
            if (file.isDirectory() && !file.canRead()) return false;
            
            return true;
            
        } catch (SecurityException e) {
            Log.w(TAG, "Security exception accessing file: " + filePath, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking file accessibility: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Get file information safely
     */
    public static FileInfo getFileInfo(String filePath) {
        FileInfo fileInfo = new FileInfo();
        
        try {
            File file = new File(filePath);
            
            if (file.exists()) {
                fileInfo.name = file.getName();
                fileInfo.path = file.getAbsolutePath();
                fileInfo.size = file.length();
                fileInfo.isDirectory = file.isDirectory();
                fileInfo.canRead = file.canRead();
                fileInfo.canWrite = file.canWrite();
                fileInfo.lastModified = file.lastModified();
                fileInfo.accessible = true;
            } else {
                fileInfo.accessible = false;
                fileInfo.errorMessage = "File does not exist";
            }
            
        } catch (SecurityException e) {
            fileInfo.accessible = false;
            fileInfo.errorMessage = "Access denied (security exception)";
            Log.w(TAG, "Security exception accessing file: " + filePath, e);
        } catch (Exception e) {
            fileInfo.accessible = false;
            fileInfo.errorMessage = "Error: " + e.getMessage();
            Log.e(TAG, "Error getting file info: " + filePath, e);
        }
        
        return fileInfo;
    }
    
    /**
     * Copy file safely
     */
    public static boolean copyFile(String sourcePath, String destinationPath) {
        if (!isFileAccessible(sourcePath)) {
            Log.e(TAG, "Source file is not accessible: " + sourcePath);
            return false;
        }
        
        File source = new File(sourcePath);
        File destination = new File(destinationPath);
        
        // Ensure parent directory exists
        File parentDir = destination.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                Log.e(TAG, "Failed to create parent directory: " + parentDir.getAbsolutePath());
                return false;
            }
        }
        
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            
            Log.d(TAG, "File copied successfully: " + sourcePath + " -> " + destinationPath);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + sourcePath + " -> " + destinationPath, e);
            return false;
        }
    }
    
    /**
     * Delete file or directory recursively
     */
    public static boolean deleteFileOrDirectory(String filePath) {
        if (!isFileAccessible(filePath)) {
            Log.e(TAG, "File is not accessible: " + filePath);
            return false;
        }
        
        File file = new File(filePath);
        
        if (file.isDirectory()) {
            // Delete directory contents first
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    if (!deleteFileOrDirectory(childFile.getAbsolutePath())) {
                        Log.w(TAG, "Failed to delete: " + childFile.getAbsolutePath());
                    }
                }
            }
        }
        
        boolean deleted = file.delete();
        if (deleted) {
            Log.d(TAG, "File deleted successfully: " + filePath);
        } else {
            Log.e(TAG, "Failed to delete file: " + filePath);
        }
        
        return deleted;
    }
    
    /**
     * Check if storage access is restricted by manufacturer
     */
    public static boolean isStorageAccessRestricted() {
        // Some manufacturers (like Huawei, Xiaomi) may have additional restrictions
        // This is a simplified check - in real implementation you might check for
        // specific manufacturer restrictions or use DevicePolicyManager
        
        try {
            String manufacturer = Build.MANUFACTURER.toLowerCase();
            
            // These manufacturers are known to have storage restrictions
            String[] restrictedManufacturers = {
                "huawei", "xiaomi", "oppo", "vivo", "meizu"
            };
            
            for (String restricted : restrictedManufacturers) {
                if (manufacturer.contains(restricted)) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking manufacturer restrictions", e);
        }
        
        return false;
    }
    
    /**
     * Get storage access instructions for restricted manufacturers
     */
    public static String getStorageAccessInstructions(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        if (manufacturer.contains("xiaomi")) {
            return "On Xiaomi devices, enable 'All files access' in Settings > Apps > Floating Video Player > Permissions";
        } else if (manufacturer.contains("huawei")) {
            return "On Huawei devices, go to Settings > Privacy & security > File access and enable 'Floating Video Player'";
        } else if (manufacturer.contains("oppo")) {
            return "On OPPO devices, enable 'All files access' in Settings > Apps > Floating Video Player > Permissions";
        } else if (manufacturer.contains("vivo")) {
            return "On Vivo devices, enable 'All files access' in Settings > Privacy & security > File access > Floating Video Player";
        } else if (manufacturer.contains("meizu")) {
            return "On Meizu devices, enable 'All files access' in Settings > Apps > Floating Video Player > Permissions";
        } else {
            return "Enable 'All files access' permission in Settings > Apps > Floating Video Player > Permissions";
        }
    }
    
    /**
     * Show storage access instruction dialog
     */
    public static void showStorageAccessInstructions(Context context) {
        String instructions = getStorageAccessInstructions(context);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Storage Access Required");
        builder.setMessage(instructions);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    // Helper class for file information
    public static class FileInfo {
        public String name;
        public String path;
        public long size;
        public boolean isDirectory;
        public boolean canRead;
        public boolean canWrite;
        public long lastModified;
        public boolean accessible;
        public String errorMessage;
        
        @Override
        public String toString() {
            return "FileInfo{" +
                    "name='" + name + '\'' +
                    ", path='" + path + '\'' +
                    ", size=" + size +
                    ", accessible=" + accessible +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}