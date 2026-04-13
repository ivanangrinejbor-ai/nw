package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class BakeByPrefixAction : TemporalAction() {
    var scope: Scope? = null
    var prefix: Formula? = null
    var resultName: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.getActiveStageListener()?.sceneManager ?: return
        val pre = prefix?.interpretString(scope) ?: return
        val res = resultName?.interpretString(scope) ?: "BakedGroup"

        if (pre.isEmpty()) return

        manager.bakeByPrefix(pre, res)
    }
}