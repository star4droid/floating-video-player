package com.floatingvideoplayer.utils;

import android.content.Context;
import android.media.MediaDrm;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.ErrorMessageProvider;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive error handler for Media3 video player with support for
 * unsupported formats, playback failures, and recovery mechanisms
 */
public class Media3ErrorHandler implements ErrorMessageProvider<Player> {
    
    private static final String TAG = "Media3ErrorHandler";
    
    // Error types
    public enum ErrorType {
        FORMAT_UNSUPPORTED,
        NETWORK_ERROR,
        DRM_ERROR,
        PLAYBACK_ERROR,
        RESOURCE_ERROR,
        TIMEOUT_ERROR,
        PERMISSION_ERROR,
        UNKNOWN_ERROR
    }
    
    // Error recovery strategies
    public enum RecoveryStrategy {
        RETRY,
        SKIP_ITEM,
        IGNORE,
        STOP_PLAYBACK,
        RESTART_PLAYER
    }
    
    // Supported formats
    private static final Map<String, Set<String>> SUPPORTED_FORMATS = new HashMap<>();
    
    static {
        // Video formats
        SUPPORTED_FORMATS.put("video", Set.of(
            "video/mp4", "video/avi", "video/mov", "video/wmv", 
            "video/flv", "video/webm", "video/mkv", "video/3gpp"
        ));
        
        // Audio formats
        SUPPORTED_FORMATS.put("audio", Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/flac",
            "audio/aac", "audio/ogg", "audio/m4a", "audio/x-wav"
        ));
    }
    
    private final Context context;
    private final OnErrorCallback errorCallback;
    private final Map<ErrorType, RecoveryStrategy> recoveryStrategies;
    
    public interface OnErrorCallback {
        void onError(ErrorType type, String message, Throwable error);
        void onFormatUnsupported(String mimeType);
        void onRecoveryAttempt(RecoveryStrategy strategy, int attemptCount);
    }
    
    /**
     * Constructor
     */
    public Media3ErrorHandler(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.errorCallback = null;
        this.recoveryStrategies = new HashMap<>();
        
        // Set default recovery strategies
        setDefaultRecoveryStrategies();
    }
    
    /**
     * Constructor with callback
     */
    public Media3ErrorHandler(@NonNull Context context, @NonNull OnErrorCallback callback) {
        this.context = context.getApplicationContext();
        this.errorCallback = callback;
        this.recoveryStrategies = new HashMap<>();
        
        // Set default recovery strategies
        setDefaultRecoveryStrategies();
    }
    
    /**
     * Set default recovery strategies for each error type
     */
    private void setDefaultRecoveryStrategies() {
        recoveryStrategies.put(ErrorType.FORMAT_UNSUPPORTED, RecoveryStrategy.SKIP_ITEM);
        recoveryStrategies.put(ErrorType.NETWORK_ERROR, RecoveryStrategy.RETRY);
        recoveryStrategies.put(ErrorType.DRM_ERROR, RecoveryStrategy.IGNORE);
        recoveryStrategies.put(ErrorType.PLAYBACK_ERROR, RecoveryStrategy.RETRY);
        recoveryStrategies.put(ErrorType.RESOURCE_ERROR, RecoveryStrategy.IGNORE);
        recoveryStrategies.put(ErrorType.TIMEOUT_ERROR, RecoveryStrategy.RETRY);
        recoveryStrategies.put(ErrorType.PERMISSION_ERROR, RecoveryStrategy.STOP_PLAYBACK);
        recoveryStrategies.put(ErrorType.UNKNOWN_ERROR, RecoveryStrategy.RETRY);
    }
    
    /**
     * Set custom recovery strategy for error type
     */
    public void setRecoveryStrategy(@NonNull ErrorType errorType, @NonNull RecoveryStrategy strategy) {
        recoveryStrategies.put(errorType, strategy);
    }
    
    /**
     * Get recovery strategy for error type
     */
    @NonNull
    public RecoveryStrategy getRecoveryStrategy(@NonNull ErrorType errorType) {
        return recoveryStrategies.getOrDefault(errorType, RecoveryStrategy.IGNORE);
    }
    
    /**
     * Media3 ErrorMessageProvider implementation
     */
    @Override
    public CharSequence getErrorMessage(Player player) {
        if (player == null) {
            return "Player not available";
        }
        
        PlaybackException exception = player.getPlayerError();
        if (exception == null) {
            return "Unknown player error";
        }
        
        ErrorType errorType = determineErrorType(exception);
        String message = getUserFriendlyMessage(errorType, exception);
        
        // Log error for debugging
        Log.e(TAG, "Media3 playback error: " + message, exception);
        
        // Notify callback
        if (errorCallback != null) {
            errorCallback.onError(errorType, message, exception);
        }
        
        return message;
    }
    
    /**
     * Determine error type from Media3 exception
     */
    @NonNull
    private ErrorType determineErrorType(@NonNull PlaybackException exception) {
        String errorString = exception.getMessage();
        Throwable cause = exception.getCause();
        
        if (errorString != null) {
            errorString = errorString.toLowerCase();
            
            if (errorString.contains("unsupported") || errorString.contains("format")) {
                return ErrorType.FORMAT_UNSUPPORTED;
            } else if (errorString.contains("network") || errorString.contains("connect")) {
                return ErrorType.NETWORK_ERROR;
            } else if (errorString.contains("drm") || errorString.contains("license")) {
                return ErrorType.DRM_ERROR;
            } else if (errorString.contains("timeout") || errorString.contains("timed out")) {
                return ErrorType.TIMEOUT_ERROR;
            } else if (errorString.contains("permission") || errorString.contains("access")) {
                return ErrorType.PERMISSION_ERROR;
            } else if (errorString.contains("resource") || errorString.contains("memory")) {
                return ErrorType.RESOURCE_ERROR;
            }
        }
        
        if (cause instanceof IOException) {
            return ErrorType.NETWORK_ERROR;
        } else if (cause instanceof MediaDrm.MediaDrmStateException) {
            return ErrorType.DRM_ERROR;
        } else if (cause instanceof SecurityException) {
            return ErrorType.PERMISSION_ERROR;
        }
        
        return ErrorType.PLAYBACK_ERROR;
    }
    
    /**
     * Get user-friendly error message
     */
    @NonNull
    private String getUserFriendlyMessage(@NonNull ErrorType errorType, @NonNull PlaybackException exception) {
        switch (errorType) {
            case FORMAT_UNSUPPORTED:
                return "This file format is not supported by the player";
                
            case NETWORK_ERROR:
                return "Network connection error. Please check your internet connection";
                
            case DRM_ERROR:
                return "This content requires digital rights management (DRM) which is not available";
                
            case PLAYBACK_ERROR:
                return "Playback error occurred. The file may be corrupted or incompatible";
                
            case RESOURCE_ERROR:
                return "Insufficient resources to play this media";
                
            case TIMEOUT_ERROR:
                return "Playback timed out. The file may be too large or the connection is slow";
                
            case PERMISSION_ERROR:
                return "Permission denied to access this media file";
                
            default:
                return "An unexpected error occurred during playback";
        }
    }
    
    /**
     * Validate if a media file format is supported
     */
    public boolean isFormatSupported(@NonNull Uri uri, @NonNull String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        
        String category = getMimeTypeCategory(mimeType);
        if (category == null) {
            return false;
        }
        
        Set<String> supportedTypes = SUPPORTED_FORMATS.get(category);
        return supportedTypes != null && supportedTypes.contains(mimeType.toLowerCase());
    }
    
    /**
     * Get category of mime type (video, audio, or null)
     */
    @Nullable
    private String getMimeTypeCategory(@NonNull String mimeType) {
        String lowerMime = mimeType.toLowerCase();
        
        if (lowerMime.startsWith("video/")) {
            return "video";
        } else if (lowerMime.startsWith("audio/")) {
            return "audio";
        }
        
        return null;
    }
    
    /**
     * Validate media file using MediaExtractor
     */
    public boolean validateMediaFile(@NonNull Uri uri) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(context, uri, null);
            
            int trackCount = extractor.getTrackCount();
            boolean hasValidTracks = false;
            
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mimeType = format.getString(MediaFormat.KEY_MIME);
                
                if (mimeType != null && getMimeTypeCategory(mimeType) != null) {
                    hasValidTracks = true;
                    break;
                }
            }
            
            extractor.release();
            return hasValidTracks;
            
        } catch (IOException e) {
            Log.e(TAG, "Error validating media file: " + uri, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error validating media file: " + uri, e);
            return false;
        }
    }
    
    /**
     * Get supported formats information
     */
    @NonNull
    public Map<String, Set<String>> getSupportedFormats() {
        return new HashMap<>(SUPPORTED_FORMATS);
    }
    
    /**
     * Show error toast to user
     */
    public void showErrorToast(@NonNull ErrorType errorType) {
        String message = getUserFriendlyMessage(errorType, null);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Attempt to recover from error
     */
    public RecoveryStrategy attemptRecovery(@NonNull ErrorType errorType, int currentAttempt, int maxAttempts) {
        RecoveryStrategy strategy = getRecoveryStrategy(errorType);
        
        if (errorCallback != null) {
            errorCallback.onRecoveryAttempt(strategy, currentAttempt);
        }
        
        // Log recovery attempt
        Log.d(TAG, String.format("Attempting recovery for %s: %s (attempt %d/%d)", 
            errorType.name(), strategy.name(), currentAttempt, maxAttempts));
        
        return strategy;
    }
    
    /**
     * Check if format is supported with detailed validation
     */
    public boolean validateFormatWithDetails(@NonNull Uri uri, @NonNull String mimeType, 
                                           boolean showToastOnError) {
        boolean isSupported = isFormatSupported(uri, mimeType);
        
        if (!isSupported && showToastOnError) {
            showErrorToast(ErrorType.FORMAT_UNSUPPORTED);
            
            if (errorCallback != null) {
                errorCallback.onFormatUnsupported(mimeType);
            }
        }
        
        return isSupported;
    }
    
    /**
     * Get MIME type from URI if possible
     */
    @Nullable
    public String getMimeTypeFromUri(@NonNull Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        
        // If content resolver doesn't know the type, try to guess from file extension
        if (mimeType == null) {
            String path = uri.getPath();
            if (path != null) {
                String extension = getFileExtension(path);
                mimeType = guessMimeTypeFromExtension(extension);
            }
        }
        
        return mimeType;
    }
    
    /**
     * Get file extension from path
     */
    @Nullable
    private String getFileExtension(@NonNull String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }
    
    /**
     * Guess MIME type from file extension
     */
    @Nullable
    private String guessMimeTypeFromExtension(@Nullable String extension) {
        if (extension == null) {
            return null;
        }
        
        switch (extension) {
            case "mp4":
            case "m4v":
                return "video/mp4";
            case "avi":
                return "video/avi";
            case "mp3":
                return "audio/mp3";
            case "flac":
                return "audio/flac";
            case "wav":
                return "audio/wav";
            case "mov":
                return "video/quicktime";
            case "webm":
                return "video/webm";
            default:
                return null;
        }
    }
    
    /**
     * Build comprehensive error report
     */
    @NonNull
    public String buildErrorReport(@NonNull PlaybackException exception, @NonNull Uri uri) {
        StringBuilder report = new StringBuilder();
        report.append("Media3 Player Error Report\n");
        report.append("===========================\n\n");
        
        report.append("Error Type: ").append(determineErrorType(exception).name()).append("\n");
        report.append("Message: ").append(exception.getMessage()).append("\n");
        report.append("Uri: ").append(uri).append("\n");
        
        if (exception.getCause() != null) {
            report.append("Cause: ").append(exception.getCause().getClass().getSimpleName())
                  .append(": ").append(exception.getCause().getMessage()).append("\n");
        }
        
        String mimeType = getMimeTypeFromUri(uri);
        if (mimeType != null) {
            report.append("MIME Type: ").append(mimeType).append("\n");
            report.append("Format Supported: ").append(isFormatSupported(uri, mimeType)).append("\n");
        }
        
        report.append("Timestamp: ").append(new java.util.Date()).append("\n");
        
        return report.toString();
    }
}
