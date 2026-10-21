# Keep rules for the release build (R8).

# WorkManager (used by libraries) creates its Room database by reflection; R8 must not remove the generated
# implementation classes.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.** { *; }

# The embedded YouTube player calls back into Kotlin from JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Crash logs stay readable when shared from Settings.
-keepattributes SourceFile,LineNumberTable

# ML Kit Digital Ink is a closed library that uses reflection and relies on its own initialisation order, so it is
# kept as shipped: without these rules the Japanese model is not found and its internal Random fails at start-up.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_digital_ink.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
