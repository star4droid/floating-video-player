package com.floatingvideoplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages window state persistence and restoration using SharedPreferences
 * Handles window position, size, z-index, and state management
 */
public class WindowStateManager {
    
    private static final String TAG = "WindowStateManager";
    
    private static final String PREFS_NAME = "WindowStatePreferences";
    private static final String KEY_WINDOW_STATES = "window_states";
    private static final String KEY_DEFAULT_SETTINGS = "default_settings";
    private static final String KEY_MULTI_WINDOW_CONFIG = "multi_window_config";
    
    // Default window state
    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 300;
    private static final int DEFAULT_X = 100;
    private static final int DEFAULT_Y = 100;
    private static final int DEFAULT_Z_ORDER = 1000;
    private static final float DEFAULT_ALPHA = 1.0f;
    
    // State persistence keys
    private static final String KEY_WINDOW_ID = "window_id";
    private static final String KEY_POSITION_X = "position_x";
    private static final String KEY_POSITION_Y = "position_y";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_Z_ORDER = "z_order";
    private static final String KEY_ALPHA = "alpha";
    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_MINIMIZED = "minimized";
    private static final String KEY_MAXIMIZED = "maximized";
    private static final String KEY_LOCKED = "locked";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_ANIMATION_TYPE = "animation_type";
    private static final String KEY_GESTURE_ENABLED = "gesture_enabled";
    private static final String KEY_AUTO_SAVE = "auto_save";
    
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Map<String, WindowState> cachedStates;
    
    /**
     * Window state information container
     */
    public static class WindowState {
        public String windowId;
        public int x;
        public int y;
        public int width;
        public int height;
        public int zOrder;
        public float alpha;
        public boolean isVisible;
        public boolean isMinimized;
        public boolean isMaximized;
        public boolean isLocked;
        public long lastUpdate;
        public String animationType;
        public boolean gestureEnabled;
        public boolean autoSave;
        
        public WindowState() {
            // Initialize with default values
            this.x = DEFAULT_X;
            this.y = DEFAULT_Y;
            this.width = DEFAULT_WIDTH;
            this.height = DEFAULT_HEIGHT;
            this.zOrder = DEFAULT_Z_ORDER;
            this.alpha = DEFAULT_ALPHA;
            this.isVisible = true;
            this.isMinimized = false;
            this.isMaximized = false;
            this.isLocked = false;
            this.lastUpdate = System.currentTimeMillis();
            this.animationType = "smooth";
            this.gestureEnabled = true;
            this.autoSave = true;
        }
        
        public WindowState(String windowId) {
            this();
            this.windowId = windowId;
        }
        
        public WindowState(String windowId, int x, int y, int width, int height) {
            this(windowId);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Default window settings
     */
    public static class DefaultSettings {
        public int playerWidth = DEFAULT_WIDTH;
        public int playerHeight = DEFAULT_HEIGHT;
        public int playerX = DEFAULT_X;
        public int playerY = DEFAULT_Y;
        public int fileManagerWidth = 500;
        public int fileManagerHeight = 400;
        public int fileManagerX = 50;
        public int fileManagerY = 50;
        public boolean enableAnimations = true;
        public boolean enableGestures = true;
        public boolean autoRestoreWindows = true;
        public float defaultOpacity = 1.0f;
        public String defaultAnimationType = "smooth";
        public int maxWindows = 5;
        public boolean persistOnClose = true;
    }
    
    /**
     * Multi-window configuration
     */
    public static class MultiWindowConfig {
        public boolean splitScreenSupport = true;
        public boolean pictureInPictureSupport = true;
        public boolean autoAdjustOnOrientation = true;
        public boolean autoMinimizeInBackground = false;
        public int minWindowSize = 200;
        public int maxWindowSize = 1000;
        public boolean respectSystemMultiWindow = true;
        public boolean adaptiveLayout = true;
    }
    
    public WindowStateManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.editor = preferences.edit();
        this.cachedStates = new HashMap<>();
        loadCachedStates();
    }
    
