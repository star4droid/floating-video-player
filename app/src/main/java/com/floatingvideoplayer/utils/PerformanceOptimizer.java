package com.floatingvideoplayer.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Memory and performance optimization manager for the floating video player
 * Handles lazy loading, background processing, memory management, and battery optimization
 */
public class PerformanceOptimizer {
    
    private static final String TAG = "PerformanceOptimizer";
    
    // Memory thresholds
    private static final long LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB
    private static final int MAX_CACHE_SIZE = 100; // Default cache size
    
    // Performance monitoring
    public interface PerformanceListener {
        void onMemoryWarning(long availableMemory);
        void onMemoryCritical(long availableMemory);
        void onLowPerformanceDetected();
        void onHighCpuUsageDetected();
        void onBatteryOptimizationEnabled();
        void onPerformanceMetricsUpdated(PerformanceMetrics metrics);
    }
    
    /**
     * Performance metrics container
     */
    public static class PerformanceMetrics {
        public long totalMemory;
        public long availableMemory;
        public long usedMemory;
        public float memoryUsagePercent;
        public float cpuUsagePercent;
        public int activeThreads;
        public long cacheSize;
        public long cacheHitRate;
        public long backgroundTasks;
        public boolean isLowMemoryMode;
        public boolean isBatteryOptimized;
        public long lastUpdateTime;
        
        public PerformanceMetrics(long totalMemory, long availableMemory, float cpuUsage, int threads) {
            this.totalMemory = totalMemory;
            this.availableMemory = availableMemory;
            this.usedMemory = totalMemory - availableMemory;
            this.memoryUsagePercent = (float) usedMemory / totalMemory * 100;
            this.cpuUsagePercent = cpuUsage;
            this.activeThreads = threads;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Task priority levels
     */
    public enum TaskPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Background task interface
     */
    public interface BackgroundTask {
        void execute() throws Exception;
        String getTaskId();
        TaskPriority getPriority();
        boolean shouldCacheResult();
        long getEstimatedDuration();
    }
    
    /**
     * Resource cleanup callback
     */
    public interface ResourceCleanupCallback {
        void cleanup();
        String getResourceId();
        long getLastUsedTime();
    }
    
    private Context context;
    private ActivityManager activityManager;
    private Handler mainHandler;
    private Handler backgroundHandler;
    
    // Caching systems
    private LruCache<String, Object> memoryCache;
    private LruCache<String, byte[]> thumbnailCache;
    private Map<String, Object> persistentCache;
    
    // Threading
    private ExecutorService backgroundExecutor;
    private ScheduledExecutorService maintenanceExecutor;
    private boolean isLowMemoryMode = false;
    private boolean isBatteryOptimized = false;
    
    // Performance monitoring
    private PerformanceListener performanceListener;
    private PerformanceMetrics currentMetrics;
    private List<ResourceCleanupCallback> cleanupCallbacks;
    private Map<String, Long> resourceUsage;
    
    // Optimization settings
    private boolean enableLazyLoading = true;
    private boolean enableBackgroundThumbnails = true;
    private boolean enableMemoryManagement = true;
    private boolean enableBatteryOptimization = true;
    private boolean enableGarbageCollection = true;
    private boolean enableCacheOptimization = true;
    private int maxMemoryUsage = 100; // MB
    private int maxThreadCount = 4;
    
    // Battery optimization
    private static final String BATTERY_OPTIMIZATION_INTENT = 
        "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";
    
    public PerformanceOptimizer(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.backgroundHandler = new Handler(Looper.getMainLooper());
        
        // Initialize caching systems
        initializeCache();
        
        // Initialize thread pools
        initializeThreading();
        
        // Initialize monitoring
        cleanupCallbacks = new ArrayList<>();
        resourceUsage = new HashMap<>();
        
        // Start performance monitoring
        startPerformanceMonitoring();
        
        Log.d(TAG, "PerformanceOptimizer initialized");
    }
    
    /**
     * Initialize caching systems
     */
    private void initializeCache() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        int cacheSize = (int) Math.min(
            memInfo.totalMem / 1024 / 1024 / 8, // Use 1/8 of available memory
            MAX_CACHE_SIZE
        );
        
        memoryCache = new LruCache<String, Object>(cacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Object oldValue, Object newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                Log.d(TAG, "Cache entry removed: " + key);
            }
            
            @Override
            protected int sizeOf(String key, Object value) {
                // Estimate size of cached object
                if (value instanceof byte[]) {
                    return ((byte[]) value).length;
                } else if (value instanceof String) {
                    return ((String) value).length() * 2;
                }
                return 1024; // Default estimate
            }
        };
        
        thumbnailCache = new LruCache<String, byte[]>(cacheSize / 2) {
            @Override
            protected int sizeOf(String key, byte[] value) {
                return value.length;
            }
        };
        
