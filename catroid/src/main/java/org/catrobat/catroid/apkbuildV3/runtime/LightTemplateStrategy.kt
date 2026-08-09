package org.catrobat.catroid.apkbuildV3.runtime

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.apkbuildV3.ProjectEncryptorV3
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.ZipArchiver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LightTemplateStrategy(private val context: Context) {
    private val tag = "LightTemplateStrategy"
    private val cache by lazy {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val totalRam = am?.let {
            val memInfo = ActivityManager.MemoryInfo()
            it.getMemoryInfo(memInfo)
            memInfo.totalMem
        } ?: (2L * 1024 * 1024 * 1024)
        MemoryAwareCache.forDevice(totalRam / (1024 * 1024))
    }

    private var metadata: ProjectLoaderV3.ProjectMetadata? = null
    private val loadedScenes = mutableSetOf<String>()

    fun initialize(cacheDir: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        return try {
            onProgress?.invoke(0f)

            val loader = ProjectLoaderV3(context)
            val meta = loader.loadLight(cacheDir) ?: return false
            metadata = meta
            onProgress?.invoke(0.5f)

            val firstScene = meta.project.defaultScene
            if (firstScene != null) {
                loadScene(firstScene.name, cacheDir)
            }

            ProjectManager.getInstance().currentProject = meta.project

            onProgress?.invoke(1f)
            Log.i(tag, "Light template initialized: ${meta.project.name} " +
                    "(preloaded: ${firstScene?.name})")

            true
        } catch (e: Exception) {
            Log.e(tag, "Light template initialization failed", e)
            false
        }
    }

    fun loadScene(sceneName: String, cacheDir: File): Scene? {
        val meta = metadata ?: return null

        cache.get(sceneName)?.let { return it as? Scene }

        val scene = meta.project.getSceneByName(sceneName) ?: return null

        return try {
            val extractDir = File(cacheDir, "scene_${sceneName}")
            extractDir.deleteRecursively()
            extractDir.mkdirs()

            val sceneDirOld = scene.directory
            val sceneExtractDir = File(extractDir, sceneDirOld.name)

            val decryptedZip = File(cacheDir, "project_light_decrypted.zip")
            if (decryptedZip.exists()) {
                ZipArchiver().unzip(decryptedZip, extractDir)
            } else {
                Log.e(tag, "Decrypted zip not found for on-demand scene loading")
                return null
            }

            val loadedSceneDir = File(extractDir, meta.projectDir.name)
            val sceneFiles = loadedSceneDir.listFiles()

            val sceneImagesDir = File(sceneExtractDir, "images")
            val sceneSoundsDir = File(sceneExtractDir, "sounds")

            if (sceneImagesDir.exists()) {
                sceneImagesDir.listFiles()?.forEach { file ->
                    MemoryAwarePipeline.copyFile(file, File(sceneExtractDir, file.name))
                }
            }
            if (sceneSoundsDir.exists()) {
                sceneSoundsDir.listFiles()?.forEach { file ->
                    MemoryAwarePipeline.copyFile(file, File(sceneExtractDir, file.name))
                }
            }

            scene.setProject(meta.project)

            val sceneSize = estimateSceneSize(scene)
            cache.put(sceneName, scene, sceneSize)

            loadedScenes.add(sceneName)
            Log.d(tag, "Scene '$sceneName' loaded on demand (~${sceneSize / (1024 * 1024)} MB)")

            scene
        } catch (e: Exception) {
            Log.e(tag, "Failed to load scene '$sceneName' on demand", e)
            null
        }
    }

    fun preloadInitialScenes(cacheDir: File, count: Int = 3) {
        val meta = metadata ?: return
        val scenes = meta.project.sceneList.take(count)
        for (scene in scenes) {
            if (scene.name !in loadedScenes) {
                loadScene(scene.name, cacheDir)
            }
        }
    }

    fun evictScene(sceneName: String) {
        cache.remove(sceneName)
        loadedScenes.remove(sceneName)
        Log.d(tag, "Scene '$sceneName' evicted from cache")
    }

    fun clearCache() {
        cache.clear()
        loadedScenes.clear()
        Log.d(tag, "Scene cache cleared")
    }

    val memoryUsageBytes: Long get() = cache.approximateMemoryBytes()

    val activeScenes: Set<String> get() = loadedScenes.toSet()

    private fun estimateSceneSize(scene: Scene): Long {
        var total = 0L
        for (sprite in scene.spriteList) {
            total += sprite.lookList.sumOf { look ->
                look.file?.length() ?: 0L
            }
            total += sprite.soundList.sumOf { sound ->
                sound.file?.length() ?: 0L
            }
        }
        return total.coerceAtLeast(1024L * 1024L)
    }
}

private object MemoryAwarePipeline {
    fun copyFile(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.channel.transferTo(0, source.length(), output.channel)
            }
        }
    }
}
