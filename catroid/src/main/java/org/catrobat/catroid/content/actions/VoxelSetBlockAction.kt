package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelSetBlockAction : TemporalAction() {
    var scope: Scope? = null
    var worldId: Formula? = null
    var x: Formula? = null
    var y: Formula? = null
    var z: Formula? = null
    var type: Formula? = null
    var data: Formula? = null

    override fun update(percent: Float) {
        val id = worldId?.interpretString(scope) ?: "chunk1"
        val vx = x?.interpretDouble(scope)?.toInt() ?: 0
        val vy = y?.interpretDouble(scope)?.toInt() ?: 0
        val vz = z?.interpretDouble(scope)?.toInt() ?: 0
        val vType = type?.interpretDouble(scope)?.toInt()?.toShort() ?: 1
        val vData = data?.interpretDouble(scope)?.toInt()?.toByte() ?: 0

        VoxelManager.setBlock(id, vx, vy, vz, vType, vData)
    }
}
