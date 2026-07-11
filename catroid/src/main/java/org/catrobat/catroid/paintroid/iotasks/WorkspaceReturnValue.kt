package org.catrobat.catroid.paintroid.iotasks

import org.catrobat.catroid.paintroid.colorpicker.ColorHistory
import org.catrobat.catroid.paintroid.model.CommandManagerModel

data class WorkspaceReturnValue(
    val commandManagerModel: CommandManagerModel?,
    val colorHistory: ColorHistory?
)
