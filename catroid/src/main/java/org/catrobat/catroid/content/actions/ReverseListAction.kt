package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.formulaeditor.UserList
import java.util.ArrayList

class ReverseListAction : TemporalAction() {

    var userList: UserList? = null

    override fun update(percent: Float) {
        val list = userList ?: return
        @Suppress("UNCHECKED_CAST")
        val items = (list.value as? List<Any>) ?: return
        list.value = ArrayList(items.reversed())
    }
}
