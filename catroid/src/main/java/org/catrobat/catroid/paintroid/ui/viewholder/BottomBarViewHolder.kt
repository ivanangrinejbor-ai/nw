package org.catrobat.catroid.paintroid.ui.viewholder

import android.view.View
import org.catrobat.catroid.paintroid.contract.MainActivityContracts
import org.catrobat.catroid.paintroid.tools.ToolType

class BottomBarViewHolder(val layout: View) : MainActivityContracts.BottomBarViewHolder {
    override val isVisible: Boolean
        get() = layout.visibility == View.VISIBLE

    override fun show() {
        layout.visibility = View.VISIBLE
    }

    override fun hide() {
        layout.visibility = View.GONE
    }

    override fun setSelectedTool(toolType: ToolType) {
        val handledButtonIds = HashSet<Int>()
        for (type in ToolType.values()) {
            if (!handledButtonIds.add(type.toolButtonID)) {
                continue
            }
            val button = layout.findViewById<View>(type.toolButtonID) ?: continue
            button.isSelected = type == toolType
        }
    }
}
