package com.floatingvideoplayer.services;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.floatingvideoplayer.models.MediaFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for extracting metadata from media files
 * Supports both video and audio files with comprehensive metadata extraction
 */
public class MediaMetadataExtractor {
    
    private static final String TAG = "MediaMetadataExtractor";
    
    private Context context;
    
    public MediaMetadataExtractor(Context context) {
        this.context = context;
    }
    
    /**
     * Extract metadata from a media file
     */
    public MediaFile extractMetadata(String filePath) {
        MediaFile mediaFile = new MediaFile();
        
        try {
            File file = new File(filePath);
            mediaFile.setPath(filePath);
            mediaFile.setName(file.getName());
            mediaFile.setExtension(getFileExtension(filePath));
            mediaFile.setSize(file.length());
            mediaFile.setDateModified(file.lastModified());
            mediaFile.setDateAdded(System.currentTimeMillis());
            
            String extension = mediaFile.getExtension().toLowerCase();
            
            if (isVideoFile(extension)) {
                mediaFile.setType(MediaFile.MediaType.VIDEO);
                extractVideoMetadata(filePath, mediaFile);
            } else if (isAudioFile(extension)) {
                mediaFile.setType(MediaFile.MediaType.AUDIO);
                extractAudioMetadata(filePath, mediaFile);
            } else {
                mediaFile.setType(MediaFile.MediaType.UNKNOWN);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting metadata for: " + filePath, e);
        }
        
        return mediaFile;
    }
    
    /**
     * Extract video metadata
     */
    private void extractVideoMetadata(String filePath, MediaFile mediaFile) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(filePath);
                
                // Duration
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    mediaFile.setDuration(Long.parseLong(durationStr));
                }
                
                // Width and height
                String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                
                if (widthStr != null) {
                    mediaFile.setWidth(Integer.parseInt(widthStr));
                }
                if (heightStr != null) {
                    mediaFile.setHeight(Integer.parseInt(heightStr));
                }
                
                // Bitrate
                String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                if (bitrateStr != null) {
                    mediaFile.setBitrate(Integer.parseInt(bitrateStr) / 1000); // Convert to kbps
                }
                
                // Title
                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if (title != null && !title.isEmpty()) {
                    mediaFile.setTitle(title);
                }
                
                // Artist
                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (artist != null && !artist.isEmpty()) {
                    mediaFile.setArtist(artist);
                }
                
                // Album
                String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (album != null && !album.isEmpty()) {
                    mediaFile.setAlbum(album);
                }
                
                // Genre
                String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                if (genre != null && !genre.isEmpty()) {
                    // Store genre in title for now
                    if (mediaFile.getTitle().isEmpty()) {
                        mediaFile.setTitle(genre);
                    }
                }
                
