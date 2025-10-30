package com.floatingvideoplayer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver to handle system boot and package events.
 * Ensures the overlay service can restart when the device is rebooted.
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
            case Intent.ACTION_REBOOT:
                handleBootEvent(context);
                break;
                
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_PACKAGE_REPLACED:
                handlePackageReplaced(context);
                break;
                
            default:
                Log.d(TAG, "Unhandled action: " + action);
                break;
        }
    }
    
    /**
     * Handle device boot events
     */
    private void handleBootEvent(Context context) {
        Log.d(TAG, "Handling boot event");
        
        try {
            // Optionally restart the overlay service after boot
            // This depends on whether you want the overlay to persist across reboots
            
            // For now, we just log the event and let the user manually start the service
            Log.i(TAG, "Device boot completed, user can now start overlay service");
            
            // If you want automatic restart, uncomment below:
            // Intent serviceIntent = new Intent(context, OverlayService.class);
            // serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_SHOW_FILE_MANAGER);
            // 
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //     context.startForegroundService(serviceIntent);
            // } else {
            //     context.startService(serviceIntent);
            // }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling boot event", e);
        }
    }
    
    /**
     * Handle app package replacement
     */
    private void handlePackageReplaced(Context context) {
        Log.d(TAG, "Handling package replacement");
        
        try {
            String packageName = context.getPackageName();
            Log.i(TAG, "App package updated: " + packageName);
            
            // The app has been updated, ensure any necessary cleanup or restart
            // For now, we just log the event
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling package replacement", e);
        }
    }
}