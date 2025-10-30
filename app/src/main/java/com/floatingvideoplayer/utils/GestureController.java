package com.floatingvideoplayer.utils;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced gesture controller for window interactions
 * Handles drag & drop, pinch zoom, swipe gestures, and more
 */
public class GestureController {
    
    private static final String TAG = "GestureController";
    
    // Gesture constants
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final float SCALE_FACTOR_MIN = 0.5f;
    private static final float SCALE_FACTOR_MAX = 3.0f;
    private static final long DOUBLE_TAP_TIMEOUT = 300; // milliseconds
    
    // Drag and drop
    public static final String MIME_TYPE_VIDEO = "video/*";
    public static final String MIME_TYPE_AUDIO = "audio/*";
    public static final String MIME_TYPE_FILE = "application/*";
    
    /**
     * Gesture types
     */
    public enum GestureType {
        TAP,
        DOUBLE_TAP,
        LONG_PRESS,
        DRAG,
        PINCH_ZOOM,
        SWIPE_UP,
        SWIPE_DOWN,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        DROP,
        EDGE_DRAG
    }
    
    /**
     * Drag and drop information
     */
    public static class DragDropInfo {
        public String sourceWindowId;
        public String targetWindowId;
        public String filePath;
        public String mimeType;
        public PointF dropPosition;
        public boolean isValid;
        public long timestamp;
        
        public DragDropInfo() {
            this.timestamp = System.currentTimeMillis();
            this.isValid = false;
        }
    }
    
    /**
     * Gesture listener interface
     */
    public interface GestureListener {
        void onGestureDetected(GestureType type, View view, MotionEvent event);
        void onDragStart(View view, MotionEvent event);
        void onDragMove(View view, MotionEvent event, float deltaX, float deltaY);
        void onDragEnd(View view, MotionEvent event);
        void onPinchZoomStart(View view, ScaleGestureDetector detector);
        void onPinchZoom(View view, ScaleGestureDetector detector, float scaleFactor);
        void onPinchZoomEnd(View view, ScaleGestureDetector detector);
        void onSwipeGesture(View view, MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY);
        void onDoubleTap(View view, MotionEvent event);
        void onLongPress(View view, MotionEvent event);
        void onEdgeDrag(View view, MotionEvent event, int edge);
        void onDrop(DragDropInfo dropInfo);
    }
    
    /**
     * Window bounds for edge detection
     */
    public static class WindowBounds {
        public int left, top, right, bottom;
        public int width, height;
        public int edgeThreshold = 20; // pixels from edge to trigger edge drag
        
        public WindowBounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.width = right - left;
            this.height = bottom - top;
        }
        
        public boolean isInLeftEdge(int x, int y) {
            return x >= left && x <= left + edgeThreshold && y >= top && y <= bottom;
        }
        
        public boolean isInRightEdge(int x, int y) {
            return x >= right - edgeThreshold && x <= right && y >= top && y <= bottom;
        }
        
        public boolean isInTopEdge(int x, int y) {
            return x >= left && x <= right && y >= top && y <= top + edgeThreshold;
        }
        
        public boolean isInBottomEdge(int x, int y) {
            return x >= left && x <= right && y >= bottom - edgeThreshold && y <= bottom;
        }
        
