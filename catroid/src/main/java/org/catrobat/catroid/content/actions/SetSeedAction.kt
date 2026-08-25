package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GlobalManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetSeedAction : TemporalAction() {
    var scope: Scope? = null
    var seed: Formula? = null
    private var started = false

    override fun update(percent: Float) {
        if (started) return
        started = true
        val value = seed?.interpretDouble(scope) ?: return
        GlobalManager.setRandomSeed(value.toLong())
    }

    override fun restart() {
        super.restart()
        started = false
    }
}
