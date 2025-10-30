package com.floatingvideoplayer.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.floatingvideoplayer.models.MediaFile;
import com.floatingvideoplayer.ui.FileBrowserWindow;
import com.floatingvideoplayer.ui.DraggableVideoPlayerWindow;
import com.floatingvideoplayer.utils.FileAccessUtils;
import com.floatingvideoplayer.utils.StoragePermissionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for integrating file manager functionality with video player
 * Provides a unified API for media file selection and playback
 */
public class FileManagerPlayerIntegration {
    
    private static final String TAG = "FileManagerPlayerIntegration";
    
    private Context context;
    private FileManagerService fileManagerService;
    private FileBrowserWindow fileBrowserWindow;
    private StoragePermissionHelper permissionHelper;
    private DraggableVideoPlayerWindow videoPlayerWindow;
    
    // Callbacks for integration events
    public interface FileSelectionCallback {
        void onFileSelected(MediaFile mediaFile);
        void onFilesSelected(List<MediaFile> mediaFiles);
        void onSelectionCancelled();
    }
    
    public interface PlayerStateCallback {
        void onPlayerCreated();
        void onPlayerDestroyed();
        void onMediaChanged(MediaFile mediaFile);
    }
    
    public FileManagerPlayerIntegration(Context context) {
        this.context = context;
        this.fileManagerService = new FileManagerService(context);
        this.fileBrowserWindow = new FileBrowserWindow(context);
        this.permissionHelper = new StoragePermissionHelper();
    }
    
    /**
     * Initialize the integration service
     */
    public void initialize() {
        Log.d(TAG, "Initializing file manager player integration");
        
        // Check permissions on initialization
        if (!permissionHelper.canAccessExternalStorage(context)) {
            Log.w(TAG, "Storage permissions not granted");
            showPermissionDialog();
        }
        
        // Pre-load common directories
        fileManagerService.getAllMediaFilesFromMediaStore(new FileManagerService.MediaFilesCallback() {
            @Override
            public void onMediaFilesLoaded(List<MediaFile> mediaFiles) {
                Log.d(TAG, "Pre-loaded " + mediaFiles.size() + " media files from MediaStore");
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Error pre-loading media files: " + error);
            }
        });
    }
    
    /**
     * Show file browser with auto-close after selection
     */
    public void showFileBrowser(FileSelectionCallback callback) {
        if (!permissionHelper.canAccessExternalStorage(context)) {
            showPermissionDialog();
            return;
        }
        
        fileBrowserWindow.show();
        
        // This is a simplified implementation - in a real app you would
        // implement proper callbacks from FileBrowserWindow
        if (callback != null) {
            // Set up callback handling
            // callback.onFilesSelected(selectedFiles);
        }
    }
    
    /**
     * Show file browser for multiple file selection (playlist creation)
     */
    public void showFileBrowserForPlaylist(FileSelectionCallback callback) {
        showFileBrowser(callback);
        
        // Set up multi-selection mode
        // This would need to be implemented in FileBrowserWindow
    }
    
