package com.floatingvideoplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.floatingvideoplayer.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Draggable and resizable file manager window
 */
public class DraggableFileManagerWindow {
    
    private static final String TAG = "DraggableFileManagerWindow";
    
    private static final int MIN_WINDOW_WIDTH = 300;
    private static final int MIN_WINDOW_HEIGHT = 400;
    private static final int MAX_WINDOW_WIDTH = 800;
    private static final int MAX_WINDOW_HEIGHT = 1000;
    
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View fileManagerView;
    
    // Window state
    private boolean isVisible = false;
    private boolean isDragging = false;
    private boolean isResizing = false;
    
    // Drag and resize variables
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private int resizeCorner = -1;
    
    // File system
    private File currentDirectory;
    private List<File> fileList;
    private ArrayAdapter<String> fileAdapter;
    private OnVideoSelectedListener videoSelectedListener;
    
    // Views
    private TextView pathTextView;
    private ListView fileListView;
    private ImageButton upButton;
    private ImageButton refreshButton;
    private ImageButton homeButton;
    private ImageButton minimizeButton;
    private ImageButton closeButton;
    
    // Resize handles
    private FrameLayout resizeHandleTopLeft;
    private FrameLayout resizeHandleTopRight;
    private FrameLayout resizeHandleBottomLeft;
    private FrameLayout resizeHandleBottomRight;
    private View resizeHandleLeft;
    private View resizeHandleRight;
    private View resizeHandleTop;
    private View resizeHandleBottom;
    
    public DraggableFileManagerWindow(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.currentDirectory = Environment.getExternalStorageDirectory();
        this.fileList = new ArrayList<>();
    }
    
