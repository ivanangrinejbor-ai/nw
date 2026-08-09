package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable

class AverageOfListAction : TemporalAction() {

    var userList: UserList? = null
    var resultVariable: UserVariable? = null

    override fun update(percent: Float) {
        val list = userList ?: return
        val result = resultVariable ?: return
        var sum = 0.0
        var count = 0
        for (item in list.value) {
            val num = (item as? String)?.toDoubleOrNull() ?: (item as? Number)?.toDouble() ?: continue
            sum += num
            count++
        }
        result.value = if (count > 0) sum / count else 0.0
    }
}
