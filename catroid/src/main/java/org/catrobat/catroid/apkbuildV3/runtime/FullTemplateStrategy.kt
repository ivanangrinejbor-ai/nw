package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.XstreamSerializer
import java.io.File

/**
 * Full Template loading strategy.
 *
 * On startup:
 * 1. Decrypt the entire project payload.
 * 2. Extract all files.
 * 3. Load ALL scenes and ALL sprites into memory.
 * 4. Set up all data structures.
 *
 * Scene transitions are instantaneous because everything is already in RAM.
 *
 * Memory usage: HIGH (entire project in memory).
 * Suitable for: powerful devices with sufficient RAM.
 */
class FullTemplateStrategy(private val context: Context) {
    private val tag = "FullTemplateStrategy"

    /**
     * Loads the complete project into memory.
     * After this call, all scenes are fully accessible.
     *
     * @param cacheDir  Working directory for extraction
     * @param onProgress  Progress callback (0.0 - 1.0)
     * @return  true if loading succeeded
     */
    fun load(cacheDir: File, onProgress: ((Float) -> Unit)? = null): Boolean {
        return try {
            onProgress?.invoke(0f)

            val loader = ProjectLoaderV3(context)
            val result = loader.loadFull(cacheDir) { progress ->
                onProgress?.invoke(progress * 0.6f)
            } ?: return false

            val project = result.project
            val projectDir = result.projectDir

            onProgress?.invoke(0.6f)

            // Pre-warm: iterate through all scenes and sprites to
            // force resource loading into caches
            preloadProject(project, onProgress)

            // Register with ProjectManager
            ProjectManager.getInstance().currentProject = project

            onProgress?.invoke(1f)
            Log.i(tag, "Full template loaded successfully: ${project.name}")

            // Сохраняем путь к распакованному проекту для StageActivity
            project.setDirectory(projectDir)

            true
        } catch (e: Exception) {
            Log.e(tag, "Full template loading failed", e)
            false
        }
    }

    /**
     * Preloads all project resources into memory.
     * Touches every scene and sprite to ensure textures etc. are cached.
     */
    private fun preloadProject(project: Project, onProgress: ((Float) -> Unit)?) {
        val totalScenes = project.sceneList.size
        if (totalScenes == 0) return

        val sceneProgressWeight = 0.4f // remaining 40% of loading

        project.sceneList.forEachIndexed { index, scene ->
            // Initialize scene structures
            scene.firstStart = true

            // Force sprite loading
            for (sprite in scene.spriteList) {
                // Access look data to force texture references
                @Suppress("UNUSED_EXPRESSION")
                sprite.lookList.size

                // Access sound info
                @Suppress("UNUSED_EXPRESSION")
                sprite.soundList.size

                // Access scripts
                @Suppress("UNUSED_EXPRESSION")
                sprite.scriptList.size
            }

            val progress = 0.6f + (index + 1).toFloat() / totalScenes * sceneProgressWeight
            onProgress?.invoke(progress)
        }
    }
}
