package org.catrobat.catroid.apkbuildV3

import java.io.File

sealed class AssemblyResult {
    data class Success(
        val apkFile: File,
        val keyFileNames: List<String>,
        val templateType: TemplateType,
        val totalSizeBytes: Long
    ) : AssemblyResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : AssemblyResult()
}

enum class TemplateType {
    FULL,
    LIGHT
}
