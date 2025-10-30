package com.floatingvideoplayer.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages smooth animations for window transitions and state changes
 */
public class WindowAnimationManager {
    
    private static final String TAG = "WindowAnimationManager";
    
    // Animation durations
    public static final long DURATION_FAST = 150;      // Quick animations
    public static final long DURATION_NORMAL = 300;    // Standard animations
    public static final long DURATION_SLOW = 500;      // Slow animations
    public static final long DURATION_VERY_SLOW = 800; // Complex animations
    
    // Animation types
    public static final String ANIMATION_FADE = "fade";
    public static final String ANIMATION_SLIDE = "slide";
    public static final String ANIMATION_SCALE = "scale";
    public static final String ANIMATION_ZOOM = "zoom";
    public static final String ANIMATION_ROTATE = "rotate";
    public static final String ANIMATION_BOUNCE = "bounce";
    public static final String ANIMATION_SMOOTH = "smooth";
    public static final String ANIMATION_NONE = "none";
    
    private Context context;
    private Handler mainHandler;
    private ConcurrentHashMap<String, Animator> activeAnimations;
    private ConcurrentHashMap<String, Animation> viewAnimations;
    
    /**
     * Animation completion listener
     */
    public interface AnimationListener {
        void onAnimationStart(String animationId);
        void onAnimationEnd(String animationId, boolean completed);
        void onAnimationCancel(String animationId);
    }
    
    /**
     * Animation configuration
     */
    public static class AnimationConfig {
        public String animationType = ANIMATION_SMOOTH;
        public long duration = DURATION_NORMAL;
        public float startAlpha = 1.0f;
        public float endAlpha = 1.0f;
        public float startScale = 1.0f;
        public float endScale = 1.0f;
        public int startX = 0;
        public int startY = 0;
        public int endX = 0;
        public int endY = 0;
        public float startRotation = 0f;
        public float endRotation = 0f;
        public boolean fadeIn = false;
        public boolean fadeOut = false;
        public boolean bounce = false;
        public boolean elastic = false;
        public AnimationListener listener;
        
        public AnimationConfig() {}
        
        public AnimationConfig(String type, long duration) {
            this.animationType = type;
            this.duration = duration;
        }
    }
    
