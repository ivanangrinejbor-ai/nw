package org.catrobat.catroid.apkbuildV3.runtime

import android.content.Context
import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.XstreamSerializer
import java.io.File

class FullTemplateStrategy(private val context: Context) {
    private val tag = "FullTemplateStrategy"

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

            preloadProject(project, onProgress)

            ProjectManager.getInstance().currentProject = project

            onProgress?.invoke(1f)
            Log.i(tag, "Full template loaded successfully: ${project.name}")

            project.setDirectory(projectDir)

            true
        } catch (e: Exception) {
            Log.e(tag, "Full template loading failed", e)
            false
        }
    }

    private fun preloadProject(project: Project, onProgress: ((Float) -> Unit)?) {
        val totalScenes = project.sceneList.size
        if (totalScenes == 0) return

        val sceneProgressWeight = 0.4f

        project.sceneList.forEachIndexed { index, scene ->
            scene.firstStart = true

            for (sprite in scene.spriteList) {
                @Suppress("UNUSED_EXPRESSION")
                sprite.lookList.size
                @Suppress("UNUSED_EXPRESSION")
                sprite.soundList.size
                @Suppress("UNUSED_EXPRESSION")
                sprite.scriptList.size
            }

            val progress = 0.6f + (index + 1).toFloat() / totalScenes * sceneProgressWeight
            onProgress?.invoke(progress)
        }
    }
}
