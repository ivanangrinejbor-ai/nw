package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction

class BreakAction : TemporalAction() {

    private var loopControl: LoopControl? = null

    fun setLoopControl(loop: LoopControl?) {
        loopControl = loop
    }

    fun getLoopControl(): LoopControl? = loopControl

    override fun update(percent: Float) {
        val loop = loopControl ?: return
        loop.breakLoop = true
        loop.continueLoop = false
    }
}
