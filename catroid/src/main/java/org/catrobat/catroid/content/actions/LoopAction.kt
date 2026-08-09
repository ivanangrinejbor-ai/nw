package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import org.catrobat.catroid.utils.LoopUtil

abstract class LoopAction : RepeatAction(), LoopControl {
    var isLoopDelay = true
    protected open var currentTime = 0f
    @Volatile override var breakLoop = false
    @Volatile override var continueLoop = false
    private var skipBody = false

    final override fun delegate(delta: Float): Boolean {
        if (breakLoop) {
            breakLoop = false
            continueLoop = false
            return true
        }
        if (continueLoop) {
            continueLoop = false
            return onContinue()
        }
        return loopDelegate(delta)
    }

    protected abstract fun loopDelegate(delta: Float): Boolean

    protected open fun onContinue(): Boolean {
        action?.restart()
        return false
    }

    protected fun isLoopDelayNeeded(): Boolean = currentTime < LOOP_DELAY &&
        isLoopDelay && !LoopUtil.isAnyStitchRunning()

    override fun restart() {
        breakLoop = false
        continueLoop = false
        super.restart()
    }

    companion object {
        private const val LOOP_DELAY = 0.02f
    }
}
