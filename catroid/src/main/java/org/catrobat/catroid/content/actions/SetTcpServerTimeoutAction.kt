package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SetTcpServerTimeoutAction() : TemporalAction() {
    var scope: Scope? = null
    var timeout: Formula? = null

    override fun update(percent: Float) {
        val value = timeout?.interpretString(scope)?.toIntOrNull() ?: return
        LocalServer.serverTimeoutSeconds = value.coerceAtLeast(1)
    }
}