package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class CheckPortAction() : TemporalAction() {
    var scope: Scope? = null
    var port: Formula? = null
    var variable: UserVariable? = null

    override fun update(percent: Float) {
        val port = port ?: return
        val raw = port.interpretString(scope)?.trim()
        val portNumber = raw?.toDoubleOrNull()?.let { d ->
            if (d.isFinite() && d == Math.floor(d)) d.toInt() else null
        }
        if (portNumber == null || portNumber !in 1..65535) {
            variable?.value = "ERROR"
            return
        }
        variable?.value = LocalServer.isPortInUse(portNumber).toString()
    }
}