package com.floatingvideoplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.floatingvideoplayer.R;
import com.floatingvideoplayer.models.MediaFile;
import com.floatingvideoplayer.services.FileManagerService;
import com.floatingvideoplayer.services.MediaMetadataExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Enhanced file browser window with comprehensive UI features including:
 * - List/Grid view toggle
 * - Search and filter functionality
 * - Multiple file selection
 * - File operations (open, delete, rename)
 * - Breadcrumb navigation
 * - Favorites and recent files
 */
public class FileBrowserWindow {
    
    private static final String TAG = "FileBrowserWindow";
    
    private static final int MIN_WINDOW_WIDTH = 400;
    private static final int MIN_WINDOW_HEIGHT = 500;
    private static final int MAX_WINDOW_WIDTH = 1000;
    private static final int MAX_WINDOW_HEIGHT = 1200;
    
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View fileBrowserView;
    
    // File manager service
    private FileManagerService fileManagerService;
    private MediaMetadataExtractor metadataExtractor;
    
    // Window state
    private boolean isVisible = false;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private boolean isGridView = false;
    
    // Navigation state
    private File currentDirectory;
    private FileStack directoryStack;
    private List<MediaFile> currentFiles;
    private List<MediaFile> filteredFiles;
    
    // Selection state
    private List<MediaFile> selectedFiles;
    private boolean selectionMode = false;
    
    // Search and filter
    private String searchQuery = "";
    private MediaFile.MediaType filterType = MediaFile.MediaType.UNKNOWN;
    
    // Favorites and recent
    private FavoritesManager favoritesManager;
    private RecentFilesManager recentFilesManager;
    
    // Views
    private TextView pathTextView;
    private ListView listView;
    private GridView gridView;
    private TextView emptyView;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ImageButton filterButton;
    private ImageButton favoritesButton;
    private ImageButton recentButton;
    private ImageButton upButton;
    private ImageButton homeButton;
    private ImageButton listGridToggleButton;
    private ImageButton minimizeButton;
    private ImageButton closeButton;
    private ImageButton selectAllButton;
    private ImageButton clearSelectionButton;
    private ImageButton deleteButton;
    private ImageButton renameButton;
    private LinearLayout breadcrumbContainer;
    private LinearLayout searchBarContainer;
    private LinearLayout actionBarContainer;
    private FrameLayout listViewContainer;
    private FrameLayout gridViewContainer;
    
    public FileBrowserWindow(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.fileManagerService = new FileManagerService(context);
        this.metadataExtractor = new MediaMetadataExtractor(context);
        this.directoryStack = new FileStack();
        this.currentFiles = new ArrayList<>();
        this.filteredFiles = new ArrayList<>();
        this.selectedFiles = new ArrayList<>();
        this.favoritesManager = new FavoritesManager(context);
        this.recentFilesManager = new RecentFilesManager(context);
        this.currentDirectory = Environment.getExternalStorageDirectory();
    }
    
