package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import org.catrobat.catroid.runtime.RuntimeServicesHolder

class RunOnUiThreadAction : Action() {
    var nestedAction: Action? = null
    private var posted = false

    override fun act(delta: Float): Boolean {
        if (!posted) {
            posted = true
            val action = nestedAction ?: return true
            // Posts action execution asynchronously to the UI thread without blocking the script runner
            RuntimeServicesHolder.services.postToMainThread {
                action.act(delta)
            }
        }
        return true
    }

    override fun restart() {
        super.restart()
        posted = false
        nestedAction?.restart()
    }

    override fun setActor(actor: Actor) {
        super.setActor(actor)
        nestedAction?.setActor(actor)
    }
}
