package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class DeleteParticlesAction : TemporalAction() {
    var scope: Scope? = null
    var particleId: Formula? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val sceneManager = listener.sceneManager ?: return
        val id = particleId?.interpretString(scope) ?: return

        val hostObject = sceneManager.findGameObject(id)
        if (hostObject != null) {
            sceneManager.removeGameObject(hostObject)
        }
    }
}