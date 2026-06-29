/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.apkbuild

import android.content.Context
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.ZipArchiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local APK builder for Catroid visual projects.
 * 
 * Embedds project files directly (NOT as project.zip) into the template APK.
 * Uses ApkToolboxManager for signing and manifest editing.
 * 
 * Template APK is loaded from assets/template.apk.
 * Debug keystore is auto-generated on first run.
 */
object BakedApkBuilder {
    private const val TAG = "BakedApkBuilder"
    private const val TEMPLATE_APK = "template.apk"
    private const val PROJECT_ASSETS_PREFIX = "assets/project/"

    data class ApkConfig(
        val appName: String,
        val packageName: String = "org.DanVexTeam.NewCatroid",
        val versionName: String = "1.0",
        val versionCode: Int = 1,
        val iconFile: File? = null,
        val customKeystore: File? = null,
        val keystorePass: String = "keystore",
        val keyAlias: String = "newcatroid",
        val keyPass: String = "keystore"
    )

    sealed class BuildResult {
        data class Success(val apkFile: File) : BuildResult()
        data class Error(val message: String) : BuildResult()
    }

    suspend fun build(context: Context, projectDir: File, config: ApkConfig, onProgress: (String) -> Unit): BuildResult = withContext(Dispatchers.IO) {
        try {
            onProgress("Preparing project files...")
            val tempDir = File(context.cacheDir, "apk_build_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            // Step 1: Copy template APK from assets
            onProgress("Loading template APK...")
            val templateApk = File(tempDir, "template_temp.apk")
            try {
                context.assets.open(TEMPLATE_APK).use { input ->
                    FileOutputStream(templateApk).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Template APK not found in assets", e)
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("Template APK missing. Reinstall the app.")
            }

            // Step 2: Build project assets directory
            onProgress("Building project assets...")
            val projectAssets = File(tempDir, "project_assets")
            projectAssets.mkdirs()
            copyProjectFiles(projectDir, projectAssets)

            // Step 3: Modify AndroidManifest in the template APK before extraction
            onProgress("Configuring application...")
            val manifestConfig = ApkToolboxManager.ManifestConfig(
                appName = config.appName,
                packageName = config.packageName,
                versionName = config.versionName,
                versionCode = config.versionCode
            )
            if (!ApkToolboxManager.updateManifest(templateApk.absolutePath, manifestConfig)) {
                Log.e(TAG, "Manifest update failed")
            }

            // Step 4: Extract template APK to temp dir
            onProgress("Extracting template...")
            val extractedDir = File(tempDir, "extracted")
            extractedDir.mkdirs()
            extractApk(templateApk, extractedDir)

            // Step 5: Inject project files into extracted APK
            onProgress("Embedding project files...")
            val projectDirInApk = File(extractedDir, PROJECT_ASSETS_PREFIX)
            projectDirInApk.mkdirs()
            projectAssets.copyRecursively(projectDirInApk, true)

            // Step 6: Replace icon if provided
            if (config.iconFile != null && config.iconFile.exists()) {
                onProgress("Replacing icon...")
                replaceIcon(extractedDir, config.iconFile)
            }

            // Step 7: Remove old signatures
            onProgress("Removing old signatures...")
            val metaInf = File(extractedDir, "META-INF")
            if (metaInf.exists()) metaInf.deleteRecursively()

            // Step 8: Repackage as unsigned APK
            onProgress("Packaging APK...")
            val unsignedApk = File(tempDir, "unsigned.apk")
            zipDirectory(extractedDir, unsignedApk)

            // Step 9: Sign APK
            onProgress("Signing APK...")
            val signedApk = File(tempDir, "${config.appName.replace(" ", "_")}.apk")
            val keystoreFile = config.customKeystore ?: getOrCreateDebugKeystore(context, tempDir)

            if (!ApkToolboxManager.signApk(
                context,
                unsignedApk.absolutePath, signedApk.absolutePath,
                keystoreFile.absolutePath, config.keyAlias, config.keyPass
            )) {
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("APK signing failed")
            }

            // Step 10: Cleanup
            onProgress("Cleaning up...")
            templateApk.delete()
            extractedDir.deleteRecursively()
            projectAssets.deleteRecursively()
            unsignedApk.delete()

            val resultFile = File(context.cacheDir, signedApk.name)
            if (resultFile.exists()) resultFile.delete()
            signedApk.copyTo(resultFile, true)
            tempDir.deleteRecursively()

            BuildResult.Success(resultFile)
        } catch (e: Exception) {
            Log.e(TAG, "Build failed", e)
            BuildResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun copyProjectFiles(projectDir: File, destAssets: File) {
        val filesDir = File(destAssets, "files")
        filesDir.mkdirs()
        projectDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name != "tmp") {
                val dest = File(filesDir, file.name)
                file.copyRecursively(dest, true)
            }
        }
        // Copy code.xml directly
        val codeXml = File(projectDir, "code.xml")
        if (codeXml.exists()) {
            codeXml.copyTo(File(destAssets, "code.xml"), true)
        }
    }

    private fun extractApk(apkFile: File, destDir: File) {
        ZipInputStream(FileInputStream(apkFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.contains("..")) { entry = zis.nextEntry; continue }
                val targetFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun zipDirectory(sourceDir: File, destFile: File) {
        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val relativePath = file.relativeTo(sourceDir).path.replace('\\', '/')
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$relativePath/"))
                } else {
                    zos.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).use { it.copyTo(zos) }
                }
                zos.closeEntry()
            }
        }
    }

    private fun replaceIcon(extractedDir: File, iconFile: File) {
        // Find and replace mipmap icons
        extractedDir.walkTopDown().filter { it.name.startsWith("ic_launcher") }
            .forEach { existing -> existing.delete(); iconFile.copyTo(existing, true) }

        val resDir = File(extractedDir, "res")
        if (resDir.exists()) {
            resDir.walkTopDown().filter { it.isDirectory && it.name.startsWith("mipmap") }
                .forEach { dir ->
                    dir.listFiles()?.filter { it.name.startsWith("ic_launcher") }
                        ?.forEach { it.delete(); iconFile.copyTo(File(dir, it.name), true) }
                }
        }
    }

    private fun getOrCreateDebugKeystore(context: Context, tempDir: File): File {
        val debugDir = File(context.filesDir, "apk_signing")
        debugDir.mkdirs()
        val keystore = File(debugDir, "debug_auto.jks")
        if (!keystore.exists()) {
            ApkToolboxManager.generateKeyStore(
                keystore.absolutePath, "newcatroid",
                "keystore", "CN=NewCatroid Auto,O=NewCatroid,C=US"
            )
        }
        return keystore
    }
}