    /**
     * Save window state to persistent storage
     */
    public boolean saveWindowState(WindowState state) {
        try {
            if (state == null || state.windowId == null) {
                Log.w(TAG, "Invalid window state or window ID");
                return false;
            }
            
            JSONObject jsonState = new JSONObject();
            jsonState.put(KEY_WINDOW_ID, state.windowId);
            jsonState.put(KEY_POSITION_X, state.x);
            jsonState.put(KEY_POSITION_Y, state.y);
            jsonState.put(KEY_WIDTH, state.width);
            jsonState.put(KEY_HEIGHT, state.height);
            jsonState.put(KEY_Z_ORDER, state.zOrder);
            jsonState.put(KEY_ALPHA, state.alpha);
            jsonState.put(KEY_VISIBLE, state.isVisible);
            jsonState.put(KEY_MINIMIZED, state.isMinimized);
            jsonState.put(KEY_MAXIMIZED, state.isMaximized);
            jsonState.put(KEY_LOCKED, state.isLocked);
            jsonState.put(KEY_LAST_UPDATE, state.lastUpdate);
            jsonState.put(KEY_ANIMATION_TYPE, state.animationType);
            jsonState.put(KEY_GESTURE_ENABLED, state.gestureEnabled);
            jsonState.put(KEY_AUTO_SAVE, state.autoSave);
            
            // Get existing states
            JSONArray existingStates = getWindowStatesArray();
            
            // Update or add new state
            boolean found = false;
            for (int i = 0; i < existingStates.length(); i++) {
                JSONObject existingState = existingStates.getJSONObject(i);
                if (existingState.getString(KEY_WINDOW_ID).equals(state.windowId)) {
                    existingStates.put(i, jsonState);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                existingStates.put(jsonState);
            }
            
            // Save to preferences
            editor.putString(KEY_WINDOW_STATES, existingStates.toString());
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
            
            boolean success = editor.commit();
            if (success) {
                cachedStates.put(state.windowId, state);
                Log.d(TAG, "Window state saved: " + state.windowId);
            }
            
            return success;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error saving window state", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error saving window state", e);
            return false;
        }
    }
    
    /**
     * Load window state from persistent storage
     */
    public WindowState loadWindowState(String windowId) {
        try {
            if (windowId == null) {
                Log.w(TAG, "Window ID is null");
                return null;
            }
            
            // Check cache first
            if (cachedStates.containsKey(windowId)) {
                return cachedStates.get(windowId);
            }
            
            JSONArray states = getWindowStatesArray();
            for (int i = 0; i < states.length(); i++) {
                JSONObject stateJson = states.getJSONObject(i);
                if (stateJson.getString(KEY_WINDOW_ID).equals(windowId)) {
                    WindowState state = parseWindowState(stateJson);
                    cachedStates.put(windowId, state);
                    return state;
                }
            }
            
            // Return default state if not found
            return new WindowState(windowId);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error loading window state", e);
            return new WindowState(windowId);
        }
    }
    
    /**
     * Remove window state from storage
     */
    public boolean removeWindowState(String windowId) {
        try {
            JSONArray states = getWindowStatesArray();
            JSONArray newStates = new JSONArray();
            
            for (int i = 0; i < states.length(); i++) {
                JSONObject stateJson = states.getJSONObject(i);
                if (!stateJson.getString(KEY_WINDOW_ID).equals(windowId)) {
                    newStates.put(stateJson);
                }
            }
            
            editor.putString(KEY_WINDOW_STATES, newStates.toString());
            boolean success = editor.commit();
            
            if (success) {
                cachedStates.remove(windowId);
                Log.d(TAG, "Window state removed: " + windowId);
            }
            
            return success;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error removing window state", e);
            return false;
        }
    }
    
