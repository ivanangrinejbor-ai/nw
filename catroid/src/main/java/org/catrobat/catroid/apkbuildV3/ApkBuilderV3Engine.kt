package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.os.StatFs
import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.XstreamSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApkBuilderV3Engine {
    private const val TAG = "ApkBuilderV3Engine"
    private const val TEMPLATE_RUNTIME_APK = "template_runtime.apk"
    private const val ASSET_PROJECT_PAYLOAD = "project.ncv3"
    private const val BUILD_HEADROOM_MB = 256L

    suspend fun build(
        context: Context,
        projectDir: File,
        config: ApkBuilderV3Config,
        listener: BuildProgressListener
    ): AssemblyResult = withContext(Dispatchers.IO) {
        var tempDir: File? = null
        try {
            listener.onProgress(0f, "Initializing build environment...", "")

            tempDir = File(context.cacheDir, "apk_v3_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val projectSize = projectDir.sizeRecursively()
            val neededSpace = projectSize * 3 + BUILD_HEADROOM_MB * 1024 * 1024
            checkDiskSpace(tempDir, neededSpace)

            listener.onProgress(5f, "Loading and serializing project...", "")

            val project = ProjectManager.getInstance().currentProject
                ?: XstreamSerializer.getInstance().loadProject(projectDir, context)

            if (project == null || project.sceneList.isEmpty()) {
                return@withContext AssemblyResult.Failure("Project is empty — no scenes to build.")
            }

            listener.onProgress(10f, "Packaging project resources...", Constants.CODE_XML_FILE_NAME)

            val stagingDir = File(tempDir, "staging")
            val payloadZip = File(tempDir, "payload_raw.zip")
            stageProjectPayload(project, projectDir, stagingDir, payloadZip, context) { file ->
                listener.onProgress(10f, "Packaging project resources...", file)
            }

            listener.onProgress(30f, "Generating encryption keys...", "")

            val keyResult = DynamicKeyManager.generateKey(project.name)
            if (!DynamicKeyManager.verifyKeyIntegrity(keyResult.keyFileContents)) {
                return@withContext AssemblyResult.Failure("Key generation integrity check failed.")
            }

            listener.onProgress(35f, "Encrypting project payload (V3 format)...", payloadZip.name)

            val encryptedPayload = File(tempDir, ASSET_PROJECT_PAYLOAD)
            ProjectEncryptorV3.encrypt(
                sourceFile = payloadZip,
                destFile = encryptedPayload,
                key = keyResult.selectedKey,
                onProgress = { p ->
                    listener.onProgress(35f + p * 15f, "Encrypting project payload...", payloadZip.name)
                }
            )

            listener.onProgress(50f, "Verifying payload integrity...", encryptedPayload.name)

            if (!IntegrityValidator.validate(encryptedPayload, keyResult.selectedKey)) {
                return@withContext AssemblyResult.Failure("Payload integrity check failed. " +
                        "The encrypted project may be corrupted.")
            }

            payloadZip.delete()
            stagingDir.deleteRecursively()

            listener.onProgress(55f, "Preparing runtime template...", TEMPLATE_RUNTIME_APK)

            val assembledApk = V3ApkAssembler.assemble(
                context = context,
                config = config,
                encryptedPayload = encryptedPayload,
                keyFileNames = keyResult.keyFileNames,
                keyFileContents = keyResult.keyFileContents,
                workDir = tempDir,
                firebaseConfig = config.firebaseConfig
            ) { p, file ->
                listener.onProgress(55f + p * 43f, "Assembling APK...", file)
            }

            listener.onProgress(98f, "Finalizing...", assembledApk.name)

            val safeName = config.appName
                .replace(" ", "_")
                .replace(Regex("""[\\/:*?"<>|]"""), "_")
                .trim('_', '.')
                .ifBlank { "app" }

            val resultFile = File(context.cacheDir, "${safeName}.apk")
            if (resultFile.exists()) resultFile.delete()
            listener.onProgress(99f, "Finalizing...", resultFile.name)
            assembledApk.copyTo(resultFile, overwrite = true)

            tempDir.deleteRecursively()

            listener.onProgress(100f, "Done!", resultFile.name)

            Log.i(TAG, "Build complete: ${resultFile.absolutePath} " +
                    "(${resultFile.length() / (1024 * 1024)} MB)")

            AssemblyResult.Success(
                apkFile = resultFile,
                keyFileNames = keyResult.keyFileNames,
                templateType = config.templateType,
                totalSizeBytes = resultFile.length()
            )

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during build", e)
            tempDir?.deleteRecursively()
            AssemblyResult.Failure(
                "Not enough memory to build this project. " +
                        "Close other applications and try again, or use Light Template."
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Build failed", e)
            tempDir?.deleteRecursively()
            AssemblyResult.Failure(
                e.message ?: "Unknown build error: ${e.javaClass.simpleName}",
                e
            )
        }
    }

    private fun stageProjectPayload(
        project: Project,
        projectDir: File,
        stagingDir: File,
        payloadZip: File,
        context: Context,
        onFile: (String) -> Unit
    ) {
        stagingDir.mkdirs()
        Log.d(TAG, "stageProjectPayload: start projectDir=${projectDir.absolutePath}")

        val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n"
        val projectXml = xmlHeader + XstreamSerializer.getInstance().getXmlAsStringFromProject(project)
        File(stagingDir, Constants.CODE_XML_FILE_NAME).writeText(projectXml)
        onFile(Constants.CODE_XML_FILE_NAME)

        copyDir(File(projectDir, "files"), File(stagingDir, "files"), onFile)

        for (scene in project.sceneList) {
            val sceneDirName = scene.getDirectory().name
            copyDir(File(projectDir, "$sceneDirName/images"), File(stagingDir, "$sceneDirName/images"), onFile)
            copyDir(File(projectDir, "$sceneDirName/sounds"), File(stagingDir, "$sceneDirName/sounds"), onFile)
        }
        Log.d(TAG, "stageProjectPayload: staged ${project.sceneList.size} scenes")

        MemoryAwarePipeline.zipDirectoryStreaming(stagingDir, payloadZip, onFile = onFile)

        Log.d(TAG, "Project staged: ${payloadZip.length() / (1024 * 1024)} MB")
    }

    private fun copyDir(src: File, dst: File, onFile: (String) -> Unit) {
        if (!src.exists() || !src.isDirectory) return
        dst.mkdirs()
        src.listFiles()?.forEach { file ->
            onFile("${src.name}/${file.name}")
            MemoryAwarePipeline.copyFile(file, File(dst, file.name))
        }
    }

    private fun checkDiskSpace(dir: File, neededBytes: Long) {
        runCatching {
            val stat = StatFs(dir.absolutePath)
            val usable = stat.blockSizeLong * stat.availableBlocksLong
            if (usable < neededBytes) {
                throw IllegalStateException(
                    "Insufficient disk space. Need ~${neededBytes / (1024 * 1024)} MB, " +
                            "available: ${usable / (1024 * 1024)} MB."
                )
            }
        }
    }

    private fun File.sizeRecursively(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
