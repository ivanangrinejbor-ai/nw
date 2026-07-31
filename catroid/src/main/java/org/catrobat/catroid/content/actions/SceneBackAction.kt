package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GlobalManager
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.stage.StageActivity

class SceneBackAction : TemporalAction() {
    var sprite: Sprite? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        var previous: String? = GlobalManager.sceneBackStack.pollFirst()
        val project = org.catrobat.catroid.ProjectManager.getInstance().currentProject
        while (previous != null && project?.getSceneByName(previous) == null) {
            previous = GlobalManager.sceneBackStack.pollFirst()
        }
        if (previous == null) return
        sprite?.releaseAllPointers()
        GlobalManager.suppressNextBackStackPush = true
        listener.transitionToScene(previous, GlobalManager.stopSounds, GlobalManager.saveScenes)
    }
}
