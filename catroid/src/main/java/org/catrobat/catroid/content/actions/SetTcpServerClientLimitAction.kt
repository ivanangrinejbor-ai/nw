package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetTcpServerClientLimitAction() : TemporalAction() {
    var scope: Scope? = null
    var limit: Formula? = null

    override fun update(percent: Float) {
        val value = limit?.interpretString(scope)?.toIntOrNull() ?: return
        LocalServer.clientLimit = value.coerceAtLeast(1)
    }
}