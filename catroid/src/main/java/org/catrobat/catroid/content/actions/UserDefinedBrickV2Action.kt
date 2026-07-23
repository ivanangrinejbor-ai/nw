package org.catrobat.catroid.content.actions

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.eventids.UserDefinedBrickV2EventId
import org.catrobat.catroid.formulaeditor.Formula
import java.util.UUID

class UserDefinedBrickV2Action : SingleSpriteEventAction() {
    var scope: Scope? = null
        set(scope) {
            field = scope
            super.sprite = scope?.sprite
        }

    var userDefinedBrickID: UUID? = null
    var blockName: String? = null
    var paramFormulas: Map<String, Formula>? = null

    private fun getInterpretedInputs(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        paramFormulas?.forEach { (key, formula) ->
            val obj = formula.interpretObject(scope)
            if (obj != null) {
                result[key] = obj
            }
        }
        return result
    }

    override fun getEventId() =
        userDefinedBrickID?.let { id ->
            UserDefinedBrickV2EventId(id, getInterpretedInputs())
        }
}
