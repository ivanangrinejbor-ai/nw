package org.catrobat.catroid.content.actions

import android.os.Handler
import android.os.Looper
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor

class RunOnUiThreadAction : Action() {
    var nestedAction: Action? = null
    private var posted = false

    override fun act(delta: Float): Boolean {
        if (!posted) {
            posted = true
            val action = nestedAction ?: return true
            Handler(Looper.getMainLooper()).post {
                var remaining = action.act(delta)
                while (!remaining) {
                    remaining = action.act(delta)
                }
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