    /**
     * Show the file browser window
     */
    public void show() {
        if (isVisible) {
            hide();
            return;
        }
        
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            fileBrowserView = inflater.inflate(R.layout.floating_filemanager_layout, null);
            
            layoutParams = createWindowLayoutParams();
            initializeViews();
            setupControls();
            setupSearch();
            setupFilter();
            setupActionBar();
            setupTouchListeners();
            loadDirectory(currentDirectory);
            
            windowManager.addView(fileBrowserView, layoutParams);
            isVisible = true;
            
            Log.d(TAG, "File browser window shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing file browser window", e);
            Toast.makeText(context, "Failed to show file browser", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hide the file browser window
     */
    public void hide() {
        if (!isVisible || fileBrowserView == null) return;
        
        try {
            windowManager.removeView(fileBrowserView);
            fileBrowserView = null;
            isVisible = false;
            
            Log.d(TAG, "File browser window hidden");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding file browser window", e);
        }
    }
    
    /**
     * Toggle between list and grid view
     */
    private void toggleView() {
        isGridView = !isGridView;
        
        if (isGridView) {
            listViewContainer.setVisibility(View.GONE);
            gridViewContainer.setVisibility(View.VISIBLE);
            listGridToggleButton.setImageResource(R.drawable.ic_list);
        } else {
            listViewContainer.setVisibility(View.VISIBLE);
            gridViewContainer.setVisibility(View.GONE);
            listGridToggleButton.setImageResource(R.drawable.ic_grid);
        }
        
        updateFileDisplay();
    }
    
    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Main containers
        pathTextView = fileBrowserView.findViewById(R.id.tv_current_path);
        breadcrumbContainer = fileBrowserView.findViewById(R.id.breadcrumb_container);
        searchBarContainer = fileBrowserView.findViewById(R.id.search_bar_container);
        actionBarContainer = fileBrowserView.findViewById(R.id.action_bar_container);
        listViewContainer = fileBrowserView.findViewById(R.id.list_view_container);
        gridViewContainer = fileBrowserView.findViewById(R.id.grid_view_container);
        
        // Views
        listView = fileBrowserView.findViewById(R.id.lv_file_list);
        gridView = fileBrowserView.findViewById(R.id.gv_file_grid);
        emptyView = fileBrowserView.findViewById(R.id.tv_empty_view);
        
        // Search and filter
        searchEditText = fileBrowserView.findViewById(R.id.et_search);
        searchButton = fileBrowserView.findViewById(R.id.btn_search);
        filterButton = fileBrowserView.findViewById(R.id.btn_filter);
        
        // Action bar buttons
        upButton = fileBrowserView.findViewById(R.id.btn_up_directory);
        homeButton = fileBrowserView.findViewById(R.id.btn_home);
        listGridToggleButton = fileBrowserView.findViewById(R.id.btn_list_grid_toggle);
        favoritesButton = fileBrowserView.findViewById(R.id.btn_favorites);
        recentButton = fileBrowserView.findViewById(R.id.btn_recent);
        minimizeButton = fileBrowserView.findViewById(R.id.btn_minimize_filemanager);
        closeButton = fileBrowserView.findViewById(R.id.btn_close_filemanager);
        
        // Selection mode buttons
        selectAllButton = fileBrowserView.findViewById(R.id.btn_select_all);
        clearSelectionButton = fileBrowserView.findViewById(R.id.btn_clear_selection);
        deleteButton = fileBrowserView.findViewById(R.id.btn_delete);
        renameButton = fileBrowserView.findViewById(R.id.btn_rename);
        
        // Setup list view adapter
        listView.setAdapter(new MediaFileAdapter(context, new ArrayList<>()));
        listView.setOnItemClickListener(this::onFileItemClick);
        listView.setOnItemLongClickListener(this::onFileItemLongClick);
        
        // Setup grid view adapter
        gridView.setAdapter(new MediaFileGridAdapter(context, new ArrayList<>()));
        gridView.setOnItemClickListener(this::onGridItemClick);
        gridView.setOnItemLongClickListener(this::onGridItemLongClick);
        
        Log.d(TAG, "Views initialized successfully");
    }
    
    /**
     * Setup control buttons
     */
    private void setupControls() {
        // Navigation buttons
        if (upButton != null) {
            upButton.setOnClickListener(v -> navigateUp());
        }
        
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> navigateToHome());
        }
        
        if (listGridToggleButton != null) {
            listGridToggleButton.setOnClickListener(v -> toggleView());
        }
        
