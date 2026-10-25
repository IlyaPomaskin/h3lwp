# libGDX
-keep class com.badlogic.gdx.** { *; }
-keep class com.badlogic.gdx.backends.android.** { *; }
-dontwarn com.badlogic.gdx.**
-keepclassmembers class com.badlogic.gdx.utils.** {
    <fields>;
    <init>(...);
}

# libGDX uses reflection to instantiate AndroidLiveWallpaperService.Engine subclasses
-keep class * extends com.badlogic.gdx.backends.android.AndroidLiveWallpaperService { *; }
-keep class * extends com.badlogic.gdx.ApplicationListener { *; }

# libktx async coroutine bridge — uses reflection to detect the renderer thread
-keep class ktx.async.** { *; }
-dontwarn ktx.async.**

# Kotlinx Coroutines — debug agent, ServiceLoader-discovered factories
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidDispatcherFactory {
    <init>();
}
-dontwarn kotlinx.coroutines.**

# Jetpack DataStore (proto / preferences)
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# Compose runtime — already covered by AGP defaults, keep as belt-and-braces
-keep class androidx.compose.runtime.** { *; }

# Attributes required for Vitals stack traces and Kotlin reflection
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleTypeAnnotations,AnnotationDefault
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions
-keepattributes *Annotation*

# Keep Kotlin module metadata so kotlin-reflect / serialization can resolve types
-keep class kotlin.Metadata { *; }
-keep class **$$serializer { *; }

# Keep enum methods used by libGDX JSON I/O for tile/terrain enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Project — keep parser data classes (read via reflection by libGDX storage)
-keep class com.homm3.livewallpaper.parser.** { *; }
-keep class com.homm3.livewallpaper.core.WallpaperPreferences { *; }
