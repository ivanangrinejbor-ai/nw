package org.catrobat.catroid.ai.analysis

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.context.ContextManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick

object ProjectAnalyzer {

    data class AnalysisResult(
        val projectName: String,
        val totalScenes: Int,
        val totalObjects: Int,
        val totalScripts: Int,
        val totalBricks: Int,
        val scenes: List<SceneAnalysis>,
        val summary: String
    )

    data class SceneAnalysis(
        val name: String,
        val objects: List<ObjectAnalysis>
    )

    data class ObjectAnalysis(
        val name: String,
        val scriptCount: Int,
        val brickCount: Int,
        val scriptTypes: Map<String, Int>,
        val brickTypes: Map<String, Int>,
        val hasLooks: Boolean,
        val hasSounds: Boolean
    )

    fun analyzeProject(project: Project): AnalysisResult {
        val scenes = project.sceneList.map { scene ->
            ContextManager.cacheScene(scene)
            val objects = scene.spriteList.map { sprite ->
                ContextManager.cacheObject(scene.name, sprite)
                analyzeSprite(sprite)
            }
            SceneAnalysis(scene.name, objects)
        }

        val totalObjects = scenes.sumOf { it.objects.size }
        val totalScripts = scenes.sumOf { scene -> scene.objects.sumOf { it.scriptCount } }
        val totalBricks = scenes.sumOf { scene -> scene.objects.sumOf { it.brickCount } }

        val summary = buildString {
            appendLine("Project: ${project.name}")
            appendLine("Scenes: ${scenes.size}")
            appendLine("Total objects: $totalObjects")
            appendLine("Total scripts: $totalScripts")
            appendLine("Total bricks: $totalBricks")
            appendLine()
            for (scene in scenes) {
                appendLine("Scene: ${scene.name}")
                for (obj in scene.objects) {
                    appendLine("  ${obj.name}: ${obj.scriptCount} scripts, ${obj.brickCount} bricks")
                }
            }
        }

        ContextManager.markProjectAnalyzed(project.name)

        return AnalysisResult(
            projectName = project.name,
            totalScenes = scenes.size,
            totalObjects = totalObjects,
            totalScripts = totalScripts,
            totalBricks = totalBricks,
            scenes = scenes,
            summary = summary
        )
    }

    private fun analyzeSprite(sprite: Sprite): ObjectAnalysis {
        val scriptTypes = mutableMapOf<String, Int>()
        val brickTypes = mutableMapOf<String, Int>()
        var brickCount = 0

        for (script in sprite.scriptList) {
            val scriptName = script::class.java.simpleName
            scriptTypes[scriptName] = (scriptTypes[scriptName] ?: 0) + 1

            val bricks = script.getBrickList()
            brickCount += bricks.size
            for (brick in bricks) {
                val brickName = brick::class.java.simpleName
                brickTypes[brickName] = (brickTypes[brickName] ?: 0) + 1
            }
        }

        return ObjectAnalysis(
            name = sprite.name,
            scriptCount = sprite.scriptList.size,
            brickCount = brickCount,
            scriptTypes = scriptTypes,
            brickTypes = brickTypes,
            hasLooks = sprite.lookList.isNotEmpty(),
            hasSounds = sprite.soundList.isNotEmpty()
        )
    }

    fun findUnusedVariables(project: Project): List<String> {
        // TODO: implement actual unused variable analysis
        return emptyList()
    }

    fun findUnusedBroadcasts(project: Project): List<String> {
        // TODO: implement actual unused broadcast analysis
        return emptyList()
    }

    fun findDuplicatedScripts(project: Project): List<String> {
        // TODO: implement actual duplicate script detection
        return emptyList()
    }
}
