package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.VoxelManager
import org.catrobat.catroid.stage.StageActivity

class VoxelBuildAction : TemporalAction() {
    var scope: Scope? = null
    var worldId: Formula? = null
    var texture: Formula? = null
    var atlasW: Formula? = null
    var atlasH: Formula? = null

    override fun update(percent: Float) {
        val id = worldId?.interpretString(scope) ?: "chunk1"
        val tex = texture?.interpretString(scope) ?: ""
        val w = atlasW?.interpretDouble(scope)?.toInt() ?: 16
        val h = atlasH?.interpretDouble(scope)?.toInt() ?: 16

        val buffer = VoxelManager.getBuffer(id)
        if (buffer != null) {
            val stageListener = StageActivity.getActiveStageListener()
            stageListener?.threeDManager?.updateVoxelMesh(id, buffer, tex, w, h)
        }
    }
}
