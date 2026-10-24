# libGDX Skin resolves these classes by name from uiskin.json and fills their fields reflectively
-keep class com.badlogic.gdx.scenes.scene2d.ui.**$*Style { <init>(); <fields>; }
-keep class com.badlogic.gdx.scenes.scene2d.ui.Skin$TintedDrawable { <init>(); <fields>; }
-keepnames class com.badlogic.gdx.graphics.g2d.BitmapFont
-keepnames class com.badlogic.gdx.graphics.Color
-dontwarn com.badlogic.gdx.**

# Readable Vitals stack traces via the uploaded mapping file
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