        persistentCache = new HashMap<>();
    }
    
    /**
     * Initialize threading systems
     */
    private void initializeThreading() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        int threadPoolSize = Math.min(coreCount, maxThreadCount);
        
        backgroundExecutor = Executors.newFixedThreadPool(threadPoolSize);
        maintenanceExecutor = Executors.newScheduledThreadPool(2);
        
        Log.d(TAG, "Thread pools initialized with " + threadPoolSize + " threads");
    }
    
    /**
     * Start performance monitoring
     */
    private void startPerformanceMonitoring() {
        maintenanceExecutor.scheduleWithFixedDelay(this::updatePerformanceMetrics, 5, 5, TimeUnit.SECONDS);
        maintenanceExecutor.scheduleWithFixedDelay(this::performMaintenanceTasks, 30, 30, TimeUnit.SECONDS);
        maintenanceExecutor.scheduleWithFixedDelay(this::checkMemoryPressure, 10, 10, TimeUnit.SECONDS);
        
        Log.d(TAG, "Performance monitoring started");
    }
    
    /**
     * Update performance metrics
     */
    private void updatePerformanceMetrics() {
        try {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            
            Debug.MemoryInfo debugMemInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemInfo);
            
            // Calculate CPU usage (simplified)
            float cpuUsage = calculateCpuUsage();
            
            int activeThreads = Thread.getAllStackTraces().size();
            
            currentMetrics = new PerformanceMetrics(
                memInfo.totalMem,
                memInfo.availMem,
                cpuUsage,
                activeThreads
            );
            
            currentMetrics.cacheSize = memoryCache.size();
            currentMetrics.cacheHitRate = calculateCacheHitRate();
            currentMetrics.backgroundTasks = getBackgroundTaskCount();
            currentMetrics.isLowMemoryMode = isLowMemoryMode;
            currentMetrics.isBatteryOptimized = isBatteryOptimized;
            
            // Check for performance issues
            checkPerformanceThresholds();
            
            if (performanceListener != null) {
                mainHandler.post(() -> performanceListener.onPerformanceMetricsUpdated(currentMetrics));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating performance metrics", e);
        }
    }
    
    /**
     * Calculate CPU usage percentage
     */
    private float calculateCpuUsage() {
        try {
            // This is a simplified CPU usage calculation
            // In practice, you'd use more sophisticated methods
            Runtime runtime = Runtime.getRuntime();
            long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
            long afterUsed = beforeUsed; // Placeholder
            
            // Simple heuristic: if memory usage is high, assume high CPU
            if (currentMetrics != null) {
                return Math.min(currentMetrics.memoryUsagePercent, 100.0f);
            }
            
            return 0.0f;
        } catch (Exception e) {
            Log.w(TAG, "Error calculating CPU usage", e);
            return 0.0f;
        }
    }
    
    /**
     * Check performance thresholds
     */
    private void checkPerformanceThresholds() {
        if (currentMetrics == null) return;
        
        // Memory warnings
        if (currentMetrics.availableMemory < CRITICAL_MEMORY_THRESHOLD) {
            if (performanceListener != null) {
                mainHandler.post(() -> performanceListener.onMemoryCritical(currentMetrics.availableMemory));
            }
            enableLowMemoryMode();
        } else if (currentMetrics.availableMemory < LOW_MEMORY_THRESHOLD) {
            if (performanceListener != null) {
                mainHandler.post(() -> performanceListener.onMemoryWarning(currentMetrics.availableMemory));
            }
        }
        
        // High CPU usage detection
        if (currentMetrics.cpuUsagePercent > 80.0f) {
            if (performanceListener != null) {
                mainHandler.post(() -> performanceListener.onHighCpuUsageDetected());
            }
        }
        
        // High memory usage detection
        if (currentMetrics.memoryUsagePercent > 90.0f) {
            if (performanceListener != null) {
                mainHandler.post(() -> performanceListener.onLowPerformanceDetected());
            }
        }
    }
    
    /**
     * Enable low memory mode
     */
    private void enableLowMemoryMode() {
        if (isLowMemoryMode) return;
        
        isLowMemoryMode = true;
        Log.w(TAG, "Low memory mode enabled");
        
        // Reduce cache sizes
        memoryCache.evictAll();
        thumbnailCache.evictAll();
        
        // Force garbage collection
        if (enableGarbageCollection) {
            System.gc();
        }
        
        // Reduce thread pool size
        if (backgroundExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
            ((java.util.concurrent.ThreadPoolExecutor) backgroundExecutor).setCorePoolSize(1);
        }
    }
    
    /**
     * Disable low memory mode
     */
    public void disableLowMemoryMode() {
        if (!isLowMemoryMode) return;
        
        isLowMemoryMode = false;
        Log.d(TAG, "Low memory mode disabled");
        
        // Restore normal cache sizes
        // (This would be done by reinitializing the cache)
        
        // Restore thread pool size
        if (backgroundExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
            ((java.util.concurrent.ThreadPoolExecutor) backgroundExecutor).setCorePoolSize(maxThreadCount);
        }
    }
    
    /**
     * Check for memory pressure
     */
    private void checkMemoryPressure() {
        if (!enableMemoryManagement) return;
        
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        if (memInfo.availMem < LOW_MEMORY_THRESHOLD) {
            performMemoryCleanup();
        }
    }
    
    /**
     * Perform memory cleanup
     */
    public void performMemoryCleanup() {
        Log.d(TAG, "Performing memory cleanup");
        
        // Clear caches if needed
        if (memoryCache.size() > MAX_CACHE_SIZE / 2) {
            memoryCache.evictAll();
        }
        
        if (thumbnailCache.size() > MAX_CACHE_SIZE / 4) {
            thumbnailCache.evictAll();
        }
        
        // Run cleanup callbacks
        for (ResourceCleanupCallback callback : new ArrayList<>(cleanupCallbacks)) {
            try {
                if (isResourceStale(callback)) {
                    callback.cleanup();
                    cleanupCallbacks.remove(callback);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error in cleanup callback: " + callback.getResourceId(), e);
            }
        }
        
        // Force garbage collection if enabled
        if (enableGarbageCollection) {
            System.gc();
            Runtime.getRuntime().gc();
        }
    }
    
    /**
     * Check if resource is stale (not used recently)
     */
    private boolean isResourceStale(ResourceCleanupCallback callback) {
        long lastUsed = resourceUsage.getOrDefault(callback.getResourceId(), 0L);
        long timeSinceUsed = System.currentTimeMillis() - lastUsed;
        return timeSinceUsed > 300000; // 5 minutes
    }
    
    /**
     * Perform maintenance tasks
     */
    private void performMaintenanceTasks() {
        try {
            // Clean up stale resources
            for (String resourceId : new ArrayList<>(resourceUsage.keySet())) {
                if (isResourceStale(new ResourceCleanupCallback() {
                    @Override
                    public void cleanup() {}
                    
                    @Override
                    public String getResourceId() {
                        return resourceId;
                    }
                    
                    @Override
                    public long getLastUsedTime() {
                        return resourceUsage.getOrDefault(resourceId, 0L);
                    }
                })) {
                    resourceUsage.remove(resourceId);
                }
            }
            
            // Optimize caches
            if (enableCacheOptimization) {
                optimizeCaches();
            }
            
            // Update battery optimization status
            if (enableBatteryOptimization) {
                updateBatteryOptimizationStatus();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing maintenance tasks", e);
        }
    }
    
    /**
     * Optimize caches
     */
    private void optimizeCaches() {
        // Remove least recently used items if cache is full
        if (memoryCache.size() >= memoryCache.maxSize()) {
            // LRU cache will handle this automatically, but we can add custom logic
        }
        
        if (thumbnailCache.size() >= thumbnailCache.maxSize()) {
            // Same for thumbnail cache
        }
    }
    
    /**
     * Update battery optimization status
     */
    private void updateBatteryOptimizationStatus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Check if app is optimized for battery
                isBatteryOptimized = activityManager.isBackgroundRestricted();
                
                if (isBatteryOptimized && performanceListener != null) {
                    mainHandler.post(() -> performanceListener.onBatteryOptimizationEnabled());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking battery optimization status", e);
        }
    }
    
    /**
     * Execute background task with optimization
     */
    public void executeBackgroundTask(BackgroundTask task) {
        if (task == null) return;
        
        // Check if we should execute this task
        if (isLowMemoryMode && task.getPriority() == TaskPriority.LOW) {
            Log.d(TAG, "Skipping low priority task due to low memory mode: " + task.getTaskId());
            return;
        }
        
        backgroundExecutor.submit(() -> {
            try {
                Log.d(TAG, "Executing background task: " + task.getTaskId());
                
                // Mark resource as used
                resourceUsage.put(task.getTaskId(), System.currentTimeMillis());
                
                // Execute the task
                task.execute();
                
                // Cache result if needed
                if (task.shouldCacheResult() && task.getEstimatedDuration() > 1000) {
                    // This would be implemented based on the specific task type
                }
                
                Log.d(TAG, "Background task completed: " + task.getTaskId());
                
            } catch (Exception e) {
                Log.e(TAG, "Error executing background task: " + task.getTaskId(), e);
            }
        });
    }
    
    /**
     * Execute background task with priority
     */
    public void executePriorityBackgroundTask(BackgroundTask task) {
        if (task == null) return;
        
        // High priority tasks get immediate execution
        if (task.getPriority() == TaskPriority.CRITICAL || task.getPriority() == TaskPriority.HIGH) {
            mainHandler.post(() -> {
                try {
                    task.execute();
                } catch (Exception e) {
                    Log.e(TAG, "Error executing priority task: " + task.getTaskId(), e);
                }
            });
        } else {
            executeBackgroundTask(task);
        }
    }
    
    /**
     * Cache object with lazy loading support
     */
    public <T> T getFromCache(String key, CacheProvider<T> provider) {
        if (!enableLazyLoading) {
            return provider.provide();
        }
        
        @SuppressWarnings("unchecked")
        T value = (T) memoryCache.get(key);
        
        if (value == null && provider != null) {
            value = provider.provide();
            if (value != null) {
                memoryCache.put(key, value);
            }
        }
        
        if (value != null) {
            resourceUsage.put(key, System.currentTimeMillis());
        }
        
        return value;
    }
    
    /**
     * Cache thumbnail
     */
    public void cacheThumbnail(String videoPath, byte[] thumbnailData) {
        if (!enableBackgroundThumbnails) return;
        
        if (thumbnailData != null && videoPath != null) {
            thumbnailCache.put(videoPath, thumbnailData);
        }
    }
    
    /**
     * Get cached thumbnail
     */
    public byte[] getCachedThumbnail(String videoPath) {
        if (videoPath == null) return null;
        
        byte[] thumbnail = thumbnailCache.get(videoPath);
        if (thumbnail != null) {
            resourceUsage.put("thumbnail_" + videoPath, System.currentTimeMillis());
        }
        
        return thumbnail;
    }
    
    /**
     * Add resource cleanup callback
     */
    public void addResourceCleanupCallback(ResourceCleanupCallback callback) {
        if (callback != null && !cleanupCallbacks.contains(callback)) {
            cleanupCallbacks.add(callback);
        }
    }
    
    /**
     * Remove resource cleanup callback
     */
    public void removeResourceCleanupCallback(ResourceCleanupCallback callback) {
        cleanupCallbacks.remove(callback);
    }
    
    /**
     * Check if app should request battery optimization exemption
     */
    public void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                intent.setAction(BATTERY_OPTIMIZATION_INTENT);
                intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
                Log.d(TAG, "Battery optimization exemption request initiated");
            } catch (Exception e) {
                Log.w(TAG, "Error requesting battery optimization exemption", e);
            }
        }
    }
    
    /**
     * Calculate cache hit rate
     */
    private long calculateCacheHitRate() {
        // This would require tracking cache hits and misses
        // For now, return a simple calculation
        return (long) (memoryCache.hitCount() * 100.0 / 
                      (memoryCache.hitCount() + memoryCache.missCount()));
    }
    
    /**
     * Get background task count
     */
    private long getBackgroundTaskCount() {
        if (backgroundExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
            return ((java.util.concurrent.ThreadPoolExecutor) backgroundExecutor).getActiveCount();
        }
        return 0;
    }
    
    /**
     * Set performance listener
     */
    public void setPerformanceListener(PerformanceListener listener) {
        this.performanceListener = listener;
    }
    
    /**
     * Get current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        return currentMetrics;
    }
    
    /**
     * Check if in low memory mode
     */
    public boolean isInLowMemoryMode() {
        return isLowMemoryMode;
    }
    
    /**
     * Check if battery is optimized
     */
    public boolean isBatteryOptimized() {
        return isBatteryOptimized;
    }
    
    /**
     * Enable/disable optimization features
     */
    public void setOptimizationSettings(boolean enableLazyLoading, boolean enableBackgroundThumbnails,
                                       boolean enableMemoryManagement, boolean enableBatteryOptimization,
                                       boolean enableGarbageCollection, boolean enableCacheOptimization) {
        this.enableLazyLoading = enableLazyLoading;
        this.enableBackgroundThumbnails = enableBackgroundThumbnails;
        this.enableMemoryManagement = enableMemoryManagement;
        this.enableBatteryOptimization = enableBatteryOptimization;
        this.enableGarbageCollection = enableGarbageCollection;
        this.enableCacheOptimization = enableCacheOptimization;
        
        Log.d(TAG, "Optimization settings updated");
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Shutdown executors
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        if (maintenanceExecutor != null && !maintenanceExecutor.isShutdown()) {
            maintenanceExecutor.shutdown();
        }
        
        // Clear caches
        memoryCache.evictAll();
        thumbnailCache.evictAll();
        persistentCache.clear();
        
        // Clear callbacks
        cleanupCallbacks.clear();
        resourceUsage.clear();
        
        Log.d(TAG, "PerformanceOptimizer cleaned up");
    }
    
    /**
     * Cache provider interface for lazy loading
     */
    public interface CacheProvider<T> {
        T provide();
    }
}