package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.virtualmachine.VirtualMachineManager

class RunVm2Action : TemporalAction() {
    var scope: Scope? = null
    var arguments: Formula? = null

    override fun update(percent: Float) {
        val context = CatroidApplication.getAppContext()
        val argsString = arguments?.interpretString(scope) ?: ""

        VirtualMachineManager.createVM(context, StageActivity.DEFAULT_VM_NAME, argsString)
    }
}