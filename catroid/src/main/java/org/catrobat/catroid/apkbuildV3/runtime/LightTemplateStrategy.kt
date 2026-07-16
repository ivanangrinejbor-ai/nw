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

/**
 * Light Template loading strategy.
 *
 * On startup:
 * 1. Decrypt only the project metadata (code.xml).
 * 2. Extract project structure (~30% of total data).
 * 3. Load remaining scenes on demand.
 * 4. Evict least-recently-used scenes when memory is low.
 *
 * Scene transitions may trigger loading (with a brief wait).
 * Re-visiting a previously-evicted scene re-loads it from the encrypted payload.
 *
 * Memory usage: LOW-MEDIUM (only active scenes in RAM).
 * Suitable for: low-end devices or very large projects.
 */
class LightTemplateStrategy(private val context: Context) {
    private val tag = "LightTemplateStrategy"
    private val cache by lazy {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val totalRam = am?.let {
            val memInfo = ActivityManager.MemoryInfo()
            it.getMemoryInfo(memInfo)
            memInfo.totalMem
        } ?: (2L * 1024 * 1024 * 1024) // default to 2 GB if unavailable
        MemoryAwareCache.forDevice(totalRam / (1024 * 1024))
    }

    private var metadata: ProjectLoaderV3.ProjectMetadata? = null
    private val loadedScenes = mutableSetOf<String>()

    /**
     * Initial load: decrypt metadata, extract structure, load ~30% of the project.
     *
     * @param cacheDir  Working directory
     * @param onProgress  Progress callback (0.0 - 1.0)
     * @return  true if initialization succeeded
     */
    fun initialize(cacheDir: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        return try {
            onProgress?.invoke(0f)

            val loader = ProjectLoaderV3(context)
            val meta = loader.loadLight(cacheDir) ?: return false
            metadata = meta
            onProgress?.invoke(0.5f)

            // Load the first scene (default start scene) immediately
            val firstScene = meta.project.defaultScene
            if (firstScene != null) {
                loadScene(firstScene.name, cacheDir)
            }

            // Register with ProjectManager
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

    /**
     * Loads a specific scene on demand.
     * If the scene is already cached, returns it immediately.
     * Otherwise decrypts and loads from the encrypted payload.
     *
     * @param sceneName  Name of the scene to load
     * @param cacheDir   Working directory
     * @return  The loaded scene, or null if loading fails
     */
    fun loadScene(sceneName: String, cacheDir: File): Scene? {
        val meta = metadata ?: return null

        // Check cache
        cache.get(sceneName)?.let { return it as? Scene }

        // Check if this scene exists in the project
        val scene = meta.project.getSceneByName(sceneName) ?: return null

        return try {
            // Scene not cached — load from encrypted payload
            // Since we need specific scene data, we re-extract the relevant files
            val extractDir = File(cacheDir, "scene_${sceneName}")
            extractDir.deleteRecursively()
            extractDir.mkdirs()

            // Re-decrypt the scene's portion of the project
            val sceneDirOld = scene.directory
            val sceneExtractDir = File(extractDir, sceneDirOld.name)

            // NOTE (memory/IO): the full payload was already decrypted by
            // ProjectLoaderV3.loadLight into the on-disk file below. We deliberately do NOT
            // keep the decrypted bytes in a field. However, on-demand scene loading still
            // re-extracts the ENTIRE decrypted zip from disk here, which is an OOM/IO risk
            // for very large projects — prefer chunk-level extraction if this becomes a bottleneck.
            val decryptedZip = File(cacheDir, "project_light_decrypted.zip")
            if (decryptedZip.exists()) {
                ZipArchiver().unzip(decryptedZip, extractDir)
            } else {
                Log.e(tag, "Decrypted zip not found for on-demand scene loading")
                return null
            }

            val loadedSceneDir = File(extractDir, meta.projectDir.name)
            val sceneFiles = loadedSceneDir.listFiles()

            // Extract images and sounds for this specific scene
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

            // Update scene's directory references
            scene.setProject(meta.project)

            // Add to cache with memory tracking
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

    /**
     * Pre-loads the initial 30% of scenes (typically the first few).
     */
    fun preloadInitialScenes(cacheDir: File, count: Int = 3) {
        val meta = metadata ?: return
        val scenes = meta.project.sceneList.take(count)
        for (scene in scenes) {
            if (scene.name !in loadedScenes) {
                loadScene(scene.name, cacheDir)
            }
        }
    }

    /**
     * Evicts a scene from cache to free memory.
     */
    fun evictScene(sceneName: String) {
        cache.remove(sceneName)
        loadedScenes.remove(sceneName)
        Log.d(tag, "Scene '$sceneName' evicted from cache")
    }

    /**
     * Clears all cached scene data.
     */
    fun clearCache() {
        cache.clear()
        loadedScenes.clear()
        Log.d(tag, "Scene cache cleared")
    }

    /**
     * Returns the current memory usage of cached scenes in bytes.
     */
    val memoryUsageBytes: Long get() = cache.approximateMemoryBytes()

    /**
     * Returns the set of currently loaded scene names.
     */
    val activeScenes: Set<String> get() = loadedScenes.toSet()

    /**
     * Estimates the memory footprint of a scene.
     */
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
        return total.coerceAtLeast(1024L * 1024L) // minimum 1 MB
    }
}

/**
 * Internal helper for streaming file copies.
 */
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
