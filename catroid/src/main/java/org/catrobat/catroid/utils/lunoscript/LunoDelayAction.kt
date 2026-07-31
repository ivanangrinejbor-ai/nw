package org.catrobat.catroid.utils.lunoscript

import com.badlogic.gdx.scenes.scene2d.Action

class LunoDelayAction(val durationInSeconds: Float) : Action() {
    private var passedTime = 0f

    override fun act(delta: Float): Boolean {
        passedTime += delta
        return passedTime >= durationInSeconds
    }
}