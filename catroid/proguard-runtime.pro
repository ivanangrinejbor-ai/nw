# ProGuard rules for Runtime APK (baked projects, no editor)
# Keep everything needed for StageActivity + LunoScript
-keep class org.catrobat.catroid.utils.lunoscript.** { *; }
-keep interface org.catrobat.catroid.utils.lunoscript.** { *; }

# Keep all NDK native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all action/brick classes used by baked runtime
-keep class org.catrobat.catroid.CatroidApplication { *; }
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
-keep class org.catrobat.catroid.fast2d.** { *; }
-keep class org.catrobat.catroid.raptor.** { *; }
-keep class com.artemis.** { *; }

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

# Huawei HMS optional classes (missing on non-Huawei devices)
-dontwarn android.telephony.HwTelephonyManager
-dontwarn com.huawei.android.os.BuildEx$VERSION
-dontwarn com.huawei.android.telephony.ServiceStateEx
-dontwarn com.huawei.hianalytics.process.HiAnalyticsConfig$Builder
-dontwarn com.huawei.hianalytics.process.HiAnalyticsConfig
-dontwarn com.huawei.hianalytics.process.HiAnalyticsInstance$Builder
-dontwarn com.huawei.hianalytics.process.HiAnalyticsInstance
-dontwarn com.huawei.hianalytics.process.HiAnalyticsManager
-dontwarn com.huawei.hianalytics.util.HiAnalyticTools
-dontwarn com.huawei.libcore.io.ExternalStorageFile
-dontwarn com.huawei.libcore.io.ExternalStorageFileInputStream
-dontwarn com.huawei.libcore.io.ExternalStorageFileOutputStream
-dontwarn com.huawei.libcore.io.ExternalStorageRandomAccessFile
-dontwarn com.huawei.secure.android.common.util.SafeBase64
-dontwarn com.huawei.secure.android.common.util.SafeString

# JNA (optional desktop-only)
-dontwarn com.sun.jna.Library
-dontwarn com.sun.jna.Memory
-dontwarn com.sun.jna.Native
-dontwarn com.sun.jna.NativeLibrary
-dontwarn com.sun.jna.NativeLong
-dontwarn com.sun.jna.Platform
-dontwarn com.sun.jna.Pointer
-dontwarn com.sun.jna.Structure$ByValue
-dontwarn com.sun.jna.Structure
-dontwarn com.sun.jna.Union
-dontwarn com.sun.jna.ptr.IntByReference
-dontwarn com.sun.jna.win32.StdCallLibrary
-dontwarn com.sun.jna.win32.W32APIOptions

# Java desktop/management classes (not on Android)
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Component
-dontwarn java.awt.Rectangle
-dontwarn java.beans.Introspector
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.MemoryMXBean
-dontwarn java.lang.management.MemoryPoolMXBean
-dontwarn java.lang.management.MemoryType
-dontwarn java.lang.management.MemoryUsage
-dontwarn java.lang.management.ThreadInfo
-dontwarn java.lang.management.ThreadMXBean
-dontwarn javax.management.MBeanServer
-dontwarn javax.management.MalformedObjectNameException
-dontwarn javax.management.NotCompliantMBeanException
-dontwarn javax.management.ObjectName
-dontwarn javax.script.ScriptEngineFactory
-dontwarn javax.swing.Icon
-dontwarn javax.swing.JComponent
-dontwarn javax.swing.SwingUtilities
-dontwarn org.eclipse.jdt.internal.compiler.tool.EclipseCompiler
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid

# Kotlin experimental/errorprone annotations
-dontwarn kotlin.Experimental$Level
-dontwarn kotlin.Experimental
-dontwarn kotlin.annotations.jvm.Mutable
-dontwarn kotlin.annotations.jvm.ReadOnly
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.CompatibleWith
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.DoNotMock
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.ForOverride
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.concurrent.GuardedBy
-dontwarn org.jetbrains.kotlin.com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn org.jetbrains.kotlin.com.google.j2objc.annotations.RetainedWith
-dontwarn org.jetbrains.kotlin.com.google.j2objc.annotations.Weak
-dontwarn org.jetbrains.kotlin.com.intellij.openapi.vfs.LocalFileSystem

# javax.management (not on Android)
-dontwarn javax.management.InstanceAlreadyExistsException
-dontwarn javax.management.InstanceNotFoundException
-dontwarn javax.management.JMException
-dontwarn javax.management.MBeanRegistrationException
-dontwarn javax.management.NotificationEmitter
-dontwarn javax.management.NotificationFilter
-dontwarn javax.management.NotificationListener
-dontwarn javax.management.ObjectInstance

# Don't fail on missing classes (EclipseCompiler is desktop-only, not on Android)
-ignorewarnings

# Remove all logs from runtime
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}