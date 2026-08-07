package org.catrobat.catroid.ai.analysis

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.context.ContextManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.formulaeditor.FormulaElement

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
        val declared = mutableSetOf<String>()
        project.userVariables.forEach { declared.add(it.name ?: "") }
        project.multiplayerVariables.forEach { declared.add(it.name ?: "") }
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                sprite.userVariables.forEach { declared.add(it.name ?: "") }
                sprite.userLists.forEach { declared.add(it.name ?: "") }
            }
        }

        val used = mutableSetOf<String>()
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (script in sprite.scriptList) {
                    for (brick in script.getBrickList()) {
                        collectFormulaVariableRefs(brick, used)
                    }
                }
            }
        }
        return declared.filter { it.isNotBlank() && it !in used }
    }

    fun findUnusedBroadcasts(project: Project): List<String> {
        val declared = project.broadcastMessageContainer?.broadcastMessages?.toMutableSet() ?: mutableSetOf()
        val used = mutableSetOf<String>()
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (script in sprite.scriptList) {
                    for (brick in script.getBrickList()) {
                        extractBroadcastMessage(brick)?.let { used.add(it) }
                    }
                }
            }
        }
        return declared.filter { it !in used }
    }

    fun findDuplicatedScripts(project: Project): List<String> {
        val result = mutableListOf<String>()
        val signatures = mutableMapOf<String, String>()
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for ((i, script) in sprite.scriptList.withIndex()) {
                    val sig = scriptSignature(script)
                    val loc = "${scene.name}/${sprite.name}#$i"
                    val prev = signatures.putIfAbsent(sig, loc)
                    if (prev != null && !sig.startsWith("StartScript:")) {
                        result.add("Script '$sig' in '$loc' is a duplicate of '$prev'")
                    }
                }
            }
        }
        return result
    }

    private fun scriptSignature(script: Script): String {
        val types = script.getBrickList().joinToString(",") { it::class.java.simpleName }
        return "${script::class.java.simpleName}:$types"
    }

    private fun collectFormulaVariableRefs(brick: Brick, used: MutableSet<String>) {
        if (brick !is FormulaBrick) return
        try {
            for (formula in brick.allFormulaFieldsWithFormulas.values) {
                collectFormulaElementRefs(formula.root, used)
            }
        } catch (_: Exception) {}
    }

    private fun collectFormulaElementRefs(element: FormulaElement?, used: MutableSet<String>) {
        if (element == null) return
        if (element.elementType == FormulaElement.ElementType.USER_VARIABLE ||
            element.elementType == FormulaElement.ElementType.USER_LIST
        ) {
            element.value?.let { used.add(it) }
        }
        collectFormulaElementRefs(element.leftChild, used)
        collectFormulaElementRefs(element.rightChild, used)
        element.additionalChildren?.forEach { collectFormulaElementRefs(it, used) }
    }

    private fun extractBroadcastMessage(brick: Brick): String? {
        return try {
            val f = brick.javaClass.getDeclaredField("broadcastMessage")
            f.isAccessible = true
            val obj = f.get(brick) ?: return null
            if (obj is String) {
                obj
            } else {
                val nameField = obj.javaClass.getDeclaredField("name")
                nameField.isAccessible = true
                nameField.get(obj) as? String
            }
        } catch (_: Exception) { null }
    }

    data class BugItem(
        val category: String,
        val description: String,
        val location: String,
        val autoFixable: Boolean = true
    )

    fun findProjectBugs(project: Project): List<BugItem> {
        val bugs = mutableListOf<BugItem>()
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (script in sprite.scriptList) {
                    val bricks = script.getBrickList()
                    for (i in bricks.indices) {
                        val brick = bricks[i]
                        val name = brick::class.java.simpleName
                        if (name == "ForeverBrick") {
                            val hasWait = bricks.drop(i).takeWhile { it::class.java.simpleName != "LoopEndBrick" }
                                .any { it::class.java.simpleName.contains("Wait") }
                            if (!hasWait) {
                                bugs.add(BugItem("PERFORMANCE", "Forever loop without Wait brick in ${sprite.name}", "${scene.name}/${sprite.name}"))
                            }
                        }
                    }
                }
            }
        }
        return bugs
    }
}
