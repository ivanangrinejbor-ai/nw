package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserDefinedScriptV2
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class GetCustomParamAction : TemporalAction() {
    var scope: Scope? = null
    var paramNameFormula: Formula? = null
    var targetVar: UserVariable? = null

    override fun update(percent: Float) {
        val sc = scope ?: return
        val pName = paramNameFormula?.interpretString(sc) ?: return
        val target = targetVar ?: return

        // Search active script sequence for UserDefinedScriptV2
        val sequence = sc.sequence as? ScriptSequenceAction
        val script = sequence?.script
        if (script is UserDefinedScriptV2) {
            val valObj = script.getParamValue(pName) ?: script.getParamValue("$$pName")
            if (valObj != null) {
                target.value = valObj
            }
        }
    }
}