    /**
     * Load all window states
     */
    public Map<String, WindowState> loadAllWindowStates() {
        Map<String, WindowState> allStates = new HashMap<>();
        
        try {
            JSONArray states = getWindowStatesArray();
            for (int i = 0; i < states.length(); i++) {
                JSONObject stateJson = states.getJSONObject(i);
                WindowState state = parseWindowState(stateJson);
                allStates.put(state.windowId, state);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading all window states", e);
        }
        
        return allStates;
    }
    
    /**
     * Save default window settings
     */
    public boolean saveDefaultSettings(DefaultSettings settings) {
        try {
            JSONObject jsonSettings = new JSONObject();
            jsonSettings.put("playerWidth", settings.playerWidth);
            jsonSettings.put("playerHeight", settings.playerHeight);
            jsonSettings.put("playerX", settings.playerX);
            jsonSettings.put("playerY", settings.playerY);
            jsonSettings.put("fileManagerWidth", settings.fileManagerWidth);
            jsonSettings.put("fileManagerHeight", settings.fileManagerHeight);
            jsonSettings.put("fileManagerX", settings.fileManagerX);
            jsonSettings.put("fileManagerY", settings.fileManagerY);
            jsonSettings.put("enableAnimations", settings.enableAnimations);
            jsonSettings.put("enableGestures", settings.enableGestures);
            jsonSettings.put("autoRestoreWindows", settings.autoRestoreWindows);
            jsonSettings.put("defaultOpacity", settings.defaultOpacity);
            jsonSettings.put("defaultAnimationType", settings.defaultAnimationType);
            jsonSettings.put("maxWindows", settings.maxWindows);
            jsonSettings.put("persistOnClose", settings.persistOnClose);
            
            editor.putString(KEY_DEFAULT_SETTINGS, jsonSettings.toString());
            return editor.commit();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error saving default settings", e);
            return false;
        }
    }
    
    /**
     * Load default window settings
     */
    public DefaultSettings loadDefaultSettings() {
        DefaultSettings settings = new DefaultSettings();
        
        try {
            String settingsJson = preferences.getString(KEY_DEFAULT_SETTINGS, null);
            if (settingsJson != null) {
                JSONObject jsonSettings = new JSONObject(settingsJson);
                settings.playerWidth = jsonSettings.getInt("playerWidth");
                settings.playerHeight = jsonSettings.getInt("playerHeight");
                settings.playerX = jsonSettings.getInt("playerX");
                settings.playerY = jsonSettings.getInt("playerY");
                settings.fileManagerWidth = jsonSettings.getInt("fileManagerWidth");
                settings.fileManagerHeight = jsonSettings.getInt("fileManagerHeight");
                settings.fileManagerX = jsonSettings.getInt("fileManagerX");
                settings.fileManagerY = jsonSettings.getInt("fileManagerY");
                settings.enableAnimations = jsonSettings.getBoolean("enableAnimations");
                settings.enableGestures = jsonSettings.getBoolean("enableGestures");
                settings.autoRestoreWindows = jsonSettings.getBoolean("autoRestoreWindows");
                settings.defaultOpacity = (float) jsonSettings.getDouble("defaultOpacity");
                settings.defaultAnimationType = jsonSettings.getString("defaultAnimationType");
                settings.maxWindows = jsonSettings.getInt("maxWindows");
                settings.persistOnClose = jsonSettings.getBoolean("persistOnClose");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading default settings", e);
        }
        
        return settings;
    }
    
    /**
     * Save multi-window configuration
     */
    public boolean saveMultiWindowConfig(MultiWindowConfig config) {
        try {
            JSONObject jsonConfig = new JSONObject();
            jsonConfig.put("splitScreenSupport", config.splitScreenSupport);
            jsonConfig.put("pictureInPictureSupport", config.pictureInPictureSupport);
            jsonConfig.put("autoAdjustOnOrientation", config.autoAdjustOnOrientation);
            jsonConfig.put("autoMinimizeInBackground", config.autoMinimizeInBackground);
            jsonConfig.put("minWindowSize", config.minWindowSize);
            jsonConfig.put("maxWindowSize", config.maxWindowSize);
            jsonConfig.put("respectSystemMultiWindow", config.respectSystemMultiWindow);
            jsonConfig.put("adaptiveLayout", config.adaptiveLayout);
            
            editor.putString(KEY_MULTI_WINDOW_CONFIG, jsonConfig.toString());
            return editor.commit();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error saving multi-window config", e);
            return false;
        }
    }
    
    /**
     * Load multi-window configuration
     */
    public MultiWindowConfig loadMultiWindowConfig() {
        MultiWindowConfig config = new MultiWindowConfig();
        
        try {
            String configJson = preferences.getString(KEY_MULTI_WINDOW_CONFIG, null);
            if (configJson != null) {
                JSONObject jsonConfig = new JSONObject(configJson);
                config.splitScreenSupport = jsonConfig.getBoolean("splitScreenSupport");
                config.pictureInPictureSupport = jsonConfig.getBoolean("pictureInPictureSupport");
                config.autoAdjustOnOrientation = jsonConfig.getBoolean("autoAdjustOnOrientation");
                config.autoMinimizeInBackground = jsonConfig.getBoolean("autoMinimizeInBackground");
                config.minWindowSize = jsonConfig.getInt("minWindowSize");
                config.maxWindowSize = jsonConfig.getInt("maxWindowSize");
                config.respectSystemMultiWindow = jsonConfig.getBoolean("respectSystemMultiWindow");
                config.adaptiveLayout = jsonConfig.getBoolean("adaptiveLayout");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading multi-window config", e);
        }
        
        return config;
    }
    
    /**
     * Clear all saved window states
     */
    public boolean clearAllStates() {
        try {
            editor.remove(KEY_WINDOW_STATES);
            editor.remove(KEY_DEFAULT_SETTINGS);
            editor.remove(KEY_MULTI_WINDOW_CONFIG);
            boolean success = editor.commit();
            
            if (success) {
                cachedStates.clear();
                Log.d(TAG, "All window states cleared");
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all states", e);
            return false;
        }
    }
    
    /**
     * Get list of saved window IDs
     */
    public List<String> getSavedWindowIds() {
        List<String> windowIds = new ArrayList<>();
        
        try {
            JSONArray states = getWindowStatesArray();
            for (int i = 0; i < states.length(); i++) {
                JSONObject stateJson = states.getJSONObject(i);
                windowIds.add(stateJson.getString(KEY_WINDOW_ID));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting saved window IDs", e);
        }
        
        return windowIds;
    }
    
    /**
     * Auto-save window state with throttling
     */
    public void autoSaveWindowState(WindowState state) {
        if (state != null && state.autoSave) {
            // Simple throttling - save at most once every 2 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - state.lastUpdate > 2000) {
                saveWindowState(state);
            }
        }
    }
    
    /**
     * Apply window state to layout parameters
     */
    public void applyStateToLayoutParams(WindowState state, WindowManager.LayoutParams params) {
        if (state == null || params == null) return;
        
        params.x = state.x;
        params.y = state.y;
        params.width = state.width;
        params.height = state.height;
        params.alpha = state.alpha;
        
        if (state.isLocked) {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
    }
    
    /**
     * Extract window state from layout parameters
     */
    public WindowState extractStateFromLayoutParams(String windowId, WindowManager.LayoutParams params) {
        WindowState state = loadWindowState(windowId);
        if (params != null) {
            state.x = params.x;
            state.y = params.y;
            state.width = params.width;
            state.height = params.height;
            state.alpha = params.alpha;
            state.lastUpdate = System.currentTimeMillis();
        }
        return state;
    }
    
    // Private helper methods
    
    private JSONArray getWindowStatesArray() throws JSONException {
        String statesJson = preferences.getString(KEY_WINDOW_STATES, "[]");
        return new JSONArray(statesJson);
    }
    
    private WindowState parseWindowState(JSONObject json) throws JSONException {
        WindowState state = new WindowState();
        state.windowId = json.getString(KEY_WINDOW_ID);
        state.x = json.getInt(KEY_POSITION_X);
        state.y = json.getInt(KEY_POSITION_Y);
        state.width = json.getInt(KEY_WIDTH);
        state.height = json.getInt(KEY_HEIGHT);
        state.zOrder = json.getInt(KEY_Z_ORDER);
        state.alpha = (float) json.getDouble(KEY_ALPHA);
        state.isVisible = json.getBoolean(KEY_VISIBLE);
        state.isMinimized = json.getBoolean(KEY_MINIMIZED);
        state.isMaximized = json.getBoolean(KEY_MAXIMIZED);
        state.isLocked = json.getBoolean(KEY_LOCKED);
        state.lastUpdate = json.getLong(KEY_LAST_UPDATE);
        state.animationType = json.getString(KEY_ANIMATION_TYPE);
        state.gestureEnabled = json.getBoolean(KEY_GESTURE_ENABLED);
        state.autoSave = json.getBoolean(KEY_AUTO_SAVE);
        return state;
    }
    
    private void loadCachedStates() {
        cachedStates.clear();
        Map<String, WindowState> states = loadAllWindowStates();
        cachedStates.putAll(states);
    }
}