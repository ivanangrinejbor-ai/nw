package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserDefinedScriptV2
import org.catrobat.catroid.formulaeditor.Formula

class SetCustomParamValueAction : TemporalAction() {
    var scope: Scope? = null
    var paramNameFormula: Formula? = null
    var valueFormula: Formula? = null

    override fun update(percent: Float) {
        val sc = scope ?: return
        val pName = paramNameFormula?.interpretString(sc) ?: return
        val valObj = valueFormula?.interpretObject(sc) ?: return
        val sequence = sc.sequence as? ScriptSequenceAction
        val script = sequence?.script
        if (script is UserDefinedScriptV2) {
            script.setParamValue(pName, valObj)
        }
    }
}
