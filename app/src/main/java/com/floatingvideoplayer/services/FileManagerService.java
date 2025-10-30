package com.floatingvideoplayer.services;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.floatingvideoplayer.models.MediaFile;
import com.floatingvideoplayer.utils.StoragePermissionHelper;

/**
 * FileManagerService provides comprehensive file access using Storage Access Framework (SAF)
 * and handles MANAGE_EXTERNAL_STORAGE permission for direct file system access.
 * Supports recursive directory traversal, file filtering, and thumbnail generation.
 */
public class FileManagerService {
    
    private static final String TAG = "FileManagerService";
    
    // Supported media formats
    private static final Set<String> VIDEO_FORMATS = new HashSet<>(Arrays.asList(
        "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp", "mpg", "mpeg"
    ));
    
    private static final Set<String> AUDIO_FORMATS = new HashSet<>(Arrays.asList(
        "mp3", "flac", "wav", "aac", "ogg", "m4a", "wma", "opus", "amr"
    ));
    
    private static final Set<String> ALL_MEDIA_FORMATS = new HashSet<>();
    
    static {
        ALL_MEDIA_FORMATS.addAll(VIDEO_FORMATS);
        ALL_MEDIA_FORMATS.addAll(AUDIO_FORMATS);
    }
    
    private Context context;
    private StoragePermissionHelper permissionHelper;
    private ExecutorService executorService;
    
    // Cache for frequently accessed directories
    private File homeDirectory;
    private File downloadsDirectory;
    private File moviesDirectory;
    private File musicDirectory;
    private File picturesDirectory;
    
    public interface MediaFilesCallback {
        void onMediaFilesLoaded(List<MediaFile> mediaFiles);
        void onError(String error);
    }
    
    public interface ThumbnailCallback {
        void onThumbnailGenerated(String filePath, byte[] thumbnailData);
        void onError(String error);
    }
    
    public FileManagerService(Context context) {
        this.context = context;
        this.permissionHelper = new StoragePermissionHelper();
        this.executorService = Executors.newFixedThreadPool(4);
        
        initializeDirectories();
    }
    
    /**
     * Initialize common directory paths
     */
    private void initializeDirectories() {
        try {
            homeDirectory = Environment.getExternalStorageDirectory();
            
            // Initialize common media directories
            downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            moviesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing directories", e);
        }
    }
    
    /**
     * Check if service has required permissions
     */
    public boolean hasRequiredPermissions() {
        return permissionHelper.canAccessExternalStorage(context);
    }
    
    /**
     * Get files from a directory with filtering options
     */
    public List<File> getFilesFromDirectory(File directory, FileFilter filter) {
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }
        
        try {
            File[] files = directory.listFiles(filter);
            if (files == null) {
                return new ArrayList<>();
            }
            
            List<File> fileList = Arrays.asList(files);
            
            // Sort: directories first, then files, both alphabetically
            List<File> directories = new ArrayList<>();
            List<File> regularFiles = new ArrayList<>();
            
            for (File file : fileList) {
                if (file.isDirectory()) {
                    directories.add(file);
                } else {
                    regularFiles.add(file);
                }
            }
            
            java.util.Collections.sort(directories);
            java.util.Collections.sort(regularFiles);
            
            List<File> result = new ArrayList<>();
            result.addAll(directories);
            result.addAll(regularFiles);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting files from directory: " + directory.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all media files from external storage using MediaStore
     */
    public void getAllMediaFilesFromMediaStore(MediaFilesCallback callback) {
        executorService.execute(() -> {
            List<MediaStoreFiles> mediaFiles = new ArrayList<>();
            
            try {
                // Query videos
                mediaFiles.addAll(queryMediaStoreFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video"));
                
                // Query audio files
                mediaFiles.addAll(queryMediaStoreFiles(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio"));
                
                // Convert to MediaFile objects
                List<MediaFile> result = convertToMediaFiles(mediaFiles);
                
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onMediaFilesLoaded(result));
                
            } catch (Exception e) {
                Log.e(TAG, "Error querying MediaStore", e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onError("Error querying media files"));
            }
        });
    }
    
    /**
     * Query MediaStore for files
     */
    private List<MediaStoreFiles> queryMediaStoreFiles(Uri contentUri, String type) {
        List<MediaStoreFiles> results = new ArrayList<>();
        
        String[] projection;
        
        if ("video".equals(type)) {
            projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            };
        } else {
            projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
            };
        }
        
        try (Cursor cursor = context.getContentResolver().query(
            contentUri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndex(projection[0]);
                int nameColumn = cursor.getColumnIndex(projection[1]);
                int dataColumn = cursor.getColumnIndex(projection[2]);
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String data = cursor.getString(dataColumn);
                    
                    if (name != null && data != null) {
                        Uri contentUriFile = ContentUris.withAppendedId(contentUri, id);
                        results.add(new MediaStoreFiles(name, data, contentUriFile, type));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore: " + type, e);
        }
        
        return results;
    }
    
    /**
     * Convert MediaStore files to MediaFile objects
     */
    private List<MediaFile> convertToMediaFiles(List<MediaStoreFiles> storeFiles) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        
        for (MediaStoreFiles storeFile : storeFiles) {
            try {
                String fileName = storeFile.name;
                String extension = getFileExtension(fileName);
                
                MediaFile.MediaType mediaType = getMediaTypeFromExtension(extension);
                
                if (mediaType != MediaFile.MediaType.UNKNOWN) {
                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setPath(storeFile.data);
                    mediaFile.setName(fileName);
                    mediaFile.setType(mediaType);
                    mediaFile.setExtension(extension);
                    mediaFile.setContentUri(storeFile.contentUri);
                    mediaFile.setIsFromMediaStore(true);
                    
                    mediaFiles.add(mediaFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting file: " + storeFile.name, e);
            }
        }
        
        return mediaFiles;
    }
    
    /**
     * Generate thumbnail for a media file
     */
    public void generateThumbnail(String filePath, ThumbnailCallback callback) {
        executorService.execute(() -> {
            try {
                if (isVideoFile(filePath)) {
                    generateVideoThumbnail(filePath, callback);
                } else if (isAudioFile(filePath)) {
                    generateAudioThumbnail(filePath, callback);
                } else {
                    throw new Exception("Unsupported media type for thumbnail");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating thumbnail for: " + filePath, e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onError("Failed to generate thumbnail"));
            }
        });
    }
    
    /**
     * Generate video thumbnail using MediaMetadataRetriever
     */
    private void generateVideoThumbnail(String filePath, ThumbnailCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                retriever.setDataSource(filePath);
                
                // Extract thumbnail at 1 second or center of video
                long duration = Long.parseLong(retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION));
                long extractTime = Math.min(1000, duration / 2);
                
                Bitmap bitmap = retriever.getFrameAtTime(extractTime * 1000, 
                    android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                
                retriever.release();
                
                if (bitmap != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byte[] thumbnailData = byteArrayOutputStream.toByteArray();
                    
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onThumbnailGenerated(filePath, thumbnailData));
                    return;
                }
            }
            
            throw new Exception("Failed to extract video frame");
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating video thumbnail", e);
            throw e;
        }
    }
    
    /**
     * Generate audio thumbnail (album art placeholder)
     */
    private void generateAudioThumbnail(String filePath, ThumbnailCallback callback) {
        try {
            // For now, return a default audio icon as thumbnail
            // In a real implementation, you might extract album art from ID3 tags
            
            android.graphics.Bitmap defaultIcon = createDefaultAudioIcon();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            defaultIcon.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
            byte[] thumbnailData = byteArrayOutputStream.toByteArray();
            
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onThumbnailGenerated(filePath, thumbnailData));
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating audio thumbnail", e);
            throw e;
        }
    }
    
    /**
     * Create a default audio icon bitmap
     */
    private Bitmap createDefaultAudioIcon() {
        // Create a simple audio icon as a bitmap
        // This is a simplified implementation
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLUE);
        
        int size = 100;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        
        // Draw a simple circle with audio note symbol
        canvas.drawCircle(size/2f, size/2f, size/2f - 10, paint);
        
        return bitmap;
    }
    
