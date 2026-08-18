package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick

class CodeAnalyzer(private val context: Context) {
    private val rules = listOf<AnalysisRule>(
        ErrorRule(context),
        EmptyLoopRule(context),
        ConstantConditionRule(context),
        UnreachableCodeRule(context),
        RedundantBlockRule(context),
        ResourceLeakRule(context),
        InvalidCloneUsageRule(context),
        ParameterValidationRule(context),
        UndefinedReferenceRule(context),
        UnusedVariableRule(context),
        DeadScriptRule(context),
        ThreedCompatibilityRule(context),
        EventCascadeRule(context),
        DivisionByZeroRule(context)
    )

    val aiRule = AiSuggestionRule(context)

    fun analyzeScript(script: Script): Map<Brick, AnalysisResult> {
        if (script.isCommentedOut) {
            return emptyMap()
        }
        val results = mutableMapOf<Brick, AnalysisResult>()
        analyzeBrickList(script.brickList, results)
        return results
    }

    fun analyzeScriptWithAi(script: Script): Map<Brick, AnalysisResult> {
        val results = analyzeScript(script).toMutableMap()
        results.putAll(aiRule.getResults().filterKeys { it in script.brickList })
        return results
    }

    private fun analyzeBrickList(brickList: List<Brick>, results: MutableMap<Brick, AnalysisResult>) {
        for (brick in brickList) {
            if (brick.isCommentedOut) {
                continue
            }
            for (rule in rules) {
                val result = rule.analyze(brick)
                if (result != null) {
                    results[brick] = result
                    break
                }
            }

            if (brick is CompositeBrick) {
                brick.nestedBricks?.let { analyzeBrickList(it, results) }

                if (brick.hasSecondaryList()) {
                    brick.secondaryNestedBricks?.let { analyzeBrickList(it, results) }
                }

                if (brick is TryCatchFinallyBrick) {
                    brick.thirdNestedBricks?.let { analyzeBrickList(it, results) }
                }
            }
        }
    }

    companion object {
        fun isBrickCommentedDirectly(brick: Brick?): Boolean {
            if (brick == null) return false
            return try {
                val method = brick.javaClass.getMethod("isCommentedOut")
                method.invoke(brick) as? Boolean ?: false
            } catch (_: Exception) {
                try {
                    val field = brick.javaClass.getField("commentedOut")
                    field.isAccessible = true
                    field.get(brick) as? Boolean ?: false
                } catch (_: Exception) {
                    false
                }
            }
        }

        fun isBrickCommented(brick: Brick?): Boolean {
            val visited = mutableSetOf<Any>()
            var current: Any? = brick

            while (current != null && visited.add(current)) {
                if (current is Brick) {
                    if (isBrickCommentedDirectly(current)) return true

                    val parent = try {
                        val parentField = current.javaClass.getField("parent")
                        parentField.isAccessible = true
                        parentField.get(current)
                    } catch (_: Exception) {
                        try {
                            val parentMethod = current.javaClass.getMethod("getParent")
                            parentMethod.invoke(current)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    current = parent
                } else {
                    break
                }
            }
            return false
        }
    }
}
