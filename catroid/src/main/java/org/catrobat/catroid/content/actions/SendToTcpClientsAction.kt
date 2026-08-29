package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class SendToTcpClientsAction : TemporalAction() {
    var scope: Scope? = null
    var values: List<Formula>? = null
    var echoMode: Int = 0

    override fun update(percent: Float) {
        val vals = values?.mapNotNull { it?.interpretString(scope) }
        if (!vals.isNullOrEmpty()) {
            val prefix = if (echoMode == 1) "ALL_ECHO:" else "ALL:"
            LocalServer.sendAll(listOf("$prefix${vals.joinToString(LocalServer.VALUE_SEPARATOR.toString())}"))
        }
    }
}