    /**
     * Check if file format is supported
     */
    public boolean isSupportedMediaFormat(String fileName) {
        String extension = getFileExtension(fileName);
        return ALL_MEDIA_FORMATS.contains(extension.toLowerCase());
    }
    
    /**
     * Check if file is a video file
     */
    public boolean isVideoFile(String fileName) {
        String extension = getFileExtension(fileName);
        return VIDEO_FORMATS.contains(extension.toLowerCase());
    }
    
    /**
     * Check if file is an audio file
     */
    public boolean isAudioFile(String fileName) {
        String extension = getFileExtension(fileName);
        return AUDIO_FORMATS.contains(extension.toLowerCase());
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Get media type from file extension
     */
    private MediaFile.MediaType getMediaTypeFromExtension(String extension) {
        if (VIDEO_FORMATS.contains(extension.toLowerCase())) {
            return MediaFile.MediaType.VIDEO;
        } else if (AUDIO_FORMATS.contains(extension.toLowerCase())) {
            return MediaFile.MediaType.AUDIO;
        }
        return MediaFile.MediaType.UNKNOWN;
    }
    
    /**
     * Get common media directories
     */
    public File[] getMediaDirectories() {
        List<File> directories = new ArrayList<>();
        
        if (downloadsDirectory != null && downloadsDirectory.exists()) {
            directories.add(downloadsDirectory);
        }
        if (moviesDirectory != null && moviesDirectory.exists()) {
            directories.add(moviesDirectory);
        }
        if (musicDirectory != null && musicDirectory.exists()) {
            directories.add(musicDirectory);
        }
        if (picturesDirectory != null && picturesDirectory.exists()) {
            directories.add(picturesDirectory);
        }
        
        return directories.toArray(new File[0]);
    }
    
    /**
     * Recursively search for media files in a directory
     */
    public void searchMediaFilesRecursively(File directory, int maxDepth, MediaFilesCallback callback) {
        executorService.execute(() -> {
            List<MediaFile> results = new ArrayList<>();
            searchMediaFilesRecursively(directory, maxDepth, 0, results);
            
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onMediaFilesLoaded(results));
        });
    }
    
    /**
     * Internal recursive search implementation
     */
    private void searchMediaFilesRecursively(File directory, int maxDepth, int currentDepth, List<MediaFile> results) {
        if (currentDepth > maxDepth || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        searchMediaFilesRecursively(file, maxDepth, currentDepth + 1, results);
                    } else if (isSupportedMediaFormat(file.getName())) {
                        MediaFile mediaFile = new MediaFile();
                        mediaFile.setPath(file.getAbsolutePath());
                        mediaFile.setName(file.getName());
                        mediaFile.setType(getMediaTypeFromExtension(getFileExtension(file.getName())));
                        mediaFile.setExtension(getFileExtension(file.getName()));
                        mediaFile.setIsFromMediaStore(false);
                        
                        results.add(mediaFile);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in recursive search: " + directory.getAbsolutePath(), e);
        }
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Helper class for MediaStore files
    private static class MediaStoreFiles {
        String name;
        String data;
        Uri contentUri;
        String type;
        
        MediaStoreFiles(String name, String data, Uri contentUri, String type) {
            this.name = name;
            this.data = data;
            this.contentUri = contentUri;
            this.type = type;
        }
    }
}