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

/**
 * Dynamic context manager for the AI suggestion pipeline.
 *
 * Builds structured context with transformer-friendly object boundaries:
 *   <project_start>  <scene name="...">  <object_start name="..." isClone="false">
 *     <script_start type="StartScript">  brick_1  brick_2  ...  <script_end>
 *   <object_end>  <scene_end>  <project_end>
 *
 * Special tokens:
 *   <T_START> <T_END> <OBJ_START> <OBJ_END> <SCRIPT_START> <SCRIPT_END>
 *   <GLOBAL_VAR> <GLOBAL_LIST> <BROADCAST> <SIGNAL>
 *
 * Context size adapts dynamically to AiConfig.maxTokens.
 * On high limits, includes global variables/lists/broadcasts for full project awareness.
 *
 * All heavy work runs on Dispatchers.Default to keep UI responsive.
 */
object AipContextManager {

    // Special structural tokens for transformer LoRA compatibility
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

    /**
     * Assembled context for a single script prediction.
     * @param tokens Flattened token sequence ready for model input.
     * @param tokenCount Approximate token count (for logging).
     * @param objectCount Number of objects included in the context.
     */
    data class AssembledContext(
        val tokens: List<String>,
        val tokenCount: Int,
        val objectCount: Int
    )

    /**
     * Build context for predicting the next brick in [targetScript].
     * Runs on IO/Default — call from a coroutine.
     *
     * @param targetScript The script we are suggesting for.
     * @return Assembled context with structural tokens, truncated to AiConfig.maxTokens.
     */
    suspend fun buildContext(targetScript: Script): AssembledContext = withContext(Dispatchers.Default) {
        val project = ProjectManager.getInstance().currentProject ?: return@withContext AssembledContext(emptyList(), 0, 0)
        val maxTok = AiConfig.maxTokens
        val tokens = mutableListOf<String>()

        // ---- Project header ----
        tokens.add(T_PROJECT_START)

        // ---- Global scope (high-token mode only) ----
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

        // ---- Scenes ----
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

        // Truncate to max tokens
        val truncated = if (tokens.size > maxTok) tokens.takeLast(maxTok) else tokens
        val objCount = truncated.count { it == T_OBJECT_START }

        AssembledContext(truncated, truncated.size, objCount)
    }

    /**
     * Simplified context for fast n-gram prediction.
     * Recursively flattens nested bricks inside composite blocks.
     */
    fun buildSimpleContext(script: Script): List<String> {
        val flat = mutableListOf<String>()
        for (brick in script.brickList) {
            addBrickToContext(flat, brick)
        }
        return flat
    }

    /**
     * Build context for a specific position inside a composite block.
     * If [parentBrick] is a CompositeBrick, assembles bricks INSIDE it.
     * Otherwise uses the full script.
     */
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

    /**
     * Extract brick types used in a script (for n-gram exclusion).
     */
    fun getExistingTypes(script: Script): Set<String> =
        script.brickList.map { it.javaClass.simpleName }.toSet()

    /**
     * Count token equivalents (rough: 1 token ≈ 1 word/identifier).
     */
    fun countTokens(sequence: List<String>): Int = sequence.size
}
