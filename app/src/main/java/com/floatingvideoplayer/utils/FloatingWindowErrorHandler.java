package com.floatingvideoplayer.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced error handler for floating window components
 * Provides comprehensive error handling and recovery mechanisms
 */
public class FloatingWindowErrorHandler {
    
    private static final String TAG = "FloatingWindowErrorHandler";
    
    private Context context;
    private WindowManagerHelper windowManagerHelper;
    private AtomicBoolean isRecoveryMode = new AtomicBoolean(false);
    
    // Error callbacks
    private OnErrorListener errorListener;
    
    public interface OnErrorListener {
        void onError(String component, Exception error, boolean recoverable);
        void onRecoveryAttempt(String component, boolean success);
    }
    
    public FloatingWindowErrorHandler(Context context) {
        this.context = context;
        this.windowManagerHelper = new WindowManagerHelper(context);
    }
    
    /**
     * Handle window creation errors
     */
    public void handleWindowCreationError(String windowType, Exception error) {
        Log.e(TAG, "Window creation error for " + windowType, error);
        
        boolean recoverable = isRecoverableError(error);
        
        // Log error details
        logErrorDetails(windowType, error);
        
        // Show user-friendly error message
        showErrorMessage(windowType, recoverable);
        
        // Notify error listener
        if (errorListener != null) {
            errorListener.onError(windowType, error, recoverable);
        }
        
        // Attempt automatic recovery if possible
        if (recoverable) {
            attemptRecovery(windowType, error);
        }
    }
    
    /**
     * Handle touch event processing errors
     */
    public void handleTouchEventError(String operation, Exception error) {
        Log.w(TAG, "Touch event error during " + operation, error);
        
        // Touch events are usually not critical, so we don't show toast
        // Just log and continue operation
        if (errorListener != null) {
            errorListener.onError("touch_" + operation, error, true);
        }
    }
    
    /**
     * Handle permission-related errors
     */
    public void handlePermissionError(String permission, Exception error) {
        Log.e(TAG, "Permission error for " + permission, error);
        
        String message = "Permission required: " + getPermissionDescription(permission);
        showErrorMessage("Permission Error", message);
        
        // Request permission if needed
        requestPermissionIfNeeded(permission);
        
        if (errorListener != null) {
            errorListener.onError("permission_" + permission, error, true);
        }
    }
    
    /**
     * Handle resource cleanup errors
     */
    public void handleCleanupError(String component, Exception error) {
        Log.w(TAG, "Cleanup error for " + component, error);
        
        // Cleanup errors are usually non-critical
        // Just log and continue
        if (errorListener != null) {
            errorListener.onError("cleanup_" + component, error, true);
        }
    }
    
    /**
     * Handle service communication errors
     */
    public void handleServiceError(String operation, Exception error) {
        Log.e(TAG, "Service communication error during " + operation, error);
        
        boolean recoverable = isRecoverableServiceError(error);
        
        showServiceErrorMessage(operation, recoverable);
        
        if (errorListener != null) {
            errorListener.onError("service_" + operation, error, recoverable);
        }
        
        // For service errors, always attempt recovery
        attemptServiceRecovery(operation, error);
    }
    
    /**
     * Check if an error is recoverable
     */
    private boolean isRecoverableError(Exception error) {
        if (error == null) return false;
        
        String errorMessage = error.getMessage();
        if (errorMessage == null) return false;
        
        // Check for common recoverable error patterns
        return errorMessage.contains("permission") ||
               errorMessage.contains("window") ||
               errorMessage.contains("touch") ||
               errorMessage.contains("null") ||
               errorMessage.contains("layout") ||
               errorMessage.contains("surface");
    }
    
    /**
     * Check if service error is recoverable
     */
    private boolean isRecoverableServiceError(Exception error) {
        if (error == null) return false;
        
        String errorMessage = error.getMessage();
        if (errorMessage == null) return true; // Assume recoverable for null messages
        
        // Service errors are often recoverable due to timing issues
        return !errorMessage.contains("fatal") &&
               !errorMessage.contains("security") &&
               !errorMessage.contains("corrupt");
    }
    
