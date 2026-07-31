package org.catrobat.catroid.utils.lunoscript.security

import android.util.Log

object LunoSandbox {
    private const val TAG = "LunoSandbox"

    private val ALLOWED_PACKAGES = listOf(
        "org.catrobat.catroid.content.bricks.",
        "org.catrobat.catroid.physics.content.bricks.",
        "org.catrobat.catroid.formulaeditor.",
        "org.catrobat.catroid.common.",
        "org.catrobat.catroid.content.",
        "org.catrobat.catroid.stage.",
        "java.lang.",
        "java.util.",
        "kotlin."
    )

    private val BLOCKED_CLASSES = setOf(
        "java.lang.Runtime",
        "java.lang.ProcessBuilder",
        "java.lang.Process",
        "java.lang.reflect.Method",
        "java.lang.reflect.Field",
        "java.lang.reflect.Constructor",
        "java.lang.reflect.AccessibleObject",
        "java.io.File",
        "java.io.FileInputStream",
        "java.io.FileOutputStream",
        "java.io.FileWriter",
        "java.io.FileReader",
        "java.net.Socket",
        "java.net.ServerSocket",
        "java.net.URL",
        "java.net.HttpURLConnection",
        "java.security.KeyStore",
        "javax.crypto.Cipher",
        "dalvik.system.DexClassLoader",
        "dalvik.system.PathClassLoader",
        "android.os.Process",
        "android.app.ActivityManager",
        "android.content.Context",
        "android.content.Intent",
        "org.mozilla.javascript.ContextFactory",
        "org.luaj.vm2.Globals",
        "org.luaj.vm2.lib.jse.JsePlatform"
    )

    private val BLOCKED_PACKAGES = listOf(
        "java.net.",
        "java.io.",
        "java.security.",
        "javax.crypto.",
        "java.lang.reflect.",
        "dalvik.system.",
        "android.os.",
        "android.app.",
        "android.content.",
        "com.sun.",
        "sun.",
        "jdk."
    )

    fun isClassAllowed(className: String): Boolean {
        if (className in BLOCKED_CLASSES) {
            Log.w(TAG, "BLOCKED: $className is in the blocklist")
            return false
        }

        for (blockedPkg in BLOCKED_PACKAGES) {
            if (className.startsWith(blockedPkg)) {
                Log.w(TAG, "BLOCKED: $className is in blocked package $blockedPkg")
                return false
            }
        }

        for (allowedPkg in ALLOWED_PACKAGES) {
            if (className.startsWith(allowedPkg)) {
                return true
            }
        }

        Log.w(TAG, "DENIED: $className is not in any allowed package")
        return false
    }

    fun safeForName(className: String): Class<*>? {
        if (!isClassAllowed(className)) {
            Log.e(TAG, "SECURITY: LunoScript attempted to load forbidden class: $className")
            return null
        }
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Class not found: $className")
            null
        }
    }
}