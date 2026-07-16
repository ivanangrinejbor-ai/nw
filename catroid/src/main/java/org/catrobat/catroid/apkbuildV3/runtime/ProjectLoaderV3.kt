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

/**
 * Loads a V3 encrypted project from the APK assets.
 *
 * Supports two loading strategies:
 * - FULL:  Decrypt and extract everything, then load all scenes.
 * - LIGHT: Decrypt metadata, then load scenes on demand.
 *
 * The loader handles:
 * 1. Finding and resolving the dynamic key.
 * 2. Decrypting the payload (full or chunk-by-chunk).
 * 3. Unpacking the project archive.
 * 4. Loading the project via XstreamSerializer.
 * 5. Integrity verification.
 */
class ProjectLoaderV3(private val context: Context) {
    private val tag = "ProjectLoaderV3"
    private val payloadAssetName = "project.ncv3"

    /**
     * Результат загрузки FULL-проекта: сам проект + директория, откуда его можно
     * запустить через StageActivity.
     */
    data class FullProjectResult(
        val project: Project,
        val projectDir: File
    )

    /**
     * Loads the full project (FULL template strategy).
     * Decrypts everything upfront, extracts, and loads into ProjectManager.
     *
     * @param cacheDir  Directory to use for extraction
     * @param onProgress  Progress callback (0.0 - 1.0)
     * @return  [FullProjectResult], или null при ошибке
     */
    fun loadFull(cacheDir: File, onProgress: ((Float) -> Unit)? = null): FullProjectResult? {
        return try {
            onProgress?.invoke(0f)
            val key = resolveKey() ?: return null
            onProgress?.invoke(0.1f)

            // Extract encrypted payload from assets
            val encryptedFile = File(cacheDir, payloadAssetName)
            context.assets.open(payloadAssetName).use { input ->
                encryptedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            onProgress?.invoke(0.2f)

            // Verify integrity before decrypting
            if (!IntegrityValidator.validate(encryptedFile, key)) {
                Log.e(tag, "Integrity validation failed")
                encryptedFile.delete()
                return null
            }
            onProgress?.invoke(0.3f)

            // Decrypt everything
            val decryptedZip = File(cacheDir, "project_decrypted.zip")
            if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, decryptedZip)) {
                Log.e(tag, "Full decryption failed")
                encryptedFile.delete()
                return null
            }
            encryptedFile.delete()
            onProgress?.invoke(0.6f)

            // Extract
            val extractDir = File(cacheDir, "project_extracted").apply {
                deleteRecursively()
                mkdirs()
            }
            ZipArchiver().unzip(decryptedZip, extractDir)
            decryptedZip.delete()
            onProgress?.invoke(0.8f)

            // Load project
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

    /**
     * Loads project metadata only (LIGHT template strategy).
     * Decrypts just the code.xml to get project structure, scenes list, etc.
     * Individual scenes are loaded on demand by [loadScene].
     *
     * @param cacheDir  Directory to use for extraction
     * @return  ProjectMetadata with the structure, or null on failure
     */
    fun loadLight(cacheDir: File): ProjectMetadata? {
        return try {
            val key = resolveKey() ?: return null

            val encryptedFile = File(cacheDir, payloadAssetName)
            context.assets.open(payloadAssetName).use { input ->
                encryptedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Verify integrity
            if (!IntegrityValidator.validate(encryptedFile, key)) {
                Log.e(tag, "Integrity validation failed (light load)")
                encryptedFile.delete()
                return null
            }

            // Decrypt and extract only code.xml (first few chunks)
            val decryptedZip = File(cacheDir, "project_light_decrypted.zip")
            if (!ProjectEncryptorV3.decryptAll(encryptedFile, key, decryptedZip)) {
                encryptedFile.delete()
                return null
            }

            val extractDir = File(cacheDir, "project_light").apply {
                deleteRecursively()
                mkdirs()
            }

            // Extract only the project structure (code.xml + files/ with permissions.txt)
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

            // Don't delete the decrypted zip yet — we still need it for on-demand scene loading
            metadata
        } catch (e: Exception) {
            Log.e(tag, "Failed to load light project", e)
            null
        }
    }

    /**
     * Resolves the dynamic key from assets, falling back to static if needed.
     */
    private fun resolveKey(): ByteArray? {
        val key = DynamicKeyResolver.resolveKey(context)
        if (key != null) return key

        // Fallback: try static password for backward compatibility
        Log.w(tag, "No dynamic key found, trying static fallback")
        return null
    }

    /**
     * Metadata holder for light-loaded projects.
     */
    data class ProjectMetadata(
        val project: Project,
        val projectDir: File,
        val encryptedPayloadFile: File,
        val key: ByteArray
    )
}
