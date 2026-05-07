package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelSetTransparentAction : TemporalAction() {
    var scope: Scope? = null
    var blockId: Formula? = null
    var isTransparent: Formula? = null

    override fun update(percent: Float) {
        val id = blockId?.interpretDouble(scope)?.toInt()?.toShort() ?: 0
        val value = isTransparent?.interpretDouble(scope)?.toInt() == 1

        VoxelManager.setTransparent(id, value)
    }
}
