package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GlobalManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetGameTimeScaleAction : TemporalAction() {
    var scope: Scope? = null
    var scale: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val value = scale?.interpretDouble(scope) ?: return
        GlobalManager.gameTimeScale = value.toFloat()
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