    /**
     * Open video file in the floating player
     */
    public boolean openVideoFile(String videoPath) {
        if (!FileAccessUtils.isFileAccessible(videoPath)) {
            Toast.makeText(context, "Video file is not accessible", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            // Get or create video player window
            if (videoPlayerWindow == null) {
                videoPlayerWindow = new DraggableVideoPlayerWindow(context);
            }
            
            // Create intent to start video player
            Intent intent = new Intent(context, DraggableVideoPlayerWindow.class);
            intent.putExtra("video_path", videoPath);
            intent.putExtra("auto_play", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            
            Log.d(TAG, "Opened video file: " + videoPath);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening video file: " + videoPath, e);
            Toast.makeText(context, "Failed to open video", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Open multiple video files as playlist
     */
    public boolean openVideoPlaylist(List<String> videoPaths) {
        if (videoPaths == null || videoPaths.isEmpty()) {
            return false;
        }
        
        try {
            // Create intent for playlist
            Intent intent = new Intent(context, DraggableVideoPlayerWindow.class);
            intent.putStringArrayListExtra("playlist_paths", new ArrayList<>(videoPaths));
            intent.putExtra("auto_play", true);
            intent.putExtra("play_from_start", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            
            Log.d(TAG, "Opened playlist with " + videoPaths.size() + " videos");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening video playlist", e);
            Toast.makeText(context, "Failed to open playlist", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Toggle file browser visibility
     */
    public void toggleFileBrowser() {
        if (fileBrowserWindow.isVisible()) {
            fileBrowserWindow.hide();
        } else {
            showFileBrowser(null);
        }
    }
    
    /**
     * Hide both file browser and video player
     */
    public void hideAll() {
        if (fileBrowserWindow != null && fileBrowserWindow.isVisible()) {
            fileBrowserWindow.hide();
        }
        
        if (videoPlayerWindow != null) {
            videoPlayerWindow.hide();
        }
    }
    
    /**
     * Get recent media files
     */
    public void getRecentMediaFiles(FileManagerService.MediaFilesCallback callback) {
        fileManagerService.getAllMediaFilesFromMediaStore(callback);
    }
    
    /**
     * Search for media files in a directory
     */
    public void searchMediaFiles(String directoryPath, int maxDepth, 
                                FileManagerService.MediaFilesCallback callback) {
        if (!FileAccessUtils.isFileAccessible(directoryPath)) {
            callback.onError("Directory not accessible");
            return;
        }
        
        fileManagerService.searchMediaFilesRecursively(
            new java.io.File(directoryPath), maxDepth, callback);
    }
    
    /**
     * Generate thumbnail for media file
     */
    public void generateThumbnail(String filePath, FileManagerService.ThumbnailCallback callback) {
        fileManagerService.generateThumbnail(filePath, callback);
    }
    
    /**
     * Check if file is supported by the player
     */
    public boolean isSupportedMediaFile(String filePath) {
        if (!FileAccessUtils.isFileAccessible(filePath)) {
            return false;
        }
        
        return fileManagerService.isSupportedMediaFormat(
            new java.io.File(filePath).getName());
    }
    
    /**
     * Get file information with accessibility check
     */
    public FileAccessUtils.FileInfo getFileInfo(String filePath) {
        return FileAccessUtils.getFileInfo(filePath);
    }
    
    /**
     * Show permission dialog
     */
    private void showPermissionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Storage Permission Required");
        builder.setMessage("This app needs access to your files to play videos. Please grant storage permissions.");
        builder.setPositiveButton("Grant Permission", (dialog, which) -> {
            Intent permissionIntent = permissionHelper.getStoragePermissionIntent(context);
            if (permissionIntent != null) {
                context.startActivity(permissionIntent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Check if permissions are available
     */
    public boolean hasRequiredPermissions() {
        return permissionHelper.canAccessExternalStorage(context);
    }
    
    /**
     * Get storage access status
     */
    public String getStorageAccessStatus() {
        return permissionHelper.getStorageAccessStatus(context);
    }
    
    /**
     * Check if this is a restricted device (manufacturer-specific storage limitations)
     */
    public boolean isRestrictedDevice() {
        return FileAccessUtils.isStorageAccessRestricted();
    }
    
    /**
     * Get storage access instructions for the current device
     */
    public String getStorageAccessInstructions() {
        return FileAccessUtils.getStorageAccessInstructions(context);
    }
    
    /**
     * Show storage access instructions
     */
    public void showStorageAccessInstructions() {
        FileAccessUtils.showStorageAccessInstructions(context);
    }
    
    /**
     * Auto-select video files from a directory and play
     */
    public void playVideosFromDirectory(String directoryPath) {
        if (!FileAccessUtils.isFileAccessible(directoryPath)) {
            Toast.makeText(context, "Directory not accessible", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // This would search for video files and start playback
        // Implementation depends on FileManagerService capabilities
        Toast.makeText(context, "Scanning directory for videos...", Toast.LENGTH_SHORT).show();
        
        // Simplified implementation
        java.io.File directory = new java.io.File(directoryPath);
        List<String> videoPaths = new ArrayList<>();
        
        if (directory.exists() && directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (fileManagerService.isVideoFile(file.getName())) {
                        videoPaths.add(file.getAbsolutePath());
                    }
                }
            }
        }
        
        if (!videoPaths.isEmpty()) {
            openVideoPlaylist(videoPaths);
        } else {
            Toast.makeText(context, "No video files found in directory", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Quick access to common media directories
     */
    public void openMediaDirectory(int directoryType) {
        java.io.File[] directories = fileManagerService.getMediaDirectories();
        
        if (directories.length > 0 && directoryType < directories.length) {
            showFileBrowser(null);
            // In a full implementation, you would navigate to the specific directory
        } else {
            Toast.makeText(context, "Media directory not found", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (fileBrowserWindow != null) {
            fileBrowserWindow.destroy();
        }
        
        if (fileManagerService != null) {
            fileManagerService.shutdown();
        }
        
        Log.d(TAG, "File manager integration cleaned up");
    }
    
    /**
     * Get file browser window instance
     */
    public FileBrowserWindow getFileBrowserWindow() {
        return fileBrowserWindow;
    }
    
    /**
     * Get video player window instance
     */
    public DraggableVideoPlayerWindow getVideoPlayerWindow() {
        return videoPlayerWindow;
    }
    
    /**
     * Check if file browser is visible
     */
    public boolean isFileBrowserVisible() {
        return fileBrowserWindow != null && fileBrowserWindow.isVisible();
    }
    
    /**
     * Common media directory types
     */
    public static class MediaDirectory {
        public static final int DOWNLOADS = 0;
        public static final int MOVIES = 1;
        public static final int MUSIC = 2;
        public static final int PICTURES = 3;
        public static final int DCIM = 4;
    }
}