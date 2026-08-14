package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.NewCatroidHttpManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class HttpClearAction : TemporalAction() {
    var scope: Scope? = null
    var requestId: Formula? = null
    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true
        NewCatroidHttpManager.clearRequest(requestId?.interpretString(scope) ?: "")
    }

    override fun restart() {
        super.restart()
        executed = false
    }
}