    /**
     * Attempt automatic recovery from errors
     */
    private void attemptRecovery(String component, Exception error) {
        if (isRecoveryMode.get()) {
            Log.w(TAG, "Already in recovery mode, skipping recovery attempt");
            return;
        }
        
        isRecoveryMode.set(true);
        
        try {
            Log.d(TAG, "Attempting recovery for " + component);
            
            // Generic recovery steps
            Thread.sleep(500); // Brief pause before recovery
            
            // Check overlay permissions
            if (!windowManagerHelper.checkOverlayPermission()) {
                Log.w(TAG, "Overlay permission lost during recovery");
                requestOverlayPermission();
                return;
            }
            
            // Validate window manager state
            if (!validateWindowManagerState()) {
                Log.w(TAG, "Window manager state invalid during recovery");
                return;
            }
            
            // Recovery attempt successful
            Log.d(TAG, "Recovery successful for " + component);
            
            if (errorListener != null) {
                errorListener.onRecoveryAttempt(component, true);
            }
            
        } catch (Exception recoveryError) {
            Log.e(TAG, "Recovery failed for " + component, recoveryError);
            
            if (errorListener != null) {
                errorListener.onRecoveryAttempt(component, false);
            }
        } finally {
            isRecoveryMode.set(false);
        }
    }
    
    /**
     * Attempt service recovery
     */
    private void attemptServiceRecovery(String operation, Exception error) {
        // Service recovery typically involves restarting the service
        Log.d(TAG, "Service recovery not implemented for " + operation);
    }
    
    /**
     * Validate window manager state
     */
    private boolean validateWindowManagerState() {
        try {
            // Check if window manager is accessible
            android.view.WindowManager wm = (android.view.WindowManager) 
                context.getSystemService(Context.WINDOW_SERVICE);
            
            if (wm == null) {
                Log.e(TAG, "Window manager not accessible");
                return false;
            }
            
            // Get screen dimensions to validate state
            windowManagerHelper.getScreenDimensions();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Window manager validation failed", e);
            return false;
        }
    }
    
    /**
     * Show error message to user
     */
    private void showErrorMessage(String component, boolean recoverable) {
        String message = "Error in " + component;
        if (recoverable) {
            message += "\nAttempting automatic recovery...";
        } else {
            message += "\nPlease restart the app.";
        }
        
        showToast(message, recoverable ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
    }
    
    /**
     * Show service error message
     */
    private void showServiceErrorMessage(String operation, boolean recoverable) {
        String message = "Service error during " + operation;
        if (recoverable) {
            message += "\nRetrying...";
        }
        
        showToast(message, Toast.LENGTH_SHORT);
    }
    
    /**
     * Show toast message
     */
    private void showToast(String message, int duration) {
        try {
            Toast.makeText(context, message, duration).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast message", e);
        }
    }
    
    /**
     * Log detailed error information
     */
    private void logErrorDetails(String component, Exception error) {
        Log.e(TAG, "=== Error Details ===");
        Log.e(TAG, "Component: " + component);
        Log.e(TAG, "Error Type: " + error.getClass().getSimpleName());
        Log.e(TAG, "Error Message: " + error.getMessage());
        
        if (error.getCause() != null) {
            Log.e(TAG, "Cause: " + error.getCause().getMessage());
        }
        
        // Log stack trace
        StackTraceElement[] stackTrace = error.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            Log.e(TAG, "  at " + stackTrace[i].toString());
        }
        
        Log.e(TAG, "===================");
    }
    
    /**
     * Get permission description
     */
    private String getPermissionDescription(String permission) {
        switch (permission) {
            case "overlay":
                return "Draw over other apps";
            case "storage":
                return "File access";
            case "audio":
                return "Audio playback";
            default:
                return permission;
        }
    }
    
    /**
     * Request permission if needed
     */
    private void requestPermissionIfNeeded(String permission) {
        switch (permission) {
            case "overlay":
                requestOverlayPermission();
                break;
            // Add other permission requests as needed
        }
    }
    
    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        windowManagerHelper.requestOverlayPermission();
    }
    
    /**
     * Set error listener
     */
    public void setOnErrorListener(OnErrorListener listener) {
        this.errorListener = listener;
    }
    
    /**
     * Check if currently in recovery mode
     */
    public boolean isInRecoveryMode() {
        return isRecoveryMode.get();
    }
    
    /**
     * Force recovery mode (for testing)
     */
    public void setRecoveryMode(boolean recovery) {
        isRecoveryMode.set(recovery);
    }
}