        if (minimizeButton != null) {
            minimizeButton.setOnClickListener(v -> minimize());
        }
        
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hide());
        }
        
        // Favorites and recent
        if (favoritesButton != null) {
            favoritesButton.setOnClickListener(v -> showFavorites());
        }
        
        if (recentButton != null) {
            recentButton.setOnClickListener(v -> showRecent());
        }
        
        // Selection mode buttons
        if (selectAllButton != null) {
            selectAllButton.setOnClickListener(v -> selectAllFiles());
        }
        
        if (clearSelectionButton != null) {
            clearSelectionButton.setOnClickListener(v -> clearSelection());
        }
        
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> deleteSelectedFiles());
        }
        
        if (renameButton != null) {
            renameButton.setOnClickListener(v -> renameSelectedFile());
        }
        
        Log.d(TAG, "Controls setup complete");
    }
    
    /**
     * Setup search functionality
     */
    private void setupSearch() {
        if (searchEditText != null) {
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                performSearch();
                return true;
            });
        }
        
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> performSearch());
        }
    }
    
    /**
     * Setup filter functionality
     */
    private void setupFilter() {
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }
    }
    
    /**
     * Setup action bar visibility based on selection mode
     */
    private void setupActionBar() {
        updateActionBarVisibility();
    }
    
    /**
     * Setup touch listeners for window dragging
     */
    private void setupTouchListeners() {
        fileBrowserView.setOnTouchListener((view, event) -> {
            return handleDragTouchEvent(event);
        });
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
            
            // Add to directory stack
            directoryStack.push(currentDirectory);
            currentDirectory = directory;
            
            // Update path display
            updatePathDisplay();
            updateBreadcrumb();
            
            // Clear selection
            clearSelection();
            
            // Load files in background
            fileManagerService.executorService.execute(() -> {
                List<File> files = fileManagerService.getFilesFromDirectory(directory, 
                    file -> !file.isHidden());
                
                List<MediaFile> mediaFiles = convertToMediaFiles(files);
                
                // Apply current filter
                List<MediaFile> filtered = applyFilters(mediaFiles);
                
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> {
                    currentFiles = mediaFiles;
                    filteredFiles = filtered;
                    updateFileDisplay();
                });
            });
            
            Log.d(TAG, "Loaded directory: " + directory.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading directory", e);
            Toast.makeText(context, "Error loading directory", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Convert File objects to MediaFile objects
     */
    private List<MediaFile> convertToMediaFiles(List<File> files) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        
        for (File file : files) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setName(file.getName());
            mediaFile.setPath(file.getAbsolutePath());
            mediaFile.setIsDirectory(file.isDirectory());
            mediaFile.setSize(file.length());
            mediaFile.setDateModified(file.lastModified());
            mediaFile.setDateAdded(file.lastModified());
            
            if (!file.isDirectory()) {
                String extension = fileManagerService.getFileExtension(file.getName());
                mediaFile.setExtension(extension);
                
                if (fileManagerService.isVideoFile(file.getName())) {
                    mediaFile.setType(MediaFile.MediaType.VIDEO);
                } else if (fileManagerService.isAudioFile(file.getName())) {
                    mediaFile.setType(MediaFile.MediaType.AUDIO);
                } else {
                    mediaFile.setType(MediaFile.MediaType.UNKNOWN);
                }
            } else {
                mediaFile.setType(MediaFile.MediaType.UNKNOWN);
            }
            
            mediaFiles.add(mediaFile);
        }
        
        return mediaFiles;
    }
    
    /**
     * Apply search and filter to media files
     */
    private List<MediaFile> applyFilters(List<MediaFile> files) {
        List<MediaFile> filtered = new ArrayList<>();
        
        for (MediaFile file : files) {
            boolean matchesSearch = true;
            boolean matchesFilter = true;
            
            // Apply search query
            if (!TextUtils.isEmpty(searchQuery)) {
                String query = searchQuery.toLowerCase(Locale.getDefault());
                matchesSearch = file.getName().toLowerCase(Locale.getDefault()).contains(query);
            }
            
            // Apply type filter
            if (filterType != MediaFile.MediaType.UNKNOWN) {
                if (filterType == MediaFile.MediaType.VIDEO) {
                    matchesFilter = file.isVideo() || file.getDisplayName().contains("📁");
                } else if (filterType == MediaFile.MediaType.AUDIO) {
                    matchesFilter = file.isAudio() || file.getDisplayName().contains("📁");
                } else {
                    matchesFilter = true;
                }
            }
            
            if (matchesSearch && matchesFilter) {
                filtered.add(file);
            }
        }
        
        return filtered;
    }
    
    /**
     * Update file display
     */
    private void updateFileDisplay() {
        if (isGridView) {
            updateGridView();
        } else {
            updateListView();
        }
        
        updateEmptyView();
    }
    
    /**
     * Update list view
     */
    private void updateListView() {
        MediaFileAdapter adapter = (MediaFileAdapter) listView.getAdapter();
        adapter.updateFiles(filteredFiles);
    }
    
    /**
     * Update grid view
     */
    private void updateGridView() {
        MediaFileGridAdapter adapter = (MediaFileGridAdapter) gridView.getAdapter();
        adapter.updateFiles(filteredFiles);
    }
    
    /**
     * Update empty view
     */
    private void updateEmptyView() {
        if (filteredFiles.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            gridView.setVisibility(View.GONE);
            
            if (!TextUtils.isEmpty(searchQuery) || filterType != MediaFile.MediaType.UNKNOWN) {
                emptyView.setText("No files match your criteria");
            } else {
                emptyView.setText("This directory is empty");
            }
        } else {
            emptyView.setVisibility(View.GONE);
            if (isGridView) {
                gridView.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Update path display
     */
    private void updatePathDisplay() {
        if (pathTextView != null) {
            String path = currentDirectory.getAbsolutePath();
            if (path.length() > 50) {
                path = "..." + path.substring(path.length() - 47);
            }
            pathTextView.setText(path);
        }
    }
    
    /**
     * Update breadcrumb navigation
     */
    private void updateBreadcrumb() {
        breadcrumbContainer.removeAllViews();
        
        String[] pathParts = currentDirectory.getAbsolutePath().split("/");
        File currentPath = new File("/");
        
        for (int i = 0; i < pathParts.length; i++) {
            if (!pathParts[i].isEmpty()) {
                currentPath = new File(currentPath, pathParts[i]);
                
                TextView pathPart = new TextView(context);
                pathPart.setText(pathParts[i]);
                pathPart.setPadding(8, 4, 8, 4);
                pathPart.setBackgroundResource(R.drawable.button_background);
                pathPart.setTextSize(14);
                
                if (i < pathParts.length - 1) {
                    pathPart.setOnClickListener(v -> loadDirectory(currentPath));
                } else {
                    pathPart.setTypeface(null, Typeface.BOLD);
                }
                
                breadcrumbContainer.addView(pathPart);
                
                // Add separator
                if (i < pathParts.length - 1) {
                    TextView separator = new TextView(context);
                    separator.setText(" > ");
                    breadcrumbContainer.addView(separator);
                }
            }
        }
    }
    
    /**
     * Handle file item click in list view
     */
    private void onFileItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        if (position < filteredFiles.size()) {
            MediaFile file = filteredFiles.get(position);
            
            if (selectionMode) {
                toggleFileSelection(file);
            } else {
                handleFileSelection(file);
            }
        }
    }
    
    /**
     * Handle file item long click in list view
     */
    private boolean onFileItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        if (position < filteredFiles.size()) {
            MediaFile file = filteredFiles.get(position);
            startSelectionMode();
            toggleFileSelection(file);
            return true;
        }
        return false;
    }
    
    /**
     * Handle file item click in grid view
     */
    private void onGridItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        onFileItemClick(parent, view, position, id);
    }
    
    /**
     * Handle file item long click in grid view
     */
    private boolean onGridItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        return onFileItemLongClick(parent, view, position, id);
    }
    
    /**
     * Handle file selection
     */
    private void handleFileSelection(MediaFile file) {
        try {
            if (file.isDirectory()) {
                // Navigate into directory
                loadDirectory(new File(file.getPath()));
            } else {
                // Handle file opening
                handleFileOpen(file);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling file selection", e);
            Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle file opening
     */
    private void handleFileOpen(MediaFile file) {
        String fileName = file.getName().toLowerCase();
        
        if (file.isVideo()) {
            // Send intent to open in video player
            Intent intent = new Intent(context, DraggableVideoPlayerWindow.class);
            // This would need to be properly implemented to pass the video URL/path
            Toast.makeText(context, "Opening video: " + file.getName(), Toast.LENGTH_SHORT).show();
            
            // Add to recent files
            recentFilesManager.addRecentFile(file);
            
        } else if (file.isAudio()) {
            // Handle audio file
            Toast.makeText(context, "Opening audio: " + file.getName(), Toast.LENGTH_SHORT).show();
            
            // Add to recent files
            recentFilesManager.addRecentFile(file);
            
        } else {
            // For other file types, show info
            Toast.makeText(context, "File: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to parent directory
     */
    private void navigateUp() {
        if (!directoryStack.isEmpty()) {
            File parent = directoryStack.pop();
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
     * Start selection mode
     */
    private void startSelectionMode() {
        selectionMode = true;
        updateActionBarVisibility();
    }
    
    /**
     * Exit selection mode
     */
    private void exitSelectionMode() {
        selectionMode = false;
        selectedFiles.clear();
        updateActionBarVisibility();
        updateFileDisplay();
    }
    
    /**
     * Toggle file selection
     */
    private void toggleFileSelection(MediaFile file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
            file.setSelected(false);
        } else {
            selectedFiles.add(file);
            file.setSelected(true);
        }
        
        updateSelectionInfo();
        updateActionBarVisibility();
        updateFileDisplay();
        
        if (selectedFiles.isEmpty()) {
            exitSelectionMode();
        }
    }
    
    /**
     * Select all files
     */
    private void selectAllFiles() {
        selectedFiles.clear();
        for (MediaFile file : filteredFiles) {
            if (!file.isDirectory() || fileManagerService.isSupportedMediaFormat(file.getName())) {
                file.setSelected(true);
                selectedFiles.add(file);
            }
        }
        
        updateSelectionInfo();
        updateActionBarVisibility();
        updateFileDisplay();
    }
    
    /**
     * Clear selection
     */
    private void clearSelection() {
        for (MediaFile file : filteredFiles) {
            file.setSelected(false);
        }
        selectedFiles.clear();
        exitSelectionMode();
    }
    
    /**
     * Update selection info
     */
    private void updateSelectionInfo() {
        if (selectionMode && selectedFiles.size() > 0) {
            String info = String.format("%d files selected", selectedFiles.size());
            Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update action bar visibility based on selection mode
     */
    private void updateActionBarVisibility() {
        if (selectionMode) {
            // Show selection mode buttons
            selectAllButton.setVisibility(View.VISIBLE);
            clearSelectionButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            if (selectedFiles.size() == 1) {
                renameButton.setVisibility(View.VISIBLE);
            } else {
                renameButton.setVisibility(View.GONE);
            }
            
            // Hide some regular buttons
            favoritesButton.setVisibility(View.GONE);
            recentButton.setVisibility(View.GONE);
            filterButton.setVisibility(View.GONE);
        } else {
            // Show regular buttons
            selectAllButton.setVisibility(View.GONE);
            clearSelectionButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            renameButton.setVisibility(View.GONE);
            
            favoritesButton.setVisibility(View.VISIBLE);
            recentButton.setVisibility(View.VISIBLE);
            filterButton.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Perform search
     */
    private void performSearch() {
        searchQuery = searchEditText.getText().toString().trim();
        filteredFiles = applyFilters(currentFiles);
        updateFileDisplay();
    }
    
    /**
     * Show filter dialog
     */
    private void showFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Filter Files");
        
        View view = LayoutInflater.from(context).inflate(R.layout.filter_dialog_layout, null);
        RadioGroup filterGroup = view.findViewById(R.id.rg_filter);
        
        // Set current filter
        if (filterType == MediaFile.MediaType.VIDEO) {
            filterGroup.check(R.id.rb_videos);
        } else if (filterType == MediaFile.MediaType.AUDIO) {
            filterGroup.check(R.id.rb_audio);
        } else {
            filterGroup.check(R.id.rb_all);
        }
        
        builder.setView(view);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            int selectedId = filterGroup.getCheckedRadioButtonId();
            
            if (selectedId == R.id.rb_videos) {
                filterType = MediaFile.MediaType.VIDEO;
            } else if (selectedId == R.id.rb_audio) {
                filterType = MediaFile.MediaType.AUDIO;
            } else {
                filterType = MediaFile.MediaType.UNKNOWN;
            }
            
            filteredFiles = applyFilters(currentFiles);
            updateFileDisplay();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Show favorites
     */
    private void showFavorites() {
        List<MediaFile> favorites = favoritesManager.getFavorites();
        if (!favorites.isEmpty()) {
            filteredFiles = favorites;
            updateFileDisplay();
        } else {
            Toast.makeText(context, "No favorite files", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show recent files
     */
    private void showRecent() {
        List<MediaFile> recent = recentFilesManager.getRecentFiles();
        if (!recent.isEmpty()) {
            filteredFiles = recent;
            updateFileDisplay();
        } else {
            Toast.makeText(context, "No recent files", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Delete selected files
     */
    private void deleteSelectedFiles() {
        if (selectedFiles.isEmpty()) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Delete Files");
        builder.setMessage("Are you sure you want to delete " + selectedFiles.size() + " file(s)?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            for (MediaFile file : selectedFiles) {
                File fileObj = new File(file.getPath());
                if (fileObj.exists()) {
                    fileObj.delete();
                }
            }
            
            // Refresh current directory
            loadDirectory(currentDirectory);
            Toast.makeText(context, "Files deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Rename selected file
     */
    private void renameSelectedFile() {
        if (selectedFiles.size() != 1) return;
        
        MediaFile file = selectedFiles.get(0);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Rename File");
        
        final EditText input = new EditText(context);
        input.setText(file.getName());
        builder.setView(input);
        
        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(file.getName())) {
                File oldFile = new File(file.getPath());
                File newFile = new File(oldFile.getParent(), newName);
                
                if (oldFile.renameTo(newFile)) {
                    // Update file path
                    file.setName(newName);
                    file.setPath(newFile.getAbsolutePath());
                    
                    // Refresh current directory
                    loadDirectory(currentDirectory);
                    Toast.makeText(context, "File renamed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Minimize window
     */
    private void minimize() {
        hide();
        // TODO: Show minimized controls
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
                    
                    constrainWindowPosition();
                    windowManager.updateViewLayout(fileBrowserView, layoutParams);
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
     * Constrain window position to screen bounds
     */
    private void constrainWindowPosition() {
        android.graphics.Point screenSize = new android.graphics.Point();
        windowManager.getDefaultDisplay().getSize(screenSize);
        
        layoutParams.x = Math.max(0, Math.min(layoutParams.x, screenSize.x - layoutParams.width));
        layoutParams.y = Math.max(0, Math.min(layoutParams.y, screenSize.y - layoutParams.height));
    }
    
    // Window drag variables
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    /**
     * Create window layout parameters
     */
    private WindowManager.LayoutParams createWindowLayoutParams() {
        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                700, // Default width
                800, // Default height
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
     * Clean up resources
     */
    public void destroy() {
        hide();
        if (fileManagerService != null) {
            fileManagerService.shutdown();
        }
    }
    
    // Helper classes
    private static class FileStack {
        private java.util.Stack<File> stack = new java.util.Stack<>();
        
        public void push(File file) {
            if (file != null) {
                stack.push(file);
            }
        }
        
        public File pop() {
            return stack.isEmpty() ? null : stack.pop();
        }
        
        public boolean isEmpty() {
            return stack.isEmpty();
        }
    }
    
    private static class FavoritesManager {
        private Context context;
        private static final String PREF_NAME = "file_manager_favorites";
        
        FavoritesManager(Context context) {
            this.context = context;
        }
        
        public void addFavorite(MediaFile file) {
            // Implementation to add file to favorites
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            // Add file to favorites list
            editor.putString(file.getPath(), file.getName());
            editor.apply();
        }
        
        public List<MediaFile> getFavorites() {
            // Implementation to get favorites list
            return new ArrayList<>();
        }
    }
    
    private static class RecentFilesManager {
        private Context context;
        private static final String PREF_NAME = "file_manager_recent";
        
        RecentFilesManager(Context context) {
            this.context = context;
        }
        
        public void addRecentFile(MediaFile file) {
            // Implementation to add file to recent files
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(file.getPath(), System.currentTimeMillis());
            editor.apply();
        }
        
        public List<MediaFile> getRecentFiles() {
            // Implementation to get recent files list
            return new ArrayList<>();
        }
    }
}