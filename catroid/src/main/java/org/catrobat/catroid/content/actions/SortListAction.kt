package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.formulaeditor.UserList
import java.util.ArrayList

class SortListAction : TemporalAction() {

    var userList: UserList? = null

    @JvmField
    var ascending = true

    override fun update(percent: Float) {
        val list = userList ?: return
        @Suppress("UNCHECKED_CAST")
        val items = (list.value as? List<Any>) ?: return
        val sorted = try {
            if (ascending) {
                items.sortedBy { (it as? String)?.toDoubleOrNull() ?: (it as? Number)?.toDouble() ?: Double.MAX_VALUE }
            } else {
                items.sortedByDescending { (it as? String)?.toDoubleOrNull() ?: (it as? Number)?.toDouble() ?: -Double.MAX_VALUE }
            }
        } catch (e: Exception) {
            return
        }
        list.value = ArrayList(sorted)
    }
}
