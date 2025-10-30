package com.floatingvideoplayer.services;

import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.upstream.DefaultDataSource;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Advanced Media3 ExoPlayer implementation with playlist management,
 * audio/video playback, and notification controls support.
 */
public class Media3ExoPlayer implements Player.Listener {
    
    private final Context context;
    private final ExoPlayer exoPlayer;
    private final Handler mainHandler;
    private final Executor mainExecutor;
    
    // Playback state
    private boolean isPrepared = false;
    private boolean isShuffleMode = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;
    
    // Current playlist
    private List<MediaItem> playlist = new ArrayList<>();
    private List<Integer> shuffledIndices = new ArrayList<>();
    private int currentShuffledPosition = 0;
    
    // Playback listeners
    private OnPlaylistChangeListener playlistChangeListener;
    private OnPlaybackStateChangeListener playbackStateChangeListener;
    private OnErrorListener errorListener;
    
    /**
     * Constructor initializes ExoPlayer with Media3 configuration
     */
    public Media3ExoPlayer(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.mainExecutor = Runnable::run;
        
        // Initialize ExoPlayer with Media3 configuration
        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(context);
        
        this.exoPlayer = playerBuilder
                .setMediaSourceFactory(createMediaSourceFactory())
                .setAudioAttributes(createAudioAttributes(), true)
                .build();
        
        // Add listener for player events
        exoPlayer.addListener(this);
        
        // Set up repeat modes
        updateRepeatMode();
    }
    
    /**
     * Creates MediaSource factory for handling various formats
     */
    private ProgressiveMediaSource.Factory createMediaSourceFactory() {
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
        return new ProgressiveMediaSource.Factory(dataSourceFactory);
    }
    
