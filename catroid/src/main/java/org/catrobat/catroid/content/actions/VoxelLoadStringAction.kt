package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelLoadStringAction : TemporalAction() {
    var scope: Scope? = null
    var worldId: Formula? = null
    var dataStr: Formula? = null
    var delimX: Formula? = null
    var delimY: Formula? = null
    var delimZ: Formula? = null

    override fun update(percent: Float) {
        val id = worldId?.interpretString(scope) ?: "chunk1"
        val data = dataStr?.interpretString(scope) ?: ""
        val dx = delimX?.interpretString(scope) ?: "$"
        val dy = delimY?.interpretString(scope) ?: "#"
        val dz = delimZ?.interpretString(scope) ?: "&"

        VoxelManager.loadFromString(id, data, dx, dy, dz)
    }
}
