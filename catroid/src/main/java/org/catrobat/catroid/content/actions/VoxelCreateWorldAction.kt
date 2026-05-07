package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelCreateWorldAction : TemporalAction() {
    var scope: Scope? = null
    var worldId: Formula? = null

    var sizeX: Formula? = null
    var sizeY: Formula? = null
    var sizeZ: Formula? = null
    var worldX: Formula? = null
    var worldY: Formula? = null
    var worldZ: Formula? = null

    override fun update(percent: Float) {
        val id = worldId?.interpretString(scope) ?: "chunk"

        val sx = sizeX?.interpretDouble(scope)?.toInt() ?: 16
        val sy = sizeY?.interpretDouble(scope)?.toInt() ?: 32
        val sz = sizeZ?.interpretDouble(scope)?.toInt() ?: 16

        val wx = worldX?.interpretDouble(scope)?.toInt() ?: 0
        val wy = worldY?.interpretDouble(scope)?.toInt() ?: 0
        val wz = worldZ?.interpretDouble(scope)?.toInt() ?: 0

        VoxelManager.createWorld(id, sx, sy, sz, wx, wy, wz)
    }
}
