package org.catrobat.catroid.utils.lunoscript.security

import android.util.Log

/**
 * LunoScript Security Sandbox.
 *
 * Prevents LunoScript from loading arbitrary Java classes via Class.forName().
 * Only classes in ALLOWED_PACKAGES can be loaded. Specific dangerous classes
 * are explicitly blocked even if they fall under an allowed package prefix.
 *
 * This is the primary defence against RCE via LunoScript's Java interop.
 */
object LunoSandbox {
    private const val TAG = "LunoSandbox"

    // Whitelist of allowed packages for Class.forName() in LunoScript
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

    // Explicitly blocked classes even if package matches
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

    // Blocked package prefixes (catch-all for dangerous packages)
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

    /**
     * Check if a class name is allowed to be loaded by LunoScript.
     * Returns true if the class is safe to load.
     */
    fun isClassAllowed(className: String): Boolean {
        // 1) Check explicit blocklist
        if (className in BLOCKED_CLASSES) {
            Log.w(TAG, "BLOCKED: $className is in the blocklist")
            return false
        }

        // 2) Check blocked packages
        for (blockedPkg in BLOCKED_PACKAGES) {
            if (className.startsWith(blockedPkg)) {
                Log.w(TAG, "BLOCKED: $className is in blocked package $blockedPkg")
                return false
            }
        }

        // 3) Check allowed packages
        for (allowedPkg in ALLOWED_PACKAGES) {
            if (className.startsWith(allowedPkg)) {
                return true
            }
        }

        // 4) Default: deny
        Log.w(TAG, "DENIED: $className is not in any allowed package")
        return false
    }

    /**
     * Safe version of Class.forName() that checks the sandbox first.
     * Returns the class if allowed, null otherwise.
     */
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