    /**
     * Create and show the floating file manager window
     */
    public void show() {
        if (isVisible) {
            hide();
            return;
        }
        
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            fileManagerView = inflater.inflate(R.layout.floating_filemanager_layout, null);
            
            // Initialize window parameters
            layoutParams = createWindowLayoutParams();
            
            // Initialize views
            initializeViews();
            setupControls();
            setupResizeHandles();
            setupTouchListeners();
            
            // Load initial directory
            loadDirectory(currentDirectory);
            
            // Add to window manager
            windowManager.addView(fileManagerView, layoutParams);
            isVisible = true;
            
            Log.d(TAG, "Draggable file manager window shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing file manager window", e);
            Toast.makeText(context, "Failed to show file manager", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hide the floating file manager window
     */
    public void hide() {
        if (!isVisible || fileManagerView == null) return;
        
        try {
            windowManager.removeView(fileManagerView);
            fileManagerView = null;
            isVisible = false;
            
            Log.d(TAG, "Draggable file manager window hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding file manager window", e);
        }
    }
    
    /**
     * Create window layout parameters for draggable/resizable window
     */
    private WindowManager.LayoutParams createWindowLayoutParams() {
        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                500, // Default width
                600, // Default height
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 50;
        params.y = 100;
        
        return params;
    }
    
    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Find views
        pathTextView = fileManagerView.findViewById(R.id.tv_current_path);
        fileListView = fileManagerView.findViewById(R.id.lv_file_list);
        upButton = fileManagerView.findViewById(R.id.btn_up_directory);
        refreshButton = fileManagerView.findViewById(R.id.btn_refresh);
        homeButton = fileManagerView.findViewById(R.id.btn_home);
        minimizeButton = fileManagerView.findViewById(R.id.btn_minimize_filemanager);
        closeButton = fileManagerView.findViewById(R.id.btn_close_filemanager);
        
        // Resize handles
        resizeHandleTopLeft = fileManagerView.findViewById(R.id.resize_handle_top_left);
        resizeHandleTopRight = fileManagerView.findViewById(R.id.resize_handle_top_right);
        resizeHandleBottomLeft = fileManagerView.findViewById(R.id.resize_handle_bottom_left);
        resizeHandleBottomRight = fileManagerView.findViewById(R.id.resize_handle_bottom_right);
        resizeHandleLeft = fileManagerView.findViewById(R.id.resize_handle_left);
        resizeHandleRight = fileManagerView.findViewById(R.id.resize_handle_right);
        resizeHandleTop = fileManagerView.findViewById(R.id.resize_handle_top);
        resizeHandleBottom = fileManagerView.findViewById(R.id.resize_handle_bottom);
        
        // Setup file list adapter
        fileAdapter = new ArrayAdapter<>(context, R.layout.file_list_item, R.id.tv_file_name, new ArrayList<>());
        fileListView.setAdapter(fileAdapter);
        
        // Setup file list item click listener
        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < fileList.size()) {
                File selectedFile = fileList.get(position);
                handleFileSelection(selectedFile);
            }
        });
        
        Log.d(TAG, "Views initialized successfully");
    }
    
    /**
     * Setup control button listeners
     */
    private void setupControls() {
        // Up directory button
        if (upButton != null) {
            upButton.setOnClickListener(v -> navigateUp());
        }
        
        // Refresh button
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadDirectory(currentDirectory));
        }
        
        // Home button
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> navigateToHome());
        }
        
        // Minimize button
        if (minimizeButton != null) {
            minimizeButton.setOnClickListener(v -> minimize());
        }
        
        // Close button
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hide());
        }
        
        Log.d(TAG, "Controls setup complete");
    }
    
    /**
     * Setup resize handles for window resizing
     */
    private void setupResizeHandles() {
        View.OnTouchListener resizeTouchListener = (view, event) -> {
            handleResizeTouchEvent(event, (Integer) view.getTag());
            return true;
        };
        
        // Setup all resize handles
        if (resizeHandleTopLeft != null) {
            resizeHandleTopLeft.setTag(0); // CORNER_TOP_LEFT
            resizeHandleTopLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleTopRight != null) {
            resizeHandleTopRight.setTag(1); // CORNER_TOP_RIGHT
            resizeHandleTopRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottomLeft != null) {
            resizeHandleBottomLeft.setTag(2); // CORNER_BOTTOM_LEFT
            resizeHandleBottomLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottomRight != null) {
            resizeHandleBottomRight.setTag(3); // CORNER_BOTTOM_RIGHT
            resizeHandleBottomRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleLeft != null) {
            resizeHandleLeft.setTag(4); // SIDE_LEFT
            resizeHandleLeft.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleRight != null) {
            resizeHandleRight.setTag(5); // SIDE_RIGHT
            resizeHandleRight.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleTop != null) {
            resizeHandleTop.setTag(6); // SIDE_TOP
            resizeHandleTop.setOnTouchListener(resizeTouchListener);
        }
        
        if (resizeHandleBottom != null) {
            resizeHandleBottom.setTag(7); // SIDE_BOTTOM
            resizeHandleBottom.setOnTouchListener(resizeTouchListener);
        }
        
        Log.d(TAG, "Resize handles setup complete");
    }
    
    /**
     * Setup touch listeners for window dragging
     */
    private void setupTouchListeners() {
        fileManagerView.setOnTouchListener((view, event) -> {
            return handleDragTouchEvent(event);
        });
    }
    
    /**
     * Handle drag touch events
     */
    private boolean handleDragTouchEvent(MotionEvent event) {
        if (isResizing) return true;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    
                    layoutParams.x = initialX + deltaX;
                    layoutParams.y = initialY + deltaY;
                    
                    // Constrain to screen bounds
                    constrainWindowPosition();
                    
                    windowManager.updateViewLayout(fileManagerView, layoutParams);
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        
        return false;
    }
    
    /**
     * Handle resize touch events
     */
    private void handleResizeTouchEvent(MotionEvent event, int corner) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isResizing = true;
                resizeCorner = corner;
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    int deltaX = (int) (event.getRawX() - initialTouchX);
                    int deltaY = (int) (event.getRawY() - initialTouchY);
                    
                    applyResize(corner, deltaX, deltaY);
                    windowManager.updateViewLayout(fileManagerView, layoutParams);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isResizing = false;
                resizeCorner = -1;
                break;
        }
    }
    
    /**
     * Apply resize based on corner/side being dragged
     */
    private void applyResize(int corner, int deltaX, int deltaY) {
        int newWidth = layoutParams.width;
        int newHeight = layoutParams.height;
        int newX = layoutParams.x;
        int newY = layoutParams.y;
        
        switch (corner) {
            case 0: // CORNER_TOP_LEFT
                newWidth -= deltaX;
                newHeight -= deltaY;
                newX += deltaX;
                newY += deltaY;
                break;
                
            case 1: // CORNER_TOP_RIGHT
                newWidth += deltaX;
                newHeight -= deltaY;
                newY += deltaY;
                break;
                
            case 2: // CORNER_BOTTOM_LEFT
                newWidth -= deltaX;
                newHeight += deltaY;
                newX += deltaX;
                break;
                
            case 3: // CORNER_BOTTOM_RIGHT
                newWidth += deltaX;
                newHeight += deltaY;
                break;
                
            case 4: // SIDE_LEFT
                newWidth -= deltaX;
                newX += deltaX;
                break;
                
            case 5: // SIDE_RIGHT
                newWidth += deltaX;
                break;
                
            case 6: // SIDE_TOP
                newHeight -= deltaY;
                newY += deltaY;
                break;
                
            case 7: // SIDE_BOTTOM
                newHeight += deltaY;
                break;
        }
        
        // Apply size constraints
        newWidth = Math.max(MIN_WINDOW_WIDTH, Math.min(newWidth, MAX_WINDOW_WIDTH));
        newHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(newHeight, MAX_WINDOW_HEIGHT));
        
        layoutParams.width = newWidth;
        layoutParams.height = newHeight;
        layoutParams.x = newX;
        layoutParams.y = newY;
        
        constrainWindowPosition();
    }
    
    /**
     * Constrain window position to screen bounds
     */
    private void constrainWindowPosition() {
        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        
        // Ensure window stays within screen bounds
        layoutParams.x = Math.max(0, Math.min(layoutParams.x, screenSize.x - layoutParams.width));
        layoutParams.y = Math.max(0, Math.min(layoutParams.y, screenSize.y - layoutParams.height));
    }
    
    /**
     * Load directory contents
     */
    private void loadDirectory(File directory) {
        try {
            if (!directory.exists() || !directory.isDirectory()) {
                Toast.makeText(context, "Directory not accessible", Toast.LENGTH_SHORT).show();
                return;
            }
            
            currentDirectory = directory;
            
            // Update path display
            if (pathTextView != null) {
                String displayPath = directory.getAbsolutePath();
                if (displayPath.length() > 50) {
                    displayPath = "..." + displayPath.substring(displayPath.length() - 47);
                }
                pathTextView.setText(displayPath);
            }
            
            // Get files and folders
            File[] files = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !file.isHidden();
                }
            });
            
            fileList.clear();
            
            if (files != null) {
                // Sort: directories first, then files, both alphabetically
                List<File> directories = new ArrayList<>();
                List<File> regularFiles = new ArrayList<>();
                
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories.add(file);
                    } else {
                        regularFiles.add(file);
                    }
                }
                
                Collections.sort(directories);
                Collections.sort(regularFiles);
                
                fileList.addAll(directories);
                fileList.addAll(regularFiles);
            }
            
            // Update adapter
            List<String> displayNames = new ArrayList<>();
            for (File file : fileList) {
                String name = file.getName();
                if (file.isDirectory()) {
                    name = "📁 " + name;
                } else {
                    name = "📄 " + name;
                }
                displayNames.add(name);
            }
            
            fileAdapter.clear();
            fileAdapter.addAll(displayNames);
            fileAdapter.notifyDataSetChanged();
            
            Log.d(TAG, "Loaded directory: " + directory.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading directory", e);
            Toast.makeText(context, "Error loading directory", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle file/folder selection
     */
    private void handleFileSelection(File file) {
        try {
            if (file.isDirectory()) {
                // Navigate into directory
                loadDirectory(file);
            } else {
                // Handle file selection (e.g., open with video player)
                handleFileOpen(file);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling file selection", e);
            Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to parent directory
     */
    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            loadDirectory(parent);
        }
    }
    
    /**
     * Navigate to home directory
     */
    private void navigateToHome() {
        File homeDir = Environment.getExternalStorageDirectory();
        loadDirectory(homeDir);
    }
    
    /**
     * Handle file opening
     */
    private void handleFileOpen(File file) {
        String fileName = file.getName().toLowerCase();
        String filePath = file.getAbsolutePath();
        
        // Check if it's a video file
        if (isVideoFile(fileName)) {
            // Use callback if available, otherwise send broadcast
            if (videoSelectedListener != null) {
                videoSelectedListener.onVideoSelected(filePath);
            } else {
                // Send broadcast to load video in player
                Intent intent = new Intent("com.floatingvideoplayer.LOAD_VIDEO");
                intent.putExtra("video_path", filePath);
                context.sendBroadcast(intent);
            }
            
            Toast.makeText(context, "Opening video: " + file.getName(), Toast.LENGTH_SHORT).show();
        } else {
            // For other file types, show info or try to open with default app
            Toast.makeText(context, "File: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Set video player callback for file selection
     */
    public void setOnVideoSelectedListener(OnVideoSelectedListener listener) {
        this.videoSelectedListener = listener;
    }
    
    /**
     * Interface for video file selection callback
     */
    public interface OnVideoSelectedListener {
        void onVideoSelected(String videoPath);
    }
    
    /**
     * Check if file is a video file
     */
    private boolean isVideoFile(String fileName) {
        String[] videoExtensions = {".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v"};
        for (String ext : videoExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Minimize window
     */
    private void minimize() {
        hide();
        // TODO: Show minimized controls
    }
    
    /**
     * Check if window is currently visible
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Get current directory
     */
    public File getCurrentDirectory() {
        return currentDirectory;
    }
    
    /**
     * Get current window position and size
     */
    public int[] getWindowBounds() {
        return new int[]{layoutParams.x, layoutParams.y, layoutParams.width, layoutParams.height};
    }
    
    /**
     * Set window position and size
     */
    public void setWindowBounds(int x, int y, int width, int height) {
        if (layoutParams != null) {
            layoutParams.x = x;
            layoutParams.y = y;
            layoutParams.width = width;
            layoutParams.height = height;
            
            if (isVisible && fileManagerView != null) {
                windowManager.updateViewLayout(fileManagerView, layoutParams);
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        hide();
        if (fileAdapter != null) {
            fileAdapter.clear();
        }
        fileList.clear();
    }
}