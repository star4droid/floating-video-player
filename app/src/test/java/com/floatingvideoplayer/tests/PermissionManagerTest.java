package com.floatingvideoplayer.tests;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.floatingvideoplayer.utils.PermissionManager;
import com.floatingvideoplayer.utils.StoragePermissionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for permission handling across Android versions
 */
@RunWith(AndroidJUnit4.class)
public class PermissionManagerTest {
    
    private Context context;
    private PermissionManager permissionManager;
    private StoragePermissionHelper storagePermissionHelper;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        permissionManager = new PermissionManager(context);
        storagePermissionHelper = new StoragePermissionHelper(context);
    }
    
    @Test
    public void testPermissionManagerInitialization() {
        assertNotNull("PermissionManager should be initialized", permissionManager);
        assertNotNull("StoragePermissionHelper should be initialized", storagePermissionHelper);
    }
    
    @Test
    public void testSystemAlertWindowPermission() {
        boolean hasPermission = permissionManager.checkSystemAlertWindowPermission();
        assertNotNull("System alert window permission check should not return null", hasPermission);
    }
    
    @Test
    public void testStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ granular permissions
            boolean hasReadVideo = permissionManager.checkPermission(permissionManager.READ_MEDIA_VIDEO);
            boolean hasReadAudio = permissionManager.checkPermission(permissionManager.READ_MEDIA_AUDIO);
            boolean hasReadImages = permissionManager.checkPermission(permissionManager.READ_MEDIA_IMAGES);
            
            assertNotNull("READ_MEDIA_VIDEO permission check should not be null", hasReadVideo);
            assertNotNull("READ_MEDIA_AUDIO permission check should not be null", hasReadAudio);
            assertNotNull("READ_MEDIA_IMAGES permission check should not be null", hasReadImages);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 MANAGE_EXTERNAL_STORAGE
            boolean hasManageStorage = permissionManager.checkPermission(
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            assertNotNull("MANAGE_EXTERNAL_STORAGE permission check should not be null", hasManageStorage);
        } else {
            // Android 10 and below legacy storage
            boolean hasRead = permissionManager.checkPermission(
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
            boolean hasWrite = permissionManager.checkPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            
            assertNotNull("READ_EXTERNAL_STORAGE permission check should not be null", hasRead);
            assertNotNull("WRITE_EXTERNAL_STORAGE permission check should not be null", hasWrite);
        }
    }
    
    @Test
    public void testNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotification = permissionManager.checkPermission(
                android.Manifest.permission.POST_NOTIFICATIONS);
            assertNotNull("POST_NOTIFICATIONS permission check should not be null", hasNotification);
        }
    }
    
    @Test
    public void testForegroundServicePermissions() {
        boolean hasForegroundService = permissionManager.checkPermission(
            android.Manifest.permission.FOREGROUND_SERVICE);
        boolean hasMediaPlayback = permissionManager.checkPermission(
            android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK);
        
        assertNotNull("FOREGROUND_SERVICE permission check should not be null", hasForegroundService);
        assertNotNull("FOREGROUND_SERVICE_MEDIA_PLAYBACK permission check should not be null", hasMediaPlayback);
    }
    
    @Test
    public void testBootReceiverPermissions() {
        boolean hasBootCompleted = permissionManager.checkPermission(
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED);
        assertNotNull("RECEIVE_BOOT_COMPLETED permission check should not be null", hasBootCompleted);
    }
    
    @Test
    public void testInternetPermission() {
        boolean hasInternet = permissionManager.checkPermission(
            android.Manifest.permission.INTERNET);
        assertNotNull("INTERNET permission check should not be null", hasInternet);
    }
    
    @Test
    public void testStoragePermissionHelper() {
        // Test storage permission helper initialization
        assertNotNull("StoragePermissionHelper context should not be null", storagePermissionHelper.getContext());
    }
    
    @Test
    public void testPermissionRequestCodes() {
        // Test that permission request codes are properly defined
        assertTrue("Request code should be valid", permissionManager.REQUEST_SYSTEM_ALERT_WINDOW > 0);
        assertTrue("Request code should be valid", permissionManager.REQUEST_STORAGE_PERMISSIONS > 0);
        assertTrue("Request code should be valid", permissionManager.REQUEST_NOTIFICATION_PERMISSIONS > 0);
    }
    
    @Test
    public void testAndroidVersionCompatibility() {
        // Test Android version detection
        int currentVersion = Build.VERSION.SDK_INT;
        assertTrue("Current Android version should be >= 26", currentVersion >= Build.VERSION_CODES.O);
        assertTrue("Target Android version should be supported", currentVersion <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }
    
    @Test
    public void testManifestPermissions() {
        try {
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            String[] requestedPermissions = packageManager.getPackageInfo(
                packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            
            assertNotNull("Package should have requested permissions", requestedPermissions);
            assertTrue("Should have requested SYSTEM_ALERT_WINDOW permission", 
                hasPermission(requestedPermissions, "android.permission.SYSTEM_ALERT_WINDOW"));
            assertTrue("Should have requested FOREGROUND_SERVICE_MEDIA_PLAYBACK permission", 
                hasPermission(requestedPermissions, "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"));
            assertTrue("Should have requested INTERNET permission", 
                hasPermission(requestedPermissions, "android.permission.INTERNET"));
        } catch (PackageManager.NameNotFoundException e) {
            fail("Package not found: " + e.getMessage());
        }
    }
    
    private boolean hasPermission(String[] permissions, String permission) {
        if (permissions == null) return false;
        for (String p : permissions) {
            if (permission.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