                retriever.release();
                
            } else {
                // Fallback for older Android versions
                MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(new File(filePath)));
                if (player != null) {
                    mediaFile.setDuration(player.getDuration());
                    player.release();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video metadata for: " + filePath, e);
        }
    }
    
    /**
     * Extract audio metadata
     */
    private void extractAudioMetadata(String filePath, MediaFile mediaFile) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(filePath);
                
                // Duration
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    mediaFile.setDuration(Long.parseLong(durationStr));
                }
                
                // Title
                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if (title != null && !title.isEmpty()) {
                    mediaFile.setTitle(title);
                } else {
                    // If no title, use filename without extension
                    File file = new File(filePath);
                    String nameWithoutExt = file.getName();
                    int lastDot = nameWithoutExt.lastIndexOf('.');
                    if (lastDot > 0) {
                        nameWithoutExt = nameWithoutExt.substring(0, lastDot);
                    }
                    mediaFile.setTitle(nameWithoutExt);
                }
                
                // Artist
                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (artist != null && !artist.isEmpty()) {
                    mediaFile.setArtist(artist);
                }
                
                // Album
                String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (album != null && !album.isEmpty()) {
                    mediaFile.setAlbum(album);
                }
                
                // Bitrate
                String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                if (bitrateStr != null) {
                    mediaFile.setBitrate(Integer.parseInt(bitrateStr) / 1000); // Convert to kbps
                }
                
                // Year
                String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                if (year != null && !year.isEmpty()) {
                    // Store year in artist field for now
                    mediaFile.setArtist(mediaFile.getArtist() + " (" + year + ")");
                }
                
                retriever.release();
                
            } else {
                // Fallback for older Android versions
                MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(new File(filePath)));
                if (player != null) {
                    mediaFile.setDuration(player.getDuration());
                    player.release();
                }
                
                // Use filename as title
                File file = new File(filePath);
                String nameWithoutExt = file.getName();
                int lastDot = nameWithoutExt.lastIndexOf('.');
                if (lastDot > 0) {
                    nameWithoutExt = nameWithoutExt.substring(0, lastDot);
                }
                mediaFile.setTitle(nameWithoutExt);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting audio metadata for: " + filePath, e);
        }
    }
    
    /**
     * Check if file is a video file based on extension
     */
    private boolean isVideoFile(String extension) {
        String[] videoExtensions = {
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", 
            "3gp", "mpg", "mpeg", "mp2", "mpg", "mpv", "mp2v", "m2v"
        };
        
        for (String ext : videoExtensions) {
            if (extension.equals(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if file is an audio file based on extension
     */
    private boolean isAudioFile(String extension) {
        String[] audioExtensions = {
            "mp3", "flac", "wav", "aac", "ogg", "m4a", "wma", "opus", 
            "amr", "3ga", "ra", "ac3", "dts"
        };
        
        for (String ext : audioExtensions) {
            if (extension.equals(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get file extension from path
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || !filePath.contains(".")) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Extract metadata in background thread
     */
    public void extractMetadataAsync(String filePath, MetadataCallback callback) {
        new Thread(() -> {
            MediaFile mediaFile = extractMetadata(filePath);
            
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onMetadataExtracted(mediaFile);
                }
            });
        }).start();
    }
    
    /**
     * Extract metadata for multiple files
     */
    public Map<String, MediaFile> extractMetadataBatch(List<String> filePaths) {
        Map<String, MediaFile> metadataMap = new HashMap<>();
        
        for (String filePath : filePaths) {
            MediaFile metadata = extractMetadata(filePath);
            metadataMap.put(filePath, metadata);
        }
        
        return metadataMap;
    }
    
    /**
     * Get video format information
     */
    public String getVideoFormatInfo(MediaFile mediaFile) {
        StringBuilder info = new StringBuilder();
        
        if (!mediaFile.getResolution().equals("Unknown")) {
            info.append(mediaFile.getResolution());
        }
        
        if (mediaFile.getDuration() > 0) {
            if (info.length() > 0) info.append(", ");
            info.append(mediaFile.getFormattedDuration());
        }
        
        if (mediaFile.getBitrate() > 0) {
            if (info.length() > 0) info.append(", ");
            info.append(mediaFile.getBitrate()).append(" kbps");
        }
        
        return info.toString();
    }
    
    /**
     * Get audio format information
     */
    public String getAudioFormatInfo(MediaFile mediaFile) {
        StringBuilder info = new StringBuilder();
        
        if (!mediaFile.getArtist().isEmpty()) {
            info.append(mediaFile.getArtist());
        }
        
        if (!mediaFile.getAlbum().isEmpty()) {
            if (info.length() > 0) info.append(" - ");
            info.append(mediaFile.getAlbum());
        }
        
        if (mediaFile.getDuration() > 0) {
            if (info.length() > 0) info.append(", ");
            info.append(mediaFile.getFormattedDuration());
        }
        
        if (mediaFile.getBitrate() > 0) {
            if (info.length() > 0) info.append(", ");
            info.append(mediaFile.getBitrate()).append(" kbps");
        }
        
        return info.toString();
    }
    
    /**
     * Callback interface for async metadata extraction
     */
    public interface MetadataCallback {
        void onMetadataExtracted(MediaFile mediaFile);
    }
}