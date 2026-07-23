package org.catrobat.catroid.content.actions

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserDefinedScriptV2
import org.catrobat.catroid.formulaeditor.Formula

class IfCustomParamEqualsAction : LoopAction() {
    var scope: Scope? = null
    var paramNameFormula: Formula? = null
    var expectedValFormula: Formula? = null
    private var conditionMatched = false

    override fun delegate(delta: Float): Boolean {
        val sc = scope ?: return true
        val sequence = sc.sequence as? ScriptSequenceAction
        val script = sequence?.script
        if (script is UserDefinedScriptV2) {
            val pName = paramNameFormula?.interpretString(sc) ?: ""
            val expected = expectedValFormula?.interpretString(sc) ?: ""
            val actual = script.getParamValue(pName)?.toString() ?: ""
            conditionMatched = (actual == expected)
        } else {
            conditionMatched = false
        }

        if (conditionMatched && action != null) {
            return action.act(delta)
        }
        return true
    }

    override fun restart() {
        conditionMatched = false
        super.restart()
    }
}
