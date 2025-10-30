package com.floatingvideoplayer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver for handling notification actions
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NotificationActionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received notification action: " + action);
        
        if (action == null) {
            Log.w(TAG, "Received null action");
            return;
        }
        
        try {
            Intent serviceIntent = new Intent(context, OverlayService.class);
            
            switch (action) {
                case "ACTION_TOGGLE_OVERLAY":
                    serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_TOGGLE_OVERLAY);
                    break;
                    
                case "ACTION_SHOW_FILE_MANAGER":
                    serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_SHOW_FILE_MANAGER);
                    break;
                    
                case "ACTION_SHOW_VIDEO_PLAYER":
                    serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_SHOW_VIDEO_PLAYER);
                    break;
                    
                case "ACTION_STOP_SERVICE":
                    serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_STOP_SERVICE);
                    break;
                    
                default:
                    Log.w(TAG, "Unknown notification action: " + action);
                    return;
            }
            
            if (action.equals("ACTION_STOP_SERVICE")) {
                context.stopService(serviceIntent);
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification action: " + action, e);
        }
    }
}