package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.apkbuildV3.DynamicKeyManager
import org.catrobat.catroid.apkbuildV3.IntegrityValidator
import org.catrobat.catroid.apkbuildV3.ProjectEncryptorV3
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.ZipArchiver
import java.io.File

class ProjectLoaderV3(private val context: Context) {
    private val tag = "ProjectLoaderV3"
    private val payloadAssetName = "project.ncv3"

    data class FullProjectResult(
        val project: Project,
        val projectDir: File
    )

    fun loadFull(cacheDir: File, onProgress: ((Float) -> Unit)? = null): FullProjectResult? {
        return try {
            onProgress?.invoke(0f)
            val key = resolveKey() ?: return null
            onProgress?.invoke(0.1f)

            val encryptedFile = File(cacheDir, payloadAssetName)
            context.assets.open(payloadAssetName).use { input ->
                encryptedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            onProgress?.invoke(0.2f)

            if (!IntegrityValidator.validate(encryptedFile, key)) {
                Log.e(tag, "Integrity validation failed")
                encryptedFile.delete()
                return null
            }
            onProgress?.invoke(0.3f)

            val decryptedZip = File(cacheDir, "project_decrypted.zip")
            if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, decryptedZip)) {
                Log.e(tag, "Full decryption failed")
                encryptedFile.delete()
                return null
            }
            encryptedFile.delete()
            onProgress?.invoke(0.6f)

            val extractDir = File(cacheDir, "project_extracted").apply {
                deleteRecursively()
                mkdirs()
            }
            ZipArchiver().unzip(decryptedZip, extractDir)
            decryptedZip.delete()
            onProgress?.invoke(0.8f)

            val project = XstreamSerializer.getInstance().loadProject(extractDir, context)
                ?: return null
            onProgress?.invoke(1f)

            Log.i(tag, "Full project loaded: ${project.name} (${project.sceneList.size} scenes)")
            FullProjectResult(project, extractDir)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load full project", e)
            null
        }
    }

    fun loadLight(cacheDir: File): ProjectMetadata? {
        return try {
            val key = resolveKey() ?: return null

            val encryptedFile = File(cacheDir, payloadAssetName)
            context.assets.open(payloadAssetName).use { input ->
                encryptedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!IntegrityValidator.validate(encryptedFile, key)) {
                Log.e(tag, "Integrity validation failed (light load)")
                encryptedFile.delete()
                return null
            }

            val decryptedZip = File(cacheDir, "project_light_decrypted.zip")
            if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, decryptedZip)) {
                encryptedFile.delete()
                return null
            }

            val extractDir = File(cacheDir, "project_light").apply {
                deleteRecursively()
                mkdirs()
            }

            ZipArchiver().unzip(decryptedZip, extractDir)

            val project = XstreamSerializer.getInstance().loadProject(extractDir, context)
                ?: return null

            val metadata = ProjectMetadata(
                project = project,
                projectDir = extractDir,
                encryptedPayloadFile = encryptedFile,
                key = key
            )

            Log.i(tag, "Light project metadata loaded: ${project.name} (${project.sceneList.size} scenes)")

            metadata
        } catch (e: Exception) {
            Log.e(tag, "Failed to load light project", e)
            null
        }
    }

    private fun resolveKey(): ByteArray? {
        val dynamicKey = DynamicKeyResolver.resolveKey(context)
        if (dynamicKey != null) {
            Log.i(tag, "Using dynamic key (${dynamicKey.size} bytes)")
            return dynamicKey
        }

        Log.e(tag, "No dynamic key found")
        return null
    }

    data class ProjectMetadata(
        val project: Project,
        val projectDir: File,
        val encryptedPayloadFile: File,
        val key: ByteArray
    )
}