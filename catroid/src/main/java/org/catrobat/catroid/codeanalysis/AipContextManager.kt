package org.catrobat.catroid.codeanalysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.*
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable

object AipContextManager {

    const val T_PROJECT_START  = "<project_start>"
    const val T_PROJECT_END    = "<project_end>"
    const val T_SCENE_START    = "<scene_start>"
    const val T_SCENE_END      = "<scene_end>"
    const val T_OBJECT_START   = "<object_start>"
    const val T_OBJECT_END     = "<object_end>"
    const val T_SCRIPT_START   = "<script_start>"
    const val T_SCRIPT_END     = "<script_end>"
    const val T_GLOBAL_VAR     = "<global_var>"
    const val T_GLOBAL_LIST    = "<global_list>"
    const val T_BROADCAST      = "<broadcast>"
    const val T_SIGNAL         = "<signal>"

    data class AssembledContext(
        val tokens: List<String>,
        val tokenCount: Int,
        val objectCount: Int
    )

    suspend fun buildContext(targetScript: Script): AssembledContext = withContext(Dispatchers.Default) {
        val project = ProjectManager.getInstance().currentProject ?: return@withContext AssembledContext(emptyList(), 0, 0)
        val maxTok = AiConfig.maxTokens
        val tokens = mutableListOf<String>()

        tokens.add(T_PROJECT_START)

        if (maxTok > 5000) {
            tokens.add(T_GLOBAL_VAR)
            for (v in project.userVariables) {
                tokens.add("var:${v.name}")
            }
            tokens.add(T_GLOBAL_LIST)
            for (l in project.userLists) {
                tokens.add("list:${l.name}")
            }
            tokens.add(T_BROADCAST)
            for (msg in project.broadcastMessageContainer.broadcastMessages) {
                tokens.add("msg:${msg}")
            }
        }

        for (scene in project.sceneList) {
            if (tokens.size >= maxTok) break
            tokens.add(T_SCENE_START)
            tokens.add("scene_name=${scene.name}")

            for (sprite in scene.spriteList) {
                if (tokens.size >= maxTok) break
                tokens.add(T_OBJECT_START)
                tokens.add("name=${sprite.name}")

                for (script in sprite.scriptList) {
                    if (tokens.size >= maxTok) break
                    val typeName = script.javaClass.simpleName
                    tokens.add(T_SCRIPT_START)
                    tokens.add("script_type=${typeName}")

                    for (brick in script.brickList) {
                        if (tokens.size >= maxTok) break
                        tokens.add(brick.javaClass.simpleName)
                        if (brick is CompositeBrick) {
                            for (nested in brick.nestedBricks) {
                                if (tokens.size >= maxTok) break
                                tokens.add("  " + nested.javaClass.simpleName)
                            }
                            if (brick.hasSecondaryList()) {
                                tokens.add("  Else")
                                for (nested in brick.secondaryNestedBricks) {
                                    if (tokens.size >= maxTok) break
                                    tokens.add("  " + nested.javaClass.simpleName)
                                }
                            }
                            tokens.add("  End")
                        }
                    }

                    tokens.add(T_SCRIPT_END)
                }

                tokens.add(T_OBJECT_END)
            }

            tokens.add(T_SCENE_END)
        }

        tokens.add(T_PROJECT_END)

        val truncated = if (tokens.size > maxTok) tokens.takeLast(maxTok) else tokens
        val objCount = truncated.count { it == T_OBJECT_START }

        AssembledContext(truncated, truncated.size, objCount)
    }

    fun buildSimpleContext(script: Script): List<String> {
        val flat = mutableListOf<String>()
        for (brick in script.brickList) {
            addBrickToContext(flat, brick)
        }
        return flat
    }

    fun buildContextForParent(script: Script, parentBrick: Brick?): List<String> {
        if (parentBrick is CompositeBrick) {
            val flat = mutableListOf<String>()
            for (brick in parentBrick.nestedBricks) {
                addBrickToContext(flat, brick)
            }
            return flat
        }
        return buildSimpleContext(script)
    }

    private fun addBrickToContext(flat: MutableList<String>, brick: Brick) {
        flat.add(brick.javaClass.simpleName)
        if (brick is CompositeBrick) {
            for (nested in brick.nestedBricks) {
                addBrickToContext(flat, nested)
            }
            if (brick.hasSecondaryList()) {
                flat.add("Else")
                for (nested in brick.secondaryNestedBricks) {
                    addBrickToContext(flat, nested)
                }
            }
            flat.add("End")
        }
    }

    fun getExistingTypes(script: Script): Set<String> =
        script.brickList.map { it.javaClass.simpleName }.toSet()

    fun countTokens(sequence: List<String>): Int = sequence.size
}
