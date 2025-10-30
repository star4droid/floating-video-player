package com.floatingvideoplayer.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.floatingvideoplayer.R;
import com.floatingvideoplayer.services.OverlayService;
import com.floatingvideoplayer.utils.PermissionManager;
import com.floatingvideoplayer.utils.StoragePermissionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for Floating Video Player app.
 * Handles permissions, service management, and app entry point.
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    // Permission request codes
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int OVERLAY_PERMISSION_CODE = 1002;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 1003;
    
    // Permission launchers
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;
    private ActivityResultLauncher<Intent[]> multiplePermissionsLauncher;
    
    // UI Components
    private Button btnStartOverlay;
    private Button btnStopOverlay;
    private Button btnCheckPermissions;
    private TextView tvPermissionStatus;
    private TextView tvServiceStatus;
    
    private PermissionManager permissionManager;
    private StoragePermissionHelper storagePermissionHelper;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initComponents();
        setupUI();
        setupPermissionLaunchers();
        checkInitialPermissions();
    }
    
    /**
     * Initialize components and managers
     */
    private void initComponents() {
        permissionManager = new PermissionManager(this);
        storagePermissionHelper = new StoragePermissionHelper();
        mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "Components initialized");
    }
    
    /**
     * Setup UI components and click listeners
     */
    private void setupUI() {
        btnStartOverlay = findViewById(R.id.btn_start_overlay);
        btnStopOverlay = findViewById(R.id.btn_stop_overlay);
        btnCheckPermissions = findViewById(R.id.btn_check_permissions);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        tvServiceStatus = findViewById(R.id.tv_service_status);
        
        btnStartOverlay.setOnClickListener(v -> startOverlayService());
        btnStopOverlay.setOnClickListener(v -> stopOverlayService());
        btnCheckPermissions.setOnClickListener(v -> checkAllPermissions());
        
        // Update UI initially
        updateUI();
    }
    
    /**
     * Setup permission result launchers
     */
    private void setupPermissionLaunchers() {
        // Overlay permission launcher
        overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                    updateUI();
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Manage storage permission launcher
        manageStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (storagePermissionHelper.hasManageStoragePermission(this)) {
                    Toast.makeText(this, "Storage access permission granted", Toast.LENGTH_SHORT).show();
                    updateUI();
                } else {
                    Toast.makeText(this, "Storage access permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Multiple permissions launcher
        multiplePermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (String permission : result.keySet()) {
                    if (!result.get(permission)) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show();
                }
                updateUI();
            }
        );
    }
    
    /**
     * Check initial permissions on app startup
     */
    private void checkInitialPermissions() {
        Log.d(TAG, "Checking initial permissions");
        
        List<String> neededPermissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check overlay permission
            if (!Settings.canDrawOverlays(this)) {
                neededPermissions.add("Overlay Permission");
            }
        }
        
        // Check storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        // Request manage storage permission for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!storagePermissionHelper.hasManageStoragePermission(this)) {
                neededPermissions.add("Manage Storage Permission");
            }
        }
        
        if (!neededPermissions.isEmpty()) {
            requestRequiredPermissions();
        }
    }
    
    /**
     * Request all required permissions
     */
    private void requestRequiredPermissions() {
        Log.d(TAG, "Requesting required permissions");
        
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Add runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        // Request other permissions that need special handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                overlayPermissionLauncher.launch(overlayIntent);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!storagePermissionHelper.hasManageStoragePermission(this)) {
                Intent storageIntent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                manageStoragePermissionLauncher.launch(storageIntent);
            }
        }
        
        // Request runtime permissions if any
        if (!permissionsToRequest.isEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
    
    /**
     * Start the overlay service
     */
    private void startOverlayService() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Please grant all required permissions first", Toast.LENGTH_LONG).show();
            requestRequiredPermissions();
            return;
        }
        
        Log.d(TAG, "Starting overlay service");
        
        Intent serviceIntent = new Intent(this, OverlayService.class);
        serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_SHOW_FILE_MANAGER);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, "Overlay service started", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    /**
     * Stop the overlay service
     */
    private void stopOverlayService() {
        Log.d(TAG, "Stopping overlay service");
        
        Intent serviceIntent = new Intent(this, OverlayService.class);
        serviceIntent.putExtra(OverlayService.EXTRA_ACTION, OverlayService.ACTION_STOP_SERVICE);
        
        stopService(serviceIntent);
        
        Toast.makeText(this, "Overlay service stopped", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    /**
     * Check all permissions and update UI accordingly
     */
    private void checkAllPermissions() {
        updateUI();
        
        StringBuilder status = new StringBuilder();
        status.append("Permissions Status:\n\n");
        
        // Check overlay permission
        boolean hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        status.append("Overlay Permission: ").append(hasOverlay ? "✓" : "✗").append("\n");
        
        // Check storage permissions
        boolean hasStorage = checkStoragePermission();
        status.append("Storage Permission: ").append(hasStorage ? "✓" : "✗").append("\n");
        
        // Check manage storage permission
        boolean hasManageStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || 
                                 storagePermissionHelper.hasManageStoragePermission(this);
        status.append("Manage Storage: ").append(hasManageStorage ? "✓" : "✗").append("\n");
        
        tvPermissionStatus.setText(status.toString());
        
        // Check service status
        boolean serviceRunning = OverlayService.isServiceRunning(this);
        tvServiceStatus.setText("Service Status: " + (serviceRunning ? "Running" : "Stopped"));
    }
    
    /**
     * Check if app has required permissions
     */
    private boolean hasRequiredPermissions() {
        boolean hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean hasStorage = checkStoragePermission();
        boolean hasManageStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || 
                                 storagePermissionHelper.hasManageStoragePermission(this);
        
        return hasOverlay && hasStorage && hasManageStorage;
    }
    
    /**
     * Check storage permissions
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                   == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // No permissions needed for older versions
        }
    }
    
    /**
     * Update UI based on current state
     */
    private void updateUI() {
        mainHandler.post(() -> {
            boolean hasPermissions = hasRequiredPermissions();
            boolean serviceRunning = OverlayService.isServiceRunning(this);
            
            btnStartOverlay.setEnabled(hasPermissions && !serviceRunning);
            btnStopOverlay.setEnabled(serviceRunning);
            
            if (!hasPermissions) {
                btnStartOverlay.setText("Grant Permissions First");
            } else {
                btnStartOverlay.setText(serviceRunning ? "Service Running" : "Start Floating Player");
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(TAG, "Permission result received: " + requestCode);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Permissions are required for the app to function properly", 
                              Toast.LENGTH_LONG).show();
            }
        }
        
        updateUI();
    }
}