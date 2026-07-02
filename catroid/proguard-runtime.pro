# ProGuard rules for Runtime APK (baked projects, no editor)
# Keep everything needed for StageActivity + LunoScript

# Keep all NDK native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all action/brick classes used by baked runtime
-keep class org.catrobat.catroid.content.** { *; }
-keep class org.catrobat.catroid.content.actions.** { *; }
-keep class org.catrobat.catroid.content.bricks.** { *; }
-keep class org.catrobat.catroid.stage.** { *; }
-keep class org.catrobat.catroid.ProjectManager { *; }
-keep class org.catrobat.catroid.formulaeditor.** { *; }
-keep class org.catrobat.catroid.common.** { *; }
-keep class org.catrobat.catroid.io.** { *; }
-keep class org.catrobat.catroid.apkbuild.** { *; }
-keep class org.catrobat.catroid.virtualmachine.** { *; }

# Keep XStream for baked project loading
-keep class com.thoughtworks.xstream.** { *; }
-keep interface com.thoughtworks.xstream.** { *; }

# Keep libGDX runtime
-keep class com.badlogic.gdx.** { *; }
-keep interface com.badlogic.gdx.** { *; }

# Keep Kotlin scripting
-keep class org.jetbrains.kotlin.scripting.** { *; }

# Keep LunoScript annotations
-keep @com.danvexteam.lunoscript_annotations.LunoClass class * { *; }

# Apache Commons
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**

# Remove all logs from runtime
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}