    /**
     * Creates audio attributes for proper audio handling
     */
    private AudioAttributes createAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
    }
    
    /**
     * Load a single media item
     */
    public void loadMediaItem(@NonNull MediaItem mediaItem) {
        playlist.clear();
        playlist.add(mediaItem);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        isPrepared = true;
    }
    
    /**
     * Load a playlist of media items
     */
    public void loadPlaylist(@NonNull List<MediaItem> mediaItems) {
        if (mediaItems.isEmpty()) {
            return;
        }
        
        playlist.clear();
        playlist.addAll(mediaItems);
        
        // Prepare player with playlist
        exoPlayer.setMediaItems(mediaItems);
        exoPlayer.prepare();
        
        // Reset shuffle state
        updateShuffleState();
        updateRepeatMode();
        
        isPrepared = true;
        notifyPlaylistChanged();
    }
    
    /**
     * Add media item to current playlist
     */
    public void addMediaItem(@NonNull MediaItem mediaItem) {
        playlist.add(mediaItem);
        
        if (isPrepared) {
            exoPlayer.addMediaItem(mediaItem);
            notifyPlaylistChanged();
        }
    }
    
    /**
     * Add media item at specific position
     */
    public void addMediaItem(int position, @NonNull MediaItem mediaItem) {
        if (position < 0 || position > playlist.size()) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        
        playlist.add(position, mediaItem);
        
        if (isPrepared) {
            exoPlayer.addMediaItem(position, mediaItem);
            notifyPlaylistChanged();
        }
    }
    
    /**
     * Remove media item from playlist
     */
    public void removeMediaItem(int position) {
        if (position < 0 || position >= playlist.size()) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        
        playlist.remove(position);
        
        if (isPrepared) {
            exoPlayer.removeMediaItem(position);
            notifyPlaylistChanged();
        }
    }
    
    /**
     * Clear entire playlist
     */
    public void clearPlaylist() {
        playlist.clear();
        
        if (isPrepared) {
            exoPlayer.clearMediaItems();
        }
        
        shuffledIndices.clear();
        currentShuffledPosition = 0;
        notifyPlaylistChanged();
    }
    
    // Playback controls
    
    public void play() {
        if (!exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            exoPlayer.play();
        }
    }
    
    public void pause() {
        exoPlayer.pause();
    }
    
    public void playPause() {
        if (exoPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }
    
    public void seekTo(long positionMs) {
        if (positionMs >= 0 && positionMs <= getDuration()) {
            exoPlayer.seekTo(positionMs);
        }
    }
    
    public void seekToNext() {
        if (getCurrentMediaItem() != null) {
            exoPlayer.seekToNext();
        }
    }
    
    public void seekToPrevious() {
        if (getCurrentMediaItem() != null) {
            exoPlayer.seekToPrevious();
        }
    }
    
    public void next() {
        if (isShuffleMode) {
            navigateToNextShuffled();
        } else {
            exoPlayer.seekToNext();
        }
    }
    
    public void previous() {
        if (isShuffleMode) {
            navigateToPreviousShuffled();
        } else {
            exoPlayer.seekToPrevious();
        }
    }
    
    // Volume and speed controls
    
    public void setVolume(float volume) {
        volume = Math.max(0.0f, Math.min(1.0f, volume));
        exoPlayer.setVolume(volume);
    }
    
    public float getVolume() {
        return exoPlayer.getVolume();
    }
    
    public void setPlaybackSpeed(float speed) {
        speed = Math.max(0.25f, Math.min(2.0f, speed));
        exoPlayer.setPlaybackSpeed(speed);
    }
    
    public float getPlaybackSpeed() {
        return exoPlayer.getPlaybackParameters().speed;
    }
    
    // Shuffle and repeat modes
    
    public void setShuffleModeEnabled(boolean shuffleEnabled) {
        this.isShuffleMode = shuffleEnabled;
        exoPlayer.setShuffleModeEnabled(shuffleEnabled);
        updateShuffleState();
    }
    
    public boolean isShuffleModeEnabled() {
        return isShuffleMode;
    }
    
    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        this.repeatMode = repeatMode;
        exoPlayer.setRepeatMode(repeatMode);
        updateRepeatMode();
    }
    
    @Player.RepeatMode
    public int getRepeatMode() {
        return repeatMode;
    }
    
    // Getters for player state
    
    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }
    
    public boolean isPrepared() {
        return isPrepared && exoPlayer.getPlaybackState() != Player.STATE_IDLE;
    }
    
    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }
    
    public long getDuration() {
        return exoPlayer.getDuration();
    }
    
    public int getCurrentMediaItemIndex() {
        return exoPlayer.getCurrentMediaItemIndex();
    }
    
    @Nullable
    public MediaItem getCurrentMediaItem() {
        int index = exoPlayer.getCurrentMediaItemIndex();
        if (index >= 0 && index < playlist.size()) {
            return playlist.get(index);
        }
        return null;
    }
    
    public int getPlaylistSize() {
        return playlist.size();
    }
    
    @Nullable
    public MediaItem getMediaItemAt(int position) {
        if (position >= 0 && position < playlist.size()) {
            return playlist.get(position);
        }
        return null;
    }
    
    // Internal methods
    
    private void updateShuffleState() {
        if (isShuffleMode && playlist.size() > 1) {
            shuffledIndices.clear();
            for (int i = 0; i < playlist.size(); i++) {
                shuffledIndices.add(i);
            }
            // Fisher-Yates shuffle
            for (int i = shuffledIndices.size() - 1; i > 0; i--) {
                int j = (int) (Math.random() * (i + 1));
                int temp = shuffledIndices.get(i);
                shuffledIndices.set(i, shuffledIndices.get(j));
                shuffledIndices.set(j, temp);
            }
            currentShuffledPosition = 0;
        } else {
            shuffledIndices.clear();
        }
    }
    
    private void updateRepeatMode() {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                // No additional logic needed
                break;
            case Player.REPEAT_MODE_ONE:
                // ExoPlayer handles this automatically
                break;
            case Player.REPEAT_MODE_ALL:
                // ExoPlayer handles this automatically
                break;
        }
    }
    
    private void navigateToNextShuffled() {
        if (shuffledIndices.size() > 1) {
            currentShuffledPosition = (currentShuffledPosition + 1) % shuffledIndices.size();
            int originalPosition = shuffledIndices.get(currentShuffledPosition);
            exoPlayer.seekTo(originalPosition, 0);
        }
    }
    
    private void navigateToPreviousShuffled() {
        if (shuffledIndices.size() > 1) {
            currentShuffledPosition = (currentShuffledPosition - 1 + shuffledIndices.size()) % shuffledIndices.size();
            int originalPosition = shuffledIndices.get(currentShuffledPosition);
            exoPlayer.seekTo(originalPosition, 0);
        }
    }
    
    private void notifyPlaylistChanged() {
        if (playlistChangeListener != null) {
            mainHandler.post(() -> playlistChangeListener.onPlaylistChanged(playlist));
        }
    }
    
    private void notifyPlaybackStateChanged() {
        if (playbackStateChangeListener != null) {
            mainHandler.post(() -> playbackStateChangeListener.onPlaybackStateChanged(
                    exoPlayer.getPlaybackState(),
                    exoPlayer.isPlaying(),
                    exoPlayer.getCurrentPosition(),
                    exoPlayer.getDuration()
            ));
        }
    }
    
    // Listener interfaces
    
    public interface OnPlaylistChangeListener {
        void onPlaylistChanged(List<MediaItem> newPlaylist);
    }
    
    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(int playbackState, boolean isPlaying, long position, long duration);
    }
    
    public interface OnErrorListener {
        void onError(PlaybackException error);
    }
    
    public void setOnPlaylistChangeListener(OnPlaylistChangeListener listener) {
        this.playlistChangeListener = listener;
    }
    
    public void setOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        this.playbackStateChangeListener = listener;
    }
    
    public void setOnErrorListener(OnErrorListener listener) {
        this.errorListener = listener;
    }
    
    // Player.Listener implementation
    
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        notifyPlaybackStateChanged();
        
        if (playbackState == Player.STATE_ENDED) {
            // Auto-play next video in queue
            if (repeatMode != Player.REPEAT_MODE_ONE) {
                // Check if this is the last item
                int nextIndex = getCurrentMediaItemIndex() + 1;
                if (nextIndex < playlist.size()) {
                    // There's another item, play it
                    exoPlayer.seekTo(nextIndex, 0);
                    exoPlayer.play();
                }
            }
        }
    }
    
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        notifyPlaybackStateChanged();
    }
    
    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, @Player.MediaItemTransitionReason int reason) {
        // Update current position for shuffled mode
        if (isShuffleMode) {
            int originalPosition = getCurrentMediaItemIndex();
            for (int i = 0; i < shuffledIndices.size(); i++) {
                if (shuffledIndices.get(i) == originalPosition) {
                    currentShuffledPosition = i;
                    break;
                }
            }
        }
        
        notifyPlaybackStateChanged();
    }
    
    @Override
    public void onPlayerError(PlaybackException error) {
        if (errorListener != null) {
            mainHandler.post(() -> errorListener.onError(error));
        }
    }
    
    @Override
    public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
        this.repeatMode = repeatMode;
        notifyPlaybackStateChanged();
    }
    
    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        this.isShuffleMode = shuffleModeEnabled;
        updateShuffleState();
        notifyPlaylistChanged();
        notifyPlaybackStateChanged();
    }
    
    // Lifecycle methods
    
    public void onStart() {
        if (exoPlayer != null) {
            exoPlayer.play();
        }
    }
    
    public void onPause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }
    
    public void onStop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }
    
    public void onDestroy() {
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        playlistChangeListener = null;
        playbackStateChangeListener = null;
        errorListener = null;
    }
    
    /**
     * Get the underlying ExoPlayer instance
     */
    @NonNull
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
    
    /**
     * Check if current media item is audio-only
     */
    public boolean isCurrentItemAudioOnly() {
        MediaItem currentItem = getCurrentMediaItem();
        if (currentItem == null) {
            return false;
        }
        
        MediaMetadata metadata = currentItem.mediaMetadata;
        if (metadata == null) {
            return false;
        }
        
        return metadata.mediaType == MediaMetadata.MEDIA_TYPE_AUDIO ||
               metadata.folderType == MediaMetadata.FOLDER_TYPE_ALBUMS ||
               metadata.folderType == MediaMetadata.FOLDER_TYPE_PLAYLISTS;
    }
}
