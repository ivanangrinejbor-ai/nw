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
import android.os.StatFs
import android.util.Log
import android.os.Parcel
import android.os.Parcelable
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.common.Constants

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BakedApkBuilder {
    private const val TAG = "BakedApkBuilder"
    private const val TEMPLATE_RUNTIME_APK = "template_runtime.apk"

    enum class TemplateType {
        FULL
    }

    data class ApkConfig(
        val appName: String,
        val packageName: String = "org.danvexteam.newcatroidruntime",
        val versionName: String = "1.0",
        val versionCode: Int = 1,
        val permissions: List<String> = emptyList(),
        val minSdk: Int = 21,
        val targetSdk: Int = 35,
        val iconFile: File? = null,
        val payloadPassword: String? = null,
        val customKeystore: File? = null,
        val keystorePass: String = "keystore",
        val keyAlias: String = "newcatroid",
        val keyPass: String = "keystore"
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            appName = parcel.readString() ?: "",
            packageName = parcel.readString() ?: "org.danvexteam.newcatroidruntime",
            versionName = parcel.readString() ?: "1.0",
            versionCode = parcel.readInt(),
            permissions = parcel.createStringArrayList() ?: emptyList(),
            minSdk = parcel.readInt(),
            targetSdk = parcel.readInt(),
            iconFile = parcel.readString()?.let { File(it) },
            payloadPassword = parcel.readString(),
            customKeystore = parcel.readString()?.let { File(it) },
            keystorePass = parcel.readString() ?: "keystore",
            keyAlias = parcel.readString() ?: "newcatroid",
            keyPass = parcel.readString() ?: "keystore"
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(appName)
            parcel.writeString(packageName)
            parcel.writeString(versionName)
            parcel.writeInt(versionCode)
            parcel.writeStringList(permissions)
            parcel.writeInt(minSdk)
            parcel.writeInt(targetSdk)
            parcel.writeString(iconFile?.absolutePath)
            parcel.writeString(payloadPassword)
            parcel.writeString(customKeystore?.absolutePath)
            parcel.writeString(keystorePass)
            parcel.writeString(keyAlias)
            parcel.writeString(keyPass)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<ApkConfig> {
            override fun createFromParcel(parcel: Parcel): ApkConfig = ApkConfig(parcel)
            override fun newArray(size: Int): Array<ApkConfig?> = arrayOfNulls(size)
        }
    }

    sealed class BuildResult {
        data class Success(val apkFile: File) : BuildResult()
        data class Error(val message: String) : BuildResult()
    }

    suspend fun build(context: Context, projectDir: File, config: ApkConfig, onProgress: (String) -> Unit): BuildResult = withContext(Dispatchers.IO) {
        var tempDir: File? = null
        try {
            onProgress("Preparing project files...")
            tempDir = File(context.cacheDir, "apk_build_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val needed = projectDir.sizeRecursively() * 3 + 200L * 1024 * 1024
            runCatching {
                val stat = StatFs(tempDir.absolutePath)
                val usable = stat.blockSizeLong * stat.availableBlocksLong
                if (usable < needed) {
                    tempDir.deleteRecursively()
                    return@withContext BuildResult.Error(
                        "Недостаточно места во временном хранилище (нужно ~${needed / (1024 * 1024)} МБ)."
                    )
                }
            }

            onProgress("Loading template APK...")
            val templateApk = File(tempDir, "template_temp.apk")

            var templateLoaded = false
            val templateAssetName = TEMPLATE_RUNTIME_APK
            try {
                context.assets.open(templateAssetName).use { input ->
                    FileOutputStream(templateApk).use { output -> input.copyTo(output) }
                }
                templateLoaded = true
                Log.d(TAG, "DIAG: template APK extracted = ${templateApk.length() / (1024*1024)} MB")
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

            onProgress("Protecting project payload...")
            val currentProject = ProjectManager.getInstance()?.currentProject
                ?: try {
                    XstreamSerializer.getInstance().loadProject(projectDir, context)
                } catch (e: Exception) {
                    tempDir.deleteRecursively()
                    return@withContext BuildResult.Error("Не удалось загрузить проект для сборки: ${e.message}")
                }
            if (currentProject == null || currentProject.sceneList.isEmpty()) {
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("Проект пустой: нет ни одной сцены для сборки.")
            }

            Log.d(TAG, "=== DIAG: Project directory size breakdown ===")
            Log.d(TAG, "DIAG: projectDir = ${projectDir.absolutePath}")
            Log.d(TAG, "DIAG: projectDir total size = ${projectDir.sizeRecursively() / (1024*1024)} MB")
            val projFilesDir = File(projectDir, "files")
            if (projFilesDir.exists()) Log.d(TAG, "DIAG: projectDir/files/ size = ${projFilesDir.sizeRecursively() / (1024*1024)} MB")
            currentProject.sceneList.forEach { scene ->
                val sceneImageDir = File(scene.directory, "images")
                val sceneSoundDir = File(scene.directory, "sounds")
                Log.d(TAG, "DIAG: scene '${scene.name}' image dir = ${sceneImageDir.absolutePath} exists=${sceneImageDir.exists()} size=${sceneImageDir.sizeRecursively() / (1024*1024)} MB")
                Log.d(TAG, "DIAG: scene '${scene.name}' sound dir = ${sceneSoundDir.absolutePath} exists=${sceneSoundDir.exists()} size=${sceneSoundDir.sizeRecursively() / (1024*1024)} MB")
                Log.d(TAG, "DIAG: scene '${scene.name}' sprites=${scene.spriteList.size}")
            }
            var totalLooks = 0
            var totalSounds = 0
            var looksWithFile = 0
            var soundsWithFile = 0
            var looksFileMissing = 0
            var soundsFileMissing = 0
            currentProject.sceneList.forEach { scene ->
                scene.spriteList.forEach { sprite ->
                    sprite.lookList.forEach { look ->
                        totalLooks++
                        if (look.file != null && look.file!!.exists()) looksWithFile++
                        else looksFileMissing++
                    }
                    sprite.soundList.forEach { sound ->
                        totalSounds++
                        if (sound.file != null && sound.file!!.exists()) soundsWithFile++
                        else soundsFileMissing++
                    }
                }
            }
            Log.d(TAG, "DIAG: looks total=$totalLooks withFile=$looksWithFile missing=$looksFileMissing")
            Log.d(TAG, "DIAG: sounds total=$totalSounds withFile=$soundsWithFile missing=$soundsFileMissing")

            val encryptedProject = File(tempDir, ProtectedProjectPayload.ENCRYPTED_ASSET_NAME)
            val payloadPassword = config.payloadPassword ?: generateRandomPassword()
            createProtectedProjectPayload(
                context, projectDir, encryptedProject, payloadPassword, currentProject, tempDir
            )
            Log.d(TAG, "DIAG: encrypted payload size = ${encryptedProject.length() / (1024*1024)} MB")

            val keyFile = File(tempDir, ProtectedProjectPayload.KEY_ASSET_NAME)
            keyFile.writeText(payloadPassword)

            val keystoreFile = config.customKeystore ?: getOrCreateDebugKeystore(context, tempDir)
            val sigFile: File? = try {
                val certHash = PayloadIntegrity.certHashFromKeystore(
                    keystoreFile, config.keyPass, config.keyAlias
                )
                if (certHash != null) {
                    val f = File(tempDir, ProtectedProjectPayload.SIG_ASSET_NAME)
                    f.writeText(PayloadIntegrity.buildSigContent(encryptedProject.readBytes(), certHash))
                    Log.d(TAG, "Integrity signature written (bound to signing certificate)")
                    f
                } else {
                    Log.w(TAG, "Signing certificate unavailable — building without integrity seal")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to build integrity signature", e)
                null
            }

            onProgress("Configuring application...")
            val manifestConfig = ApkToolboxManager.ManifestConfig(
                appName = config.appName,
                packageName = config.packageName,
                versionName = config.versionName,
                versionCode = config.versionCode,
                minSdkVersion = config.minSdk,
                targetSdkVersion = config.targetSdk,
                permissionsToAdd = config.permissions,
                debuggable = false
            )
            val pathsToDelete = listOf(
                "assets/project",
                "assets/project.zip",
                "META-INF/"
            )
            val filesToAdd = buildList {
                add(encryptedProject to "assets/${ProtectedProjectPayload.ENCRYPTED_ASSET_NAME}")
                add(keyFile to "assets/${ProtectedProjectPayload.KEY_ASSET_NAME}")
                if (sigFile != null) {
                    add(sigFile to "assets/${ProtectedProjectPayload.SIG_ASSET_NAME}")
                }
            }
            if (!ApkToolboxManager.configureApk(
                    templateApk.absolutePath,
                    manifestConfig,
                    iconFile = config.iconFile,
                    pathsToDelete = pathsToDelete,
                    filesToAdd = filesToAdd,
                    workDir = tempDir
                )) {
                Log.e(TAG, "Combined APK configuration failed")
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("Не удалось настроить APK (манифест/иконка/payload).")
            }
            Log.d(TAG, "DIAG: configured APK = ${templateApk.length() / (1024*1024)} MB")
            val unsignedApk = templateApk
            Log.d(TAG, "DIAG: unsigned APK (template + payload embedded) = ${unsignedApk.length() / (1024*1024)} MB")

            onProgress("Signing APK...")
            val rawName = config.appName.replace(" ", "_")
                .replace(Regex("""[\\/:*?"<>|]"""), "_").trim('_', '.')
            val safeName = if (rawName.isBlank()) "app" else rawName
            val signedApk = File(tempDir, "$safeName.apk")

            if (!ApkToolboxManager.signApk(
                context,
                unsignedApk.absolutePath, signedApk.absolutePath,
                keystoreFile.absolutePath, config.keyAlias, config.keyPass
            )) {
                tempDir.deleteRecursively()
                return@withContext BuildResult.Error("APK signing failed")
            }
            Log.d(TAG, "DIAG: signed APK = ${signedApk.length() / (1024*1024)} MB")

            onProgress("Cleaning up...")
            templateApk.delete()
            encryptedProject.delete()

            val resultFile = File(context.cacheDir, signedApk.name)
            if (resultFile.exists()) resultFile.delete()
            signedApk.copyTo(resultFile, true)
            tempDir.deleteRecursively()

            BuildResult.Success(resultFile)
        } catch (e: Throwable) {
            Log.e(TAG, "Build failed", e)
            tempDir?.deleteRecursively()
            val message = if (e is OutOfMemoryError) {
                "Not enough memory to build this project. Close other apps and try again."
            } else {
                e.message ?: "Unknown error"
            }
            BuildResult.Error(message)
        }
    }

    private fun File.sizeRecursively(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    private fun createProtectedProjectPayload(
        context: Context,
        projectDir: File,
        encryptedProject: File,
        password: String,
        currentProject: Project,
        tempDir: File
    ) {
        val stagingDir = File(tempDir, "project_payload")
        val payloadZip = File(tempDir, "project_payload.zip")
        stagingDir.deleteRecursively()
        payloadZip.delete()
        stagingDir.mkdirs()

        val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n"
        val projectXml = xmlHeader + XstreamSerializer.getInstance().getXmlAsStringFromProject(currentProject)
        File(stagingDir, Constants.CODE_XML_FILE_NAME).writeText(projectXml)
        Log.d(TAG, "DIAG: code.xml saved to staging dir (${projectXml.length / 1024} KB)")

        val sourceFilesDir = File(projectDir, "files")
        if (sourceFilesDir.exists()) {
            sourceFilesDir.copyRecursively(File(stagingDir, "files"), overwrite = true)
            val filesSize = File(stagingDir, "files").sizeRecursively()
            Log.d(TAG, "DIAG: files/ copied: ${filesSize/(1024*1024)} MB")
        } else {
            File(stagingDir, "files").mkdirs()
            Log.d(TAG, "DIAG: files/ dir NOT FOUND at ${sourceFilesDir.absolutePath}")
        }

        val imagesDir = File(stagingDir, "images").apply { mkdirs() }
        val soundsDir = File(stagingDir, "sounds").apply { mkdirs() }
        var looksCopied = 0
        var soundsCopied = 0
        var looksSize = 0L
        var soundsSize = 0L
        val allScenes = ArrayList(currentProject.sceneList)
        if (currentProject.hasGlobalScene()) {
            allScenes.add(currentProject.globalScene)
        }
        allScenes.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                sprite.lookList.forEach { look ->
                    look.file?.takeIf { it.exists() }?.let { file ->
                        val target = File(imagesDir, file.name)
                        file.copyTo(target, overwrite = true)
                        looksCopied++
                        looksSize += target.length()
                    }
                }
                sprite.soundList.forEach { sound ->
                    sound.file?.takeIf { it.exists() }?.let { file ->
                        val target = File(soundsDir, file.name)
                        file.copyTo(target, overwrite = true)
                        soundsCopied++
                        soundsSize += target.length()
                    }
                }
            }
        }
        Log.d(TAG, "DIAG: looks copied: $looksCopied files, $looksSize bytes (${looksSize/(1024*1024)} MB)")
        Log.d(TAG, "DIAG: sounds copied: $soundsCopied files, $soundsSize bytes (${soundsSize/(1024*1024)} MB)")

        zipDirectory(stagingDir, payloadZip)
        Log.d(TAG, "DIAG: payload zip = ${payloadZip.length()/(1024*1024)} MB (before encryption)")
        ProjectCrypto.encrypt(payloadZip, encryptedProject, password, locked = true)
        Log.d(TAG, "DIAG: encrypted payload = ${encryptedProject.length()/(1024*1024)} MB")
        payloadZip.delete()
        stagingDir.deleteRecursively()
    }

    private fun generateRandomPassword(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
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
        val keystore = File(debugDir, "debug_fixed.jks")
        if (!keystore.exists()) {
            try {
                context.assets.open("debug_build.keystore").use { input ->
                    FileOutputStream(keystore).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Bundled keystore missing, generating a random one", e)
                ApkToolboxManager.generateKeyStore(
                    keystore.absolutePath, "newcatroid",
                    "keystore", "CN=NewCatroid Auto,O=NewCatroid,C=US"
                )
            }
        }
        return keystore
    }
}