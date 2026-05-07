package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager

class VoxelDeleteAction : TemporalAction() {
    var scope: Scope? = null
    var worldId: Formula? = null

    override fun update(percent: Float) {
        val id = worldId?.interpretString(scope) ?: ""
        if (id.isNotEmpty()) {
            VoxelManager.deleteWorld(id)
        }
    }
}
