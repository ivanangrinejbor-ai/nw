package org.catrobat.catroid.content.actions

import androidx.annotation.VisibleForTesting
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.JsonStore
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable

class JsonGetAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var keyFormula: Formula? = null
    var userVariable: UserVariable? = null

    @VisibleForTesting
    public override fun update(percent: Float) {
        val currentScope = scope ?: return
        val variable = userVariable ?: return
        val name = nameFormula?.interpretString(currentScope)?.trim().orEmpty()
        val key = keyFormula?.interpretString(currentScope)?.trim().orEmpty()
        if (name.isEmpty()) {
            variable.value = ""
            return
        }
        variable.value = JsonStore.get(name, key) ?: ""
    }
}
