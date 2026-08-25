package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.JsonStore
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class JsonSetAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null
    var keyFormula: Formula? = null
    var valueFormula: Formula? = null

    override fun update(percent: Float) {
        val currentScope = scope ?: return
        val name = nameFormula?.interpretString(currentScope)?.trim().orEmpty()
        if (name.isEmpty()) return
        val key = keyFormula?.interpretString(currentScope)?.trim().orEmpty()
        if (key.isEmpty()) return
        val rawValue = valueFormula?.interpretString(currentScope).orEmpty()
        JsonStore.set(name, key, coerceValue(rawValue))
    }

    private fun coerceValue(raw: String): Any? = when {
        raw.equals("true", ignoreCase = true) -> true
        raw.equals("false", ignoreCase = true) -> false
        else -> raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw
    }
}
