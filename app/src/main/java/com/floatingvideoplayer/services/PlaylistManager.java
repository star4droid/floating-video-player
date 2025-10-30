package com.floatingvideoplayer.services;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Advanced playlist and queue management system with shuffle,
 * repeat modes, and playlist creation capabilities
 */
public class PlaylistManager {
    
    private static final String TAG = "PlaylistManager";
    
    // Current playlist state
    private List<PlaylistItem> currentPlaylist = new ArrayList<>();
    private int currentIndex = 0;
    private boolean shuffleMode = false;
    private @RepeatMode int repeatMode = RepeatMode.OFF;
    
    // Shuffle state
    private List<Integer> shuffleOrder = new ArrayList<>();
    private int shufflePosition = 0;
    
    // Background executor for loading operations
    private final Executor backgroundExecutor = Executors.newFixedThreadPool(3);
    
    // Playlist change listeners
    private List<OnPlaylistChangeListener> playlistListeners = new ArrayList<>();
    private List<OnPlaybackChangeListener> playbackListeners = new ArrayList<>();
    
    public enum RepeatMode {
        OFF(0), ONE(1), ALL(2);
        
        private final int value;
        
        RepeatMode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static RepeatMode fromValue(int value) {
            for (RepeatMode mode : RepeatMode.values()) {
                if (mode.getValue() == value) {
                    return mode;
                }
            }
            return OFF;
        }
    }
    
    /**
     * Represents a media item with additional metadata for playlist management
     */
    public static class PlaylistItem {
        private final MediaItem mediaItem;
        private final long id;
        private final String displayName;
        private final long duration;
        private final long fileSize;
        private final String mimeType;
        private final Uri uri;
        
        public PlaylistItem(@NonNull MediaItem mediaItem, long id, String displayName, 
                          long duration, long fileSize, String mimeType, Uri uri) {
            this.mediaItem = mediaItem;
            this.id = id;
            this.displayName = displayName;
            this.duration = duration;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.uri = uri;
        }
        
        @NonNull
        public MediaItem getMediaItem() {
            return mediaItem;
        }
        
        public long getId() {
            return id;
        }
        