    public WindowAnimationManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.activeAnimations = new ConcurrentHashMap<>();
        this.viewAnimations = new ConcurrentHashMap<>();
    }
    
    /**
     * Animate window position change
     */
    public void animateWindowPosition(String animationId, View view, WindowManager.LayoutParams params,
                                    int newX, int newY, AnimationListener listener) {
        if (view == null || params == null) return;
        
        // Cancel existing animation for this window
        cancelAnimation(animationId);
        
        ValueAnimator animator = ValueAnimator.ofInt(
            createValueArray(params.x, params.y, newX, newY)
        );
        
        animator.setDuration(DURATION_NORMAL);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int[] values = (int[]) animation.getAnimatedValue();
                params.x = values[2];
                params.y = values[3];
                // Note: This would need access to the actual view to update layout
                // In practice, you'd pass the WindowManager instance as well
            }
        });
        
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationCancel(animationId));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        activeAnimations.put(animationId, animator);
        animator.start();
    }
    
    /**
     * Animate window size change
     */
    public void animateWindowSize(String animationId, View view, WindowManager.LayoutParams params,
                                int newWidth, int newHeight, AnimationListener listener) {
        if (view == null || params == null) return;
        
        cancelAnimation(animationId);
        
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, newWidth);
        ValueAnimator heightAnimator = ValueAnimator.ofInt(params.height, newHeight);
        
        widthAnimator.setDuration(DURATION_NORMAL);
        heightAnimator.setDuration(DURATION_NORMAL);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(widthAnimator, heightAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.width = (int) animation.getAnimatedValue();
            }
        });
        
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.height = (int) animation.getAnimatedValue();
            }
        });
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationCancel(animationId));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        activeAnimations.put(animationId, animatorSet);
        animatorSet.start();
    }
    
    /**
     * Animate window visibility (show/hide)
     */
    public void animateWindowVisibility(String animationId, View view, boolean show, AnimationListener listener) {
        if (view == null) return;
        
        cancelAnimation(animationId);
        
        Animation animation;
        if (show) {
            // Fade in and scale up
            animation = new ScaleAnimation(
                0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            animation.setDuration(DURATION_NORMAL);
            view.setVisibility(View.VISIBLE);
        } else {
            // Fade out and scale down
            animation = new ScaleAnimation(
                1.0f, 0.0f, 1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            animation.setDuration(DURATION_NORMAL);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (listener != null) {
                        mainHandler.post(() -> listener.onAnimationStart(animationId));
                    }
                }
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                    viewAnimations.remove(animationId);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                    }
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {}
                
                @Override
                public void onAnimationCancel(Animation animation) {
                    viewAnimations.remove(animationId);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onAnimationCancel(animationId));
                    }
                }
            });
        }
        
        AlphaAnimation alphaAnimation = new AlphaAnimation(
            show ? 0.0f : 1.0f,
            show ? 1.0f : 0.0f
        );
        alphaAnimation.setDuration(DURATION_NORMAL);
        
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(animation);
        
        viewAnimations.put(animationId, animationSet);
        view.startAnimation(animationSet);
    }
    
    /**
     * Animate window minimize
     */
    public void animateWindowMinimize(String animationId, View view, WindowManager.LayoutParams params,
                                    int minimizeToX, int minimizeToY, AnimationListener listener) {
        if (view == null || params == null) return;
        
        cancelAnimation(animationId);
        
        // Combine position and size animations for minimize
        AnimatorSet animatorSet = new AnimatorSet();
        
        ValueAnimator xAnimator = ValueAnimator.ofInt(params.x, minimizeToX);
        ValueAnimator yAnimator = ValueAnimator.ofInt(params.y, minimizeToY);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, 100); // Small size
        ValueAnimator heightAnimator = ValueAnimator.ofInt(params.height, 100);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(params.alpha, 0.7f);
        
        xAnimator.setDuration(DURATION_NORMAL);
        yAnimator.setDuration(DURATION_NORMAL);
        widthAnimator.setDuration(DURATION_NORMAL);
        heightAnimator.setDuration(DURATION_NORMAL);
        alphaAnimator.setDuration(DURATION_NORMAL);
        
        animatorSet.playTogether(xAnimator, yAnimator, widthAnimator, heightAnimator, alphaAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        xAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.x = (int) animation.getAnimatedValue();
            }
        });
        
        yAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.y = (int) animation.getAnimatedValue();
            }
        });
        
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.width = (int) animation.getAnimatedValue();
            }
        });
        
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.height = (int) animation.getAnimatedValue();
            }
        });
        
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.alpha = (float) animation.getAnimatedValue();
            }
        });
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationCancel(animationId));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        activeAnimations.put(animationId, animatorSet);
        animatorSet.start();
    }
    
    /**
     * Animate window maximize/restore
     */
    public void animateWindowMaximize(String animationId, View view, WindowManager.LayoutParams params,
                                    int maxWidth, int maxHeight, AnimationListener listener) {
        if (view == null || params == null) return;
        
        cancelAnimation(animationId);
        
        AnimatorSet animatorSet = new AnimatorSet();
        
        ValueAnimator xAnimator = ValueAnimator.ofInt(params.x, 50); // Center position
        ValueAnimator yAnimator = ValueAnimator.ofInt(params.y, 50);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, maxWidth);
        ValueAnimator heightAnimator = ValueAnimator.ofInt(params.height, maxHeight);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(params.alpha, 1.0f);
        
        xAnimator.setDuration(DURATION_SLOW);
        yAnimator.setDuration(DURATION_SLOW);
        widthAnimator.setDuration(DURATION_SLOW);
        heightAnimator.setDuration(DURATION_SLOW);
        alphaAnimator.setDuration(DURATION_SLOW);
        
        animatorSet.playTogether(xAnimator, yAnimator, widthAnimator, heightAnimator, alphaAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        xAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.x = (int) animation.getAnimatedValue();
            }
        });
        
        yAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.y = (int) animation.getAnimatedValue();
            }
        });
        
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.width = (int) animation.getAnimatedValue();
            }
        });
        
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.height = (int) animation.getAnimatedValue();
            }
        });
        
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.alpha = (float) animation.getAnimatedValue();
            }
        });
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationCancel(animationId));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        activeAnimations.put(animationId, animatorSet);
        animatorSet.start();
    }
    
    /**
     * Animate window opacity change
     */
    public void animateWindowOpacity(String animationId, WindowManager.LayoutParams params,
                                   float newAlpha, AnimationListener listener) {
        if (params == null) return;
        
        cancelAnimation(animationId);
        
        ValueAnimator animator = ValueAnimator.ofFloat(params.alpha, newAlpha);
        animator.setDuration(DURATION_FAST);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.alpha = (float) animation.getAnimatedValue();
            }
        });
        
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.remove(animationId);
                if (listener != null) {
                    mainHandler.post(() -> listener.onAnimationCancel(animationId));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        activeAnimations.put(animationId, animator);
        animator.start();
    }
    
    /**
     * Perform custom animation with configuration
     */
    public void performCustomAnimation(String animationId, View view, AnimationConfig config) {
        if (view == null || config == null) return;
        
        cancelAnimation(animationId);
        
        switch (config.animationType) {
            case ANIMATION_FADE:
                performFadeAnimation(animationId, view, config);
                break;
            case ANIMATION_SLIDE:
                performSlideAnimation(animationId, view, config);
                break;
            case ANIMATION_SCALE:
                performScaleAnimation(animationId, view, config);
                break;
            case ANIMATION_ZOOM:
                performZoomAnimation(animationId, view, config);
                break;
            case ANIMATION_SMOOTH:
                performSmoothAnimation(animationId, view, config);
                break;
            case ANIMATION_NONE:
                // No animation, just execute listener
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
                break;
            default:
                Log.w(TAG, "Unknown animation type: " + config.animationType);
                break;
        }
    }
    
    /**
     * Cancel specific animation
     */
    public void cancelAnimation(String animationId) {
        Animator animator = activeAnimations.remove(animationId);
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        Animation viewAnim = viewAnimations.remove(animationId);
        if (viewAnim != null && !viewAnim.hasEnded()) {
            viewAnim.cancel();
        }
    }
    
    /**
     * Cancel all animations
     */
    public void cancelAllAnimations() {
        for (String animationId : new ArrayList<>(activeAnimations.keySet())) {
            cancelAnimation(animationId);
        }
        
        for (String animationId : new ArrayList<>(viewAnimations.keySet())) {
            cancelAnimation(animationId);
        }
    }
    
    /**
     * Check if animation is running
     */
    public boolean isAnimationRunning(String animationId) {
        Animator animator = activeAnimations.get(animationId);
        if (animator != null) {
            return animator.isRunning();
        }
        
        Animation viewAnim = viewAnimations.get(animationId);
        if (viewAnim != null) {
            return !viewAnim.hasEnded() && !viewAnim.hasStarted();
        }
        
        return false;
    }
    
    // Private helper methods
    
    private int[] createValueArray(int... values) {
        return values;
    }
    
    private void performFadeAnimation(String animationId, View view, AnimationConfig config) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(
            config.startAlpha, config.endAlpha
        );
        alphaAnimation.setDuration(config.duration);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
            
            @Override
            public void onAnimationCancel(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationCancel(animationId));
                }
            }
        });
        
        viewAnimations.put(animationId, alphaAnimation);
        view.startAnimation(alphaAnimation);
    }
    
    private void performSlideAnimation(String animationId, View view, AnimationConfig config) {
        TranslateAnimation slideAnimation = new TranslateAnimation(
            Animation.ABSOLUTE, config.startX,
            Animation.ABSOLUTE, config.endX,
            Animation.ABSOLUTE, config.startY,
            Animation.ABSOLUTE, config.endY
        );
        slideAnimation.setDuration(config.duration);
        slideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
            
            @Override
            public void onAnimationCancel(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationCancel(animationId));
                }
            }
        });
        
        viewAnimations.put(animationId, slideAnimation);
        view.startAnimation(slideAnimation);
    }
    
    private void performScaleAnimation(String animationId, View view, AnimationConfig config) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            config.startScale, config.endScale,
            config.startScale, config.endScale,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(config.duration);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
            
            @Override
            public void onAnimationCancel(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationCancel(animationId));
                }
            }
        });
        
        viewAnimations.put(animationId, scaleAnimation);
        view.startAnimation(scaleAnimation);
    }
    
    private void performZoomAnimation(String animationId, View view, AnimationConfig config) {
        // Combine scale and fade for zoom effect
        AlphaAnimation alphaAnimation = new AlphaAnimation(
            config.startAlpha, config.endAlpha
        );
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            config.startScale, config.endScale,
            config.startScale, config.endScale,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        
        alphaAnimation.setDuration(config.duration);
        scaleAnimation.setDuration(config.duration);
        
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);
        
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
            
            @Override
            public void onAnimationCancel(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationCancel(animationId));
                }
            }
        });
        
        viewAnimations.put(animationId, animationSet);
        view.startAnimation(animationSet);
    }
    
    private void performSmoothAnimation(String animationId, View view, AnimationConfig config) {
        // Use a combination of effects for smooth animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(
            config.startAlpha, config.endAlpha
        );
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            0.95f, config.endScale,
            0.95f, config.endScale,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        
        alphaAnimation.setDuration(config.duration);
        scaleAnimation.setDuration(config.duration);
        
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationStart(animationId));
                }
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationEnd(animationId, true));
                }
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
            
            @Override
            public void onAnimationCancel(Animation animation) {
                viewAnimations.remove(animationId);
                if (config.listener != null) {
                    mainHandler.post(() -> config.listener.onAnimationCancel(animationId));
                }
            }
        });
        
        viewAnimations.put(animationId, animationSet);
        view.startAnimation(animationSet);
    }
}