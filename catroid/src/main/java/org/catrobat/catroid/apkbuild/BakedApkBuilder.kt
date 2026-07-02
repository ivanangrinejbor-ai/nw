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
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Local APK builder for Catroid visual projects.
 * Builds minimal runtime APK (без редактора) with encrypted project payload.
 * Template APK is loaded from assets/template_runtime.apk.
 * Debug keystore is auto-generated on first run.
 */
object BakedApkBuilder {
    private const val TAG = "BakedApkBuilder"
    private const val TEMPLATE_RUNTIME_APK = "template_runtime.apk"
    private const val TEMPLATE_RUNTIME_LITE_APK = "template_runtime_lite.apk"
    private const val TEMPLATE_RUNTIME_NOARM_APK = "template_runtime_noarm.apk"

    enum class TemplateType {
        FULL, LITE, NO_ARM
    }

    data class ApkConfig(
        val appName: String,
        val packageName: String = "org.DanVexTeam.NewCatroidRuntime",
        val versionName: String = "1.0",
        val versionCode: Int = 1,
        val iconFile: File? = null,
        val customKeystore: File? = null,
        val keystorePass: String = "keystore",
        val keyAlias: String = "newcatroid",
        val keyPass: String = "keystore",
        val templateType: TemplateType = TemplateType.FULL
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

            // Step 1: Load template APK
            onProgress("Loading template APK...")
            val templateApk = File(tempDir, "template_temp.apk")

            var templateLoaded = false
            val templateAssetName = when (config.templateType) {
                TemplateType.LITE -> TEMPLATE_RUNTIME_LITE_APK
                TemplateType.NO_ARM -> TEMPLATE_RUNTIME_NOARM_APK
                else -> TEMPLATE_RUNTIME_APK
            }
            try {
                context.assets.open(templateAssetName).use { input ->
                    FileOutputStream(templateApk).use { output -> input.copyTo(output) }
                }
                templateLoaded = true
                Log.d(TAG, "Loaded template APK from assets: $templateAssetName")
            } catch (e: Exception) {
                Log.w(TAG, "Template APK not found in assets: $templateAssetName")
            }

            if (!templateLoaded) {
                val selfApkPath = context.applicationInfo.sourceDir
                if (selfApkPath != null && File(selfApkPath).exists()) {
                    File(selfApkPath).copyTo(templateApk, overwrite = true)
                    Log.d(TAG, "Using self-APK as template: $selfApkPath")
                } else {
                    tempDir.deleteRecursively()
                    return@withContext BuildResult.Error("Template APK missing and self-APK unavailable.")
                }
            }

            // Step 2: Build encrypted baked project payload
            onProgress("Protecting project payload...")
            val encryptedProject = File(tempDir, ProtectedProjectPayload.ENCRYPTED_ASSET_NAME)
            createProtectedProjectPayload(context, projectDir, encryptedProject)

            // Step 3: Modify AndroidManifest in the template APK
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

            // Step 4: Inject encrypted project payload into template APK
            onProgress("Embedding protected project...")
            ApkToolboxManager.deleteFromApk(templateApk.absolutePath, "assets/project")
            ApkToolboxManager.deleteFromApk(templateApk.absolutePath, "assets/project.zip")
            ApkToolboxManager.addFileToApk(templateApk.absolutePath, encryptedProject, "assets/${ProtectedProjectPayload.ENCRYPTED_ASSET_NAME}")

            // Step 5: Replace icon if provided
            if (config.iconFile != null && config.iconFile.exists()) {
                onProgress("Replacing icon...")
                ApkToolboxManager.replaceIconInApk(templateApk.absolutePath, config.iconFile)
            }

            // Step 6: Remove old signatures
            onProgress("Removing old signatures...")
            ApkToolboxManager.deleteFromApk(templateApk.absolutePath, "META-INF")

            // Step 7: Sign APK
            onProgress("Signing APK...")
            val signedApk = File(tempDir, "${config.appName.replace(" ", "_")}.apk")
            val keystoreFile = config.customKeystore ?: getOrCreateDebugKeystore(context, tempDir)

            if (!ApkToolboxManager.signApk(
                context,
                templateApk.absolutePath, signedApk.absolutePath,
                keystoreFile.absolutePath, config.keyAlias, config.keyPass
            )) {
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("APK signing failed")
            }

            // Step 8: Cleanup
            onProgress("Cleaning up...")
            templateApk.delete()
            encryptedProject.delete()

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

    private fun createProtectedProjectPayload(context: Context, projectDir: File, encryptedProject: File) {
        val currentProject = ProjectManager.getInstance().currentProject
            ?: error("No current project available for protected APK build.")

        val bakedDir = File(encryptedProject.parentFile, "baked_project")
        val bakedZip = File(encryptedProject.parentFile, "baked_project.zip")
        bakedDir.deleteRecursively()
        bakedZip.delete()
        bakedDir.mkdirs()

        val lunoCode = ProjectBaker(context).bake(currentProject)
        File(bakedDir, "init.bin").writeText(lunoCode)

        val sourceFilesDir = File(projectDir, "files")
        if (sourceFilesDir.exists()) {
            sourceFilesDir.copyRecursively(File(bakedDir, "files"), overwrite = true)
        } else {
            File(bakedDir, "files").mkdirs()
        }

        val imagesDir = File(bakedDir, "images").apply { mkdirs() }
        val soundsDir = File(bakedDir, "sounds").apply { mkdirs() }
        currentProject.sceneList.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                sprite.lookList.forEach { look ->
                    look.file?.takeIf { it.exists() }?.let { file ->
                        file.copyTo(File(imagesDir, file.name), overwrite = true)
                    }
                }
                sprite.soundList.forEach { sound ->
                    sound.file?.takeIf { it.exists() }?.let { file ->
                        file.copyTo(File(soundsDir, file.name), overwrite = true)
                    }
                }
            }
        }

        zipDirectory(bakedDir, bakedZip)
        ProjectCrypto.encrypt(bakedZip, encryptedProject, ProtectedProjectPayload.PASSWORD)
        bakedZip.delete()
        bakedDir.deleteRecursively()
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