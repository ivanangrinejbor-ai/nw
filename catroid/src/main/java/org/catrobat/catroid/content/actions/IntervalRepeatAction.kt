package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.util.Log

class IntervalRepeatAction : Action() {
    companion object {
        private const val MAX_ITERATIONS = 100_000
        private const val MAX_CONCURRENT_CLONES = 10
    }

    var scope: Scope? = null
    var repeatCount: Formula? = null
    var interval: Formula? = null
    var loopBodyAction: Action? = null

    private var isInitialized = false
    private var executedCount = 0
    private var repeatCountValue = 0
    private var intervalValue = 0f
    private var timer = 0f
    private var activeClones = mutableListOf<Action>()

    override fun act(delta: Float): Boolean {
        if (!isInitialized) {
            initialize()
        }
        if (repeatCountValue > 0 && executedCount >= repeatCountValue) {
            cleanupClones()
            return true
        }
        if (executedCount >= MAX_ITERATIONS) {
            Log.w(javaClass.simpleName, "Interval repeat exceeded max iterations ($MAX_ITERATIONS), stopping")
            cleanupClones()
            return true
        }

        timer += delta
        if (timer >= intervalValue) {
            if (repeatCountValue > 0) {
                executedCount++
            } else {
                executedCount++
            }

            timer -= intervalValue

            cleanupClones()

            if (activeClones.size >= MAX_CONCURRENT_CLONES) {
                Log.w(javaClass.simpleName, "Too many concurrent clones ($MAX_CONCURRENT_CLONES), waiting for cleanup")
                return false
            }

            val actionClone = (loopBodyAction as? ScriptSequenceAction)?.clone()
            if (actionClone != null) {
                actionClone.restart()
                actor.addAction(actionClone)
                activeClones.add(actionClone)
            }
        }

        return repeatCountValue > 0 && executedCount >= repeatCountValue
    }
    
    private fun cleanupClones() {
        val iterator = activeClones.iterator()
        while (iterator.hasNext()) {
            val clone = iterator.next()
            if (clone.actor == null || !actor.actions.contains(clone)) {
                iterator.remove()
            }
        }
    }

    private fun initialize() {
        isInitialized = true
        executedCount = 0
        timer = 0f

        repeatCountValue = try {
            repeatCount?.interpretInteger(scope) ?: 0
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Repeat count interpretation failed.", e)
            0
        }

        intervalValue = try {
            interval?.interpretFloat(scope)?.takeIf { it >= 0 } ?: 0f
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "Interval interpretation failed.", e)
            0f
        }
    }

    override fun restart() {
        isInitialized = false
        executedCount = 0
        timer = 0f
        cleanupClones()
        activeClones.clear()
        loopBodyAction?.restart()
        super.restart()
    }
}