        public boolean isInAnyEdge(int x, int y) {
            return isInLeftEdge(x, y) || isInRightEdge(x, y) || 
                   isInTopEdge(x, y) || isInBottomEdge(x, y);
        }
    }
    
    private Context context;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureListener gestureListener;
    
    // State tracking
    private boolean isDragging = false;
    private boolean isScaling = false;
    private boolean isLongPress = false;
    private PointF lastTouchPoint = new PointF();
    private PointF dragStartPoint = new PointF();
    private WindowManager.LayoutParams currentLayoutParams;
    private WindowBounds currentWindowBounds;
    private String currentWindowId;
    
    // Gesture history
    private long lastTapTime = 0;
    private int tapCount = 0;
    private MotionEvent lastTapEvent = null;
    private Handler gestureHandler = new Handler(Looper.getMainLooper());
    
    // Drag and drop tracking
    private boolean isDragDropMode = false;
    private DragDropInfo currentDragDropInfo;
    private ConcurrentHashMap<String, View> droppableViews;
    private ConcurrentHashMap<String, View> draggableViews;
    
    public GestureController(Context context, GestureListener listener) {
        this.context = context;
        this.gestureListener = listener;
        this.droppableViews = new ConcurrentHashMap<>();
        this.draggableViews = new ConcurrentHashMap<>();
        
        setupGestureDetectors();
    }
    
    /**
     * Setup gesture detectors
     */
    private void setupGestureDetectors() {
        // Regular gesture detector for taps, swipes, etc.
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.d(TAG, "Single tap confirmed");
                if (gestureListener != null) {
                    gestureListener.onGestureDetected(GestureType.TAP, getView(), e);
                }
                return true;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d(TAG, "Double tap detected");
                if (gestureListener != null) {
                    gestureListener.onDoubleTap(getView(), e);
                }
                return true;
            }
            
            @Override
            public void onLongPress(MotionEvent e) {
                Log.d(TAG, "Long press detected");
                isLongPress = true;
                if (gestureListener != null) {
                    gestureListener.onLongPress(getView(), e);
                }
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isDragging && !isLongPress) {
                    // Start dragging
                    startDrag(e1, e2);
                } else if (isDragging) {
                    // Continue dragging
                    onDragMove(e2, distanceX, distanceY);
                }
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d(TAG, "Fling detected: " + velocityX + ", " + velocityY);
                
                if (gestureListener != null) {
                    gestureListener.onSwipeGesture(getView(), e1, e2, velocityX, velocityY);
                }
                
                return true;
            }
        });
        
        // Scale gesture detector for pinch zoom
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float scaleFactor = 1.0f;
            private PointF focalPoint = new PointF();
            
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                scaleFactor = 1.0f;
                
                if (gestureListener != null) {
                    gestureListener.onPinchZoomStart(getView(), detector);
                }
                
                return true;
            }
            
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(SCALE_FACTOR_MIN, Math.min(SCALE_FACTOR_MAX, scaleFactor));
                
                focalPoint.set(detector.getFocusX(), detector.getFocusY());
                
                if (gestureListener != null) {
                    gestureListener.onPinchZoom(getView(), detector, scaleFactor);
                }
                
                return true;
            }
            
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
                
                if (gestureListener != null) {
                    gestureListener.onPinchZoomEnd(getView(), detector);
                }
            }
        });
    }
    
    /**
     * Handle touch events
     */
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        
        // Handle double tap timing
        handleTapCounting(event);
        
        // Check for edge dragging first
        if (event.getAction() == MotionEvent.ACTION_DOWN && currentWindowBounds != null) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            
            if (currentWindowBounds.isInAnyEdge(x, y)) {
                if (gestureListener != null) {
                    int edge = getEdgeFromPosition(x, y, currentWindowBounds);
                    gestureListener.onEdgeDrag(getView(), event, edge);
                }
            }
        }
        
        // Handle scale gestures
        if (scaleGestureDetector.onTouchEvent(event)) {
            handled = true;
        }
        
        // Handle regular gestures
        if (!isScaling) {
            handled = gestureDetector.onTouchEvent(event) || handled;
        }
        
        // Handle drag and drop
        if (isDragDropMode && event.getAction() == MotionEvent.ACTION_UP) {
            handleDrop(event);
        }
        
        // Track drag state
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isDragging = false;
            isLongPress = false;
            lastTouchPoint.set(event.getX(), event.getY());
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isDragging && gestureListener != null) {
                gestureListener.onDragEnd(getView(), event);
            }
            isDragging = false;
        }
        
        return handled;
    }
    
    /**
     * Handle tap counting for double tap detection
     */
    private void handleTapCounting(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT && 
                lastTapEvent != null && 
                isNear(lastTapEvent.getX(), lastTapEvent.getY(), event.getX(), event.getY())) {
                tapCount++;
            } else {
                tapCount = 1;
            }
            
            lastTapTime = currentTime;
            lastTapEvent = event;
        }
    }
    
    /**
     * Check if two points are near each other
     */
    private boolean isNear(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (dx * dx + dy * dy) < (SWIPE_THRESHOLD * SWIPE_THRESHOLD);
    }
    
    /**
     * Start drag operation
     */
    private void startDrag(MotionEvent startEvent, MotionEvent currentEvent) {
        if (isDragging || isLongPress) return;
        
        isDragging = true;
        dragStartPoint.set(startEvent.getX(), startEvent.getY());
        
        if (gestureListener != null) {
            gestureListener.onDragStart(getView(), startEvent);
        }
    }
    
    /**
     * Handle drag movement
     */
    private void onDragMove(MotionEvent event, float deltaX, float deltaY) {
        if (gestureListener != null) {
            gestureListener.onDragMove(getView(), event, deltaX, deltaY);
        }
    }
    
    /**
     * Enable drag and drop mode
     */
    public void enableDragDropMode(String windowId, DragDropInfo dropInfo) {
        this.currentWindowId = windowId;
        this.currentDragDropInfo = dropInfo;
        this.isDragDropMode = true;
        Log.d(TAG, "Drag & drop mode enabled for window: " + windowId);
    }
    
    /**
     * Disable drag and drop mode
     */
    public void disableDragDropMode() {
        this.isDragDropMode = false;
        this.currentDragDropInfo = null;
        this.currentWindowId = null;
        Log.d(TAG, "Drag & drop mode disabled");
    }
    
    /**
     * Register a droppable view
     */
    public void registerDroppableView(String windowId, View view) {
        droppableViews.put(windowId, view);
    }
    
    /**
     * Unregister a droppable view
     */
    public void unregisterDroppableView(String windowId) {
        droppableViews.remove(windowId);
    }
    
    /**
     * Register a draggable view
     */
    public void registerDraggableView(String windowId, View view) {
        draggableViews.put(windowId, view);
    }
    
    /**
     * Unregister a draggable view
     */
    public void unregisterDraggableView(String windowId) {
        draggableViews.remove(windowId);
    }
    
    /**
     * Handle drop operation
     */
    private void handleDrop(MotionEvent event) {
        if (!isDragDropMode || currentDragDropInfo == null) return;
        
        float x = event.getX();
        float y = event.getY();
        
        // Check which droppable view received the drop
        String targetWindowId = findDropTargetAt(x, y);
        
        if (targetWindowId != null) {
            currentDragDropInfo.targetWindowId = targetWindowId;
            currentDragDropInfo.dropPosition.set(x, y);
            currentDragDropInfo.isValid = true;
            
            Log.d(TAG, "Drop detected: " + currentDragDropInfo.filePath + " -> " + targetWindowId);
            
            if (gestureListener != null) {
                gestureListener.onDrop(currentDragDropInfo);
            }
        } else {
            Log.d(TAG, "Drop failed: No valid target at position (" + x + ", " + y + ")");
            Toast.makeText(context, "Drop failed: Invalid target", Toast.LENGTH_SHORT).show();
        }
        
        disableDragDropMode();
    }
    
    /**
     * Find drop target at given coordinates
     */
    private String findDropTargetAt(float x, float y) {
        for (String windowId : droppableViews.keySet()) {
            View view = droppableViews.get(windowId);
            if (view != null && isPointInView(x, y, view)) {
                return windowId;
            }
        }
        return null;
    }
    
    /**
     * Check if point is within view bounds
     */
    private boolean isPointInView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return x >= location[0] && x <= location[0] + view.getWidth() &&
               y >= location[1] && y <= location[1] + view.getHeight();
    }
    
    /**
     * Set current window layout parameters
     */
    public void setCurrentWindowParams(WindowManager.LayoutParams params) {
        this.currentLayoutParams = params;
    }
    
    /**
     * Set current window bounds
     */
    public void setCurrentWindowBounds(WindowBounds bounds) {
        this.currentWindowBounds = bounds;
    }
    
    /**
     * Get edge from position
     */
    private int getEdgeFromPosition(int x, int y, WindowBounds bounds) {
        if (bounds.isInLeftEdge(x, y)) return ViewListDragSortUtils.DRAG;
        if (bounds.isInRightEdge(x, y)) return ViewListDragSortUtils.DRAG;
        if (bounds.isInTopEdge(x, y)) return ViewListDragSortUtils.DRAG;
        if (bounds.isInBottomEdge(x, y)) return ViewListDragSortUtils.DRAG;
        return 0;
    }
    
    /**
     * Get view associated with gesture controller
     */
    private View getView() {
        // This would need to be set externally or retrieved from context
        return null; // Placeholder - should be implemented based on usage
    }
    
    /**
     * Check if currently dragging
     */
    public boolean isDragging() {
        return isDragging;
    }
    
    /**
     * Check if currently scaling
     */
    public boolean isScaling() {
        return isScaling;
    }
    
    /**
     * Check if currently in long press
     */
    public boolean isLongPress() {
        return isLongPress;
    }
    
    /**
     * Check if in drag & drop mode
     */
    public boolean isDragDropMode() {
        return isDragDropMode;
    }
    
    /**
     * Get current drag & drop info
     */
    public DragDropInfo getCurrentDragDropInfo() {
        return currentDragDropInfo;
    }
    
    /**
     * Set gesture listener
     */
    public void setGestureListener(GestureListener listener) {
        this.gestureListener = listener;
    }
    
    /**
     * Enable/disable specific gestures
     */
    private boolean enableGestures = true;
    private boolean enableDrag = true;
    private boolean enablePinchZoom = true;
    private boolean enableSwipe = true;
    private boolean enableDoubleTap = true;
    private boolean enableLongPress = true;
    private boolean enableEdgeDrag = true;
    
    public void setEnableGestures(boolean enable) {
        this.enableGestures = enable;
    }
    
    public void setEnableDrag(boolean enable) {
        this.enableDrag = enable;
    }
    
    public void setEnablePinchZoom(boolean enable) {
        this.enablePinchZoom = enable;
    }
    
    public void setEnableSwipe(boolean enable) {
        this.enableSwipe = enable;
    }
    
    public void setEnableDoubleTap(boolean enable) {
        this.enableDoubleTap = enable;
    }
    
    public void setEnableLongPress(boolean enable) {
        this.enableLongPress = enable;
    }
    
    public void setEnableEdgeDrag(boolean enable) {
        this.enableEdgeDrag = enable;
    }
    
    // Utility classes that need to be imported/created
    private static class ViewListDragSortUtils {
        public static final int DRAG = 1;
        // Add other constants as needed
    }
}