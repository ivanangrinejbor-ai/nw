package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable

class MaxOfListAction : TemporalAction() {

    var userList: UserList? = null
    var resultVariable: UserVariable? = null

    override fun update(percent: Float) {
        val list = userList ?: return
        val result = resultVariable ?: return
        var max = -Double.MAX_VALUE
        var found = false
        for (item in list.value) {
            val num = (item as? String)?.toDoubleOrNull() ?: (item as? Number)?.toDouble() ?: continue
            if (num > max) max = num
            found = true
        }
        result.value = if (found) max else 0.0
    }
}
