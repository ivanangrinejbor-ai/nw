package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GlobalManager
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.stage.StageActivity

/**
 * Переходит к предыдущей сцене из истории переключений (back stack).
 * Если истории нет — ничего не делает.
 */
class SceneBackAction : TemporalAction() {
    var sprite: Sprite? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        // Пропускаем удалённые сцены в истории
        var previous: String? = GlobalManager.sceneBackStack.pollFirst()
        val project = org.catrobat.catroid.ProjectManager.getInstance().currentProject
        while (previous != null && project?.getSceneByName(previous) == null) {
            previous = GlobalManager.sceneBackStack.pollFirst()
        }
        if (previous == null) return
        sprite?.releaseAllPointers()
        // Флаг: этот переход — "назад", текущую сцену в стек НЕ пушить,
        // иначе «назад-назад» зациклится между двумя сценами.
        GlobalManager.suppressNextBackStackPush = true
        listener.transitionToScene(previous, GlobalManager.stopSounds, GlobalManager.saveScenes)
    }
}
