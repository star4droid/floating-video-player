# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# Floating Video Player ProGuard Rules
# ========================================

# Keep Application class
-keep public class com.floatingvideoplayer.FloatingVideoPlayerApp { *; }

# Keep all UI classes
-keep class com.floatingvideoplayer.ui.** { *; }
-keep class com.floatingvideoplayer.models.** { *; }

# Keep all service classes
-keep class com.floatingvideoplayer.services.** { *; }

# Keep all utility classes
-keep class com.floatingvideoplayer.utils.** { *; }

# ========================================
# Media3 ExoPlayer ProGuard Rules
# ========================================

# Media3 ExoPlayer specific rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }
-dontwarn androidx.media3.exoplayer.**
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.session.**

# Media3 common rules
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.common.**
-keep class androidx.media3.datasource.** { *; }
-dontwarn androidx.media3.datasource.**
-keep class androidx.media3.decoder.** { *; }
-dontwarn androidx.media3.decoder.**
-keep class androidx.media3.effect.** { *; }
-dontwarn androidx.media3.effect.**

# Media3 transformation rules
-keep class androidx.media3.transformer.** { *; }
-dontwarn androidx.media3.transformer.**

# ========================================
# Android Framework ProGuard Rules
# ========================================

# Keep WindowManager related classes
-keep class android.view.WindowManager { *; }
-keep class android.view.WindowManager.LayoutParams { *; }

# Keep Service lifecycle methods
-keepclassmembers class * extends android.app.Service {
    public void *(android.content.Intent);
    public void *(android.content.Intent, int);
}

# Keep BroadcastReceiver lifecycle methods
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public void *(android.content.Context, android.content.Intent);
}

# Keep Activity lifecycle methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.os.Bundle);
    protected void *(android.os.Bundle);
}

# ========================================
# Android X ProGuard Rules
# ========================================

# AndroidX Core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# AndroidX AppCompat
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# AndroidX Activity
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**

# AndroidX Fragment
-keep class androidx.fragment.** { *; }
-dontwarn androidx.fragment.**

# AndroidX RecyclerView
-keep class androidx.recyclerview.** { *; }
-dontwarn androidx.recyclerview.**

# AndroidX CardView
-keep class androidx.cardview.** { *; }
-dontwarn androidx.cardview.**

# AndroidX DocumentFile
-keep class androidx.documentfile.** { *; }
-dontwarn androidx.documentfile.**

# ========================================
# Material Design ProGuard Rules
# ========================================

# Material Design Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ========================================
# File System ProGuard Rules
# ========================================

# File access utilities
-keep class java.nio.file.** { *; }
-dontwarn java.nio.file.**

# ========================================
# Performance Optimization Rules
# ========================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ========================================
# Reflection and Dynamic Code
# ========================================

# Keep classes that use reflection
-keepattributes Signature, InnerClasses, EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# Parcelable Implementation
# ========================================

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# ========================================
# JSON and Serialization
# ========================================

# Keep Gson classes if using
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ========================================
# Media and Image Processing
# ========================================

# Image processing libraries
-keep class android.graphics.** { *; }
-keep class android.media.** { *; }

# ========================================
# Memory Management
# ========================================

# Keep weak references for memory management
-keep class java.lang.ref.WeakReference { *; }
-keep class java.lang.ref.SoftReference { *; }

# ========================================
# Error Handling
# ========================================

# Keep exception classes
-keep class java.lang.Exception { *; }
-keep class java.lang.RuntimeException { *; }
-keep class java.io.IOException { *; }

# ========================================
# Accessibility
# ========================================

# Accessibility classes
-keep class android.view.accessibility.** { *; }
-keep class androidx.core.view.accessibility.** { *; }

# ========================================
# Overlay and Window Management
# ========================================

# Window overlay permissions and types
-keep class android.type.** { *; }
-keep class android.app.** { *; }
-dontwarn android.app.**

# ========================================
# Notifications
# ========================================

# Notification classes
-keep class android.app.Notification { *; }
-keep class android.app.NotificationManager { *; }

# ========================================
# Media Session and Controls
# ========================================

# Media session components
-keep class android.media.session.** { *; }
-keep class android.media.AudioManager { *; }

# ========================================
# Testing Rules (for debug builds)
# ========================================

# Test classes (only for debug builds)
#-keep class org.junit.** { *; }
#-keep class org.mockito.** { *; }
#-keep class org.powermock.** { *; }

# ========================================
# Additional Optimization Rules
# ========================================

# Remove dead code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Optimize for size
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Merge duplicate classes
-mergeinterfacesaggressively

# ========================================
# Final Safety Rules
# ========================================

# Safety net for unknown cases
-keepattributes *

# Don't obfuscate interfaces
-keepnames class * implements java.io.Serializable
-keepnames class * implements android.os.Parcelable

# Generic keep rule for all classes
-keep class !androidx.media3.**,!androidx.documentfile.** { *; }

# Final fallback rule
-keep class com.floatingvideoplayer.** { *; }
