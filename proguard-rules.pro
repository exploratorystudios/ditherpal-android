# ProGuard rules for DitherPal

# Keep all classes in our package
-keep class com.ditherpal.** { *; }

# Keep data classes
-keepclassmembers class com.ditherpal.ui.DitherState {
    <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Coroutines
-keep class kotlin.coroutines.** { *; }
-keep interface kotlin.coroutines.** { *; }

# Android Lifecycle
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# Material3
-keep class androidx.compose.material3.** { *; }
-keep interface androidx.compose.material3.** { *; }

# Keep enum values
-keepclassmembers enum com.ditherpal.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom application classes
-keep public class com.ditherpal.MainActivity { *; }

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# Rename SourceFile to something generic
-renamesourcefileattribute SourceFile

# Optimization options
-optimizationpasses 5
-dontusemixedcaseclassnames

# Removing logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
