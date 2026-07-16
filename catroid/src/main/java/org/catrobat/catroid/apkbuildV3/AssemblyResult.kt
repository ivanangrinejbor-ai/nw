package org.catrobat.catroid.apkbuildV3

import java.io.File

/**
 * Result of an APK V3 assembly operation.
 */
sealed class AssemblyResult {
    data class Success(
        val apkFile: File,
        val keyFileName: String,
        val templateType: TemplateType,
        val totalSizeBytes: Long
    ) : AssemblyResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : AssemblyResult()
}

/**
 * Template type for the baked runtime APK.
 */
enum class TemplateType {
    /** Preloads the entire project into RAM on startup — fastest scene transitions. */
    FULL,

    /** Lazy-loads scenes on demand with LRU eviction — lower memory footprint. */
    LIGHT
}
