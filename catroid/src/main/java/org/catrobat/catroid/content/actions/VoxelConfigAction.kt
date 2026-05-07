package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelConfigAction : TemporalAction() {
    var scope: Scope? = null
    var blockId: Formula? = null
    var shape: Formula? = null
    var atlasX: Formula? = null
    var atlasY: Formula? = null

    override fun update(percent: Float) {
        val id = blockId?.interpretDouble(scope)?.toInt()?.toShort() ?: 0
        val shp = shape?.interpretDouble(scope)?.toInt() ?: 0
        val tx = atlasX?.interpretDouble(scope)?.toInt() ?: 0
        val ty = atlasY?.interpretDouble(scope)?.toInt() ?: 0

        VoxelManager.configureBlock(id, shp, tx, ty)
    }
}