        @NonNull
        public String getDisplayName() {
            return displayName;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        @Nullable
        public String getMimeType() {
            return mimeType;
        }
        
        @NonNull
        public Uri getUri() {
            return uri;
        }
        
        public boolean isVideo() {
            return mimeType != null && mimeType.startsWith("video/");
        }
        
        public boolean isAudio() {
            return mimeType != null && mimeType.startsWith("audio/");
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PlaylistItem that = (PlaylistItem) obj;
            return id == that.id;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
        
        @Override
        public String toString() {
            return "PlaylistItem{" +
                    "displayName='" + displayName + '\'' +
                    ", duration=" + duration +
                    ", mimeType='" + mimeType + '\'' +
                    '}';
        }
    }
    
    /**
     * Playlist creation result
     */
    public static class PlaylistResult {
        private final boolean success;
        private final String message;
        private final List<PlaylistItem> items;
        
        private PlaylistResult(boolean success, String message, List<PlaylistItem> items) {
            this.success = success;
            this.message = message;
            this.items = items;
        }
        
        public static PlaylistResult success(List<PlaylistItem> items) {
            return new PlaylistResult(true, "Playlist created successfully", items);
        }
        
        public static PlaylistResult error(String message) {
            return new PlaylistResult(false, message, Collections.emptyList());
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        @NonNull
        public String getMessage() {
            return message;
        }
        
        @NonNull
        public List<PlaylistItem> getItems() {
            return items;
        }
    }
    
    /**
     * Create playlist from file URIs
     */
    public CompletableFuture<PlaylistResult> createPlaylistFromUris(@NonNull List<Uri> uris) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (uris.isEmpty()) {
                    return PlaylistResult.error("No files selected");
                }
                
                List<PlaylistItem> playlistItems = new ArrayList<>();
                Set<String> processedUris = new HashSet<>();
                
                for (Uri uri : uris) {
                    String uriString = uri.toString();
                    if (processedUris.contains(uriString)) {
                        continue; // Skip duplicates
                    }
                    processedUris.add(uriString);
                    
                    PlaylistItem item = createPlaylistItemFromUri(uri);
                    if (item != null) {
                        playlistItems.add(item);
                    }
                }
                
                if (playlistItems.isEmpty()) {
                    return PlaylistResult.error("No valid media files found");
                }
                
                return PlaylistResult.success(playlistItems);
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating playlist from URIs", e);
                return PlaylistResult.error("Failed to create playlist: " + e.getMessage());
            }
        }, backgroundExecutor);
    }
    
    /**
     * Create playlist from MediaStore query
     */
    public CompletableFuture<PlaylistResult> createPlaylistFromMediaStore(@NonNull ContentResolver contentResolver, 
                                                                         @Nullable String selection,
                                                                         @Nullable String[] selectionArgs,
                                                                         @Nullable String sortOrder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<PlaylistItem> playlistItems = new ArrayList<>();
                
                String[] projection = {
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DURATION,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATA
                };
                
                Cursor cursor = contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                );
                
                if (cursor != null) {
                    try {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                        int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                        int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION);
                        int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                        int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        
                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(idColumn);
                            String name = cursor.getString(nameColumn);
                            long duration = cursor.getLong(durationColumn);
                            long size = cursor.getLong(sizeColumn);
                            String mimeType = cursor.getString(mimeTypeColumn);
                            String dataPath = cursor.getString(dataColumn);
                            
                            // Only include video and audio files
                            if (mimeType != null && (mimeType.startsWith("video/") || mimeType.startsWith("audio/"))) {
                                Uri contentUri = ContentUris.withAppendedId(
                                    MediaStore.Files.getContentUri("external"), id);
                                
                                MediaItem mediaItem = createMediaItem(contentUri, name, mimeType);
                                if (mediaItem != null) {
                                    PlaylistItem playlistItem = new PlaylistItem(
                                        mediaItem, id, name, duration, size, mimeType, contentUri);
                                    playlistItems.add(playlistItem);
                                }
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
                
                if (playlistItems.isEmpty()) {
                    return PlaylistResult.error("No media files found");
                }
                
                return PlaylistResult.success(playlistItems);
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating playlist from MediaStore", e);
                return PlaylistResult.error("Failed to create playlist: " + e.getMessage());
            }
        }, backgroundExecutor);
    }
    
    /**
     * Set current playlist
     */
    public void setPlaylist(@NonNull List<PlaylistItem> playlistItems) {
        if (playlistItems.isEmpty()) {
            Log.w(TAG, "Attempting to set empty playlist");
            return;
        }
        
        currentPlaylist.clear();
        currentPlaylist.addAll(playlistItems);
        currentIndex = 0;
        shufflePosition = 0;
        
        updateShuffleOrder();
        notifyPlaylistChanged();
        notifyPlaybackChanged();
    }
    
    /**
     * Add item to playlist
     */
    public void addToPlaylist(@NonNull PlaylistItem item) {
        currentPlaylist.add(item);
        updateShuffleOrder();
        notifyPlaylistChanged();
    }
    
    /**
     * Add item to playlist at specific position
     */
    public void addToPlaylist(int position, @NonNull PlaylistItem item) {
        if (position < 0 || position > currentPlaylist.size()) {
            position = currentPlaylist.size();
        }
        
        currentPlaylist.add(position, item);
        updateShuffleOrder();
        notifyPlaylistChanged();
    }
    
    /**
     * Remove item from playlist
     */
    public void removeFromPlaylist(int position) {
        if (position < 0 || position >= currentPlaylist.size()) {
            return;
        }
        
        boolean isCurrentItem = position == currentIndex;
        currentPlaylist.remove(position);
        
        if (isCurrentItem) {
            // Current item was removed
            if (currentPlaylist.isEmpty()) {
                currentIndex = 0;
            } else if (position >= currentPlaylist.size()) {
                currentIndex = currentPlaylist.size() - 1;
            }
            // currentIndex remains at position
        } else if (position < currentIndex) {
            // Item before current item was removed
            currentIndex--;
        }
        
        updateShuffleOrder();
        notifyPlaylistChanged();
        if (isCurrentItem) {
            notifyPlaybackChanged();
        }
    }
    
    /**
     * Clear playlist
     */
    public void clearPlaylist() {
        currentPlaylist.clear();
        currentIndex = 0;
        shufflePosition = 0;
        notifyPlaylistChanged();
        notifyPlaybackChanged();
    }
    
    /**
     * Get current playlist
     */
    @NonNull
    public List<PlaylistItem> getCurrentPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }
    
    /**
     * Get current item
     */
    @Nullable
    public PlaylistItem getCurrentItem() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }
    
    /**
     * Get next item in playlist
     */
    @Nullable
    public PlaylistItem getNextItem() {
        int nextIndex = getNextIndex();
        if (nextIndex >= 0 && nextIndex < currentPlaylist.size()) {
            return currentPlaylist.get(nextIndex);
        }
        return null;
    }
    
    /**
     * Get previous item in playlist
     */
    @Nullable
    public PlaylistItem getPreviousItem() {
        int prevIndex = getPreviousIndex();
        if (prevIndex >= 0 && prevIndex < currentPlaylist.size()) {
            return currentPlaylist.get(prevIndex);
        }
        return null;
    }
    
    /**
     * Navigate to next item
     */
    @Nullable
    public PlaylistItem next() {
        int nextIndex = getNextIndex();
        if (nextIndex >= 0 && nextIndex < currentPlaylist.size()) {
            currentIndex = nextIndex;
            updateShufflePosition();
            notifyPlaybackChanged();
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }
    
    /**
     * Navigate to previous item
     */
    @Nullable
    public PlaylistItem previous() {
        int prevIndex = getPreviousIndex();
        if (prevIndex >= 0 && prevIndex < currentPlaylist.size()) {
            currentIndex = prevIndex;
            updateShufflePosition();
            notifyPlaybackChanged();
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }
    
    /**
     * Navigate to specific position
     */
    public boolean seekTo(int position) {
        if (position >= 0 && position < currentPlaylist.size()) {
            currentIndex = position;
            updateShufflePosition();
            notifyPlaybackChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Set shuffle mode
     */
    public void setShuffleMode(boolean enabled) {
        this.shuffleMode = enabled;
        updateShuffleOrder();
        updateShufflePosition();
        notifyPlaybackChanged();
    }
    
    /**
     * Get shuffle mode
     */
    public boolean isShuffleMode() {
        return shuffleMode;
    }
    
    /**
     * Set repeat mode
     */
    public void setRepeatMode(@RepeatMode RepeatMode mode) {
        this.repeatMode = mode;
        notifyPlaybackChanged();
    }
    
    /**
     * Get repeat mode
     */
    @NonNull
    public RepeatMode getRepeatMode() {
        return repeatMode;
    }
    
    /**
     * Get current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    /**
     * Get playlist size
     */
    public int getPlaylistSize() {
        return currentPlaylist.size();
    }
    
    /**
     * Get shuffle position
     */
    public int getShufflePosition() {
        return shufflePosition;
    }
    
    /**
     * Calculate next index based on shuffle and repeat modes
     */
    private int getNextIndex() {
        if (currentPlaylist.isEmpty()) {
            return -1;
        }
        
        if (shuffleMode && currentPlaylist.size() > 1) {
            shufflePosition = (shufflePosition + 1) % shuffleOrder.size();
            return shuffleOrder.get(shufflePosition);
        } else {
            int nextIndex = currentIndex + 1;
            
            if (repeatMode == RepeatMode.ONE) {
                return currentIndex;
            } else if (nextIndex >= currentPlaylist.size()) {
                return repeatMode == RepeatMode.ALL ? 0 : -1;
            }
            
            return nextIndex;
        }
    }
    
    /**
     * Calculate previous index based on shuffle and repeat modes
     */
    private int getPreviousIndex() {
        if (currentPlaylist.isEmpty()) {
            return -1;
        }
        
        if (shuffleMode && currentPlaylist.size() > 1) {
            shufflePosition = (shufflePosition - 1 + shuffleOrder.size()) % shuffleOrder.size();
            return shuffleOrder.get(shufflePosition);
        } else {
            int prevIndex = currentIndex - 1;
            
            if (repeatMode == RepeatMode.ONE) {
                return currentIndex;
            } else if (prevIndex < 0) {
                return repeatMode == RepeatMode.ALL ? currentPlaylist.size() - 1 : -1;
            }
            
            return prevIndex;
        }
    }
    
    /**
     * Update shuffle order
     */
    private void updateShuffleOrder() {
        shuffleOrder.clear();
        for (int i = 0; i < currentPlaylist.size(); i++) {
            shuffleOrder.add(i);
        }
        
        if (shuffleMode && currentPlaylist.size() > 1) {
            // Fisher-Yates shuffle
            for (int i = shuffleOrder.size() - 1; i > 0; i--) {
                int j = (int) (Math.random() * (i + 1));
                int temp = shuffleOrder.get(i);
                shuffleOrder.set(i, shuffleOrder.get(j));
                shuffleOrder.set(j, temp);
            }
            
            // Find current item position in shuffle order
            updateShufflePosition();
        }
    }
    
    /**
     * Update shuffle position based on current index
     */
    private void updateShufflePosition() {
        if (shuffleMode && !shuffleOrder.isEmpty()) {
            for (int i = 0; i < shuffleOrder.size(); i++) {
                if (shuffleOrder.get(i) == currentIndex) {
                    shufflePosition = i;
                    break;
                }
            }
        }
    }
    
    /**
     * Create playlist item from URI
     */
    @Nullable
    private PlaylistItem createPlaylistItemFromUri(@NonNull Uri uri) {
        try {
            // Use MediaStore to get file information
            String mimeType = getContext().getContentResolver().getType(uri);
            
            // Extract filename from URI
            String displayName = uri.getLastPathSegment();
            if (displayName != null) {
                int lastSlash = displayName.lastIndexOf('/');
                if (lastSlash >= 0) {
                    displayName = displayName.substring(lastSlash + 1);
                }
            }
            
            MediaItem mediaItem = createMediaItem(uri, displayName, mimeType);
            if (mediaItem == null) {
                return null;
            }
            
            return new PlaylistItem(
                mediaItem, 0, displayName, 0, 0, mimeType, uri);
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating playlist item from URI: " + uri, e);
            return null;
        }
    }
    
    /**
     * Create MediaItem from URI and metadata
     */
    @Nullable
    private MediaItem createMediaItem(@NonNull Uri uri, @Nullable String displayName, 
                                    @Nullable String mimeType) {
        try {
            MediaItem.Builder builder = new MediaItem.Builder()
                .setUri(uri);
            
            if (displayName != null) {
                builder.setMediaMetadata(new MediaMetadata.Builder()
                    .setTitle(displayName)
                    .build());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating MediaItem from URI: " + uri, e);
            return null;
        }
    }
    
    // Notification methods
    
    private void notifyPlaylistChanged() {
        for (OnPlaylistChangeListener listener : playlistListeners) {
            listener.onPlaylistChanged(new ArrayList<>(currentPlaylist), currentIndex);
        }
    }
    
    private void notifyPlaybackChanged() {
        for (OnPlaybackChangeListener listener : playbackListeners) {
            listener.onPlaybackChanged(getCurrentItem(), currentIndex);
        }
    }
    
    /**
     * Add playlist change listener
     */
    public void addOnPlaylistChangeListener(@NonNull OnPlaylistChangeListener listener) {
        if (!playlistListeners.contains(listener)) {
            playlistListeners.add(listener);
        }
    }
    
    /**
     * Remove playlist change listener
     */
    public void removeOnPlaylistChangeListener(@NonNull OnPlaylistChangeListener listener) {
        playlistListeners.remove(listener);
    }
    
    /**
     * Add playback change listener
     */
    public void addOnPlaybackChangeListener(@NonNull OnPlaybackChangeListener listener) {
        if (!playbackListeners.contains(listener)) {
            playbackListeners.add(listener);
        }
    }
    
    /**
     * Remove playback change listener
     */
    public void removeOnPlaybackChangeListener(@NonNull OnPlaybackChangeListener listener) {
        playbackListeners.remove(listener);
    }
    
    // Listener interfaces
    
    public interface OnPlaylistChangeListener {
        void onPlaylistChanged(List<PlaylistItem> playlist, int currentIndex);
    }
    
    public interface OnPlaybackChangeListener {
        void onPlaybackChanged(@Nullable PlaylistItem currentItem, int currentIndex);
    }
}
