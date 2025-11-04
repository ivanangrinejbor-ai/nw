package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.virtualmachine.VirtualMachineManager

class BindVmOutputAction : TemporalAction() {
    var userVariable: UserVariable? = null

    override fun update(percent: Float) {
        val name = StageActivity.DEFAULT_VM_NAME
        VirtualMachineManager.setVmOutputVariable(name, userVariable)
    }
}