package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.eventids.EventId
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import android.util.Log

class CreateObjectAction : TemporalAction() {
    var scope: Scope? = null
    var objectName: Formula? = null
    var targetSceneName: String? = null  // null = Current scene

    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true

        val project = scope?.project ?: return
        val name = objectName?.interpretString(scope) ?: return
        if (name.isBlank()) return

        // Resolve scene
        val scene = resolveScene(project)
        if (scene == null) {
            Log.e(TAG, "Target scene not found")
            return
        }

        // Check for duplicate name in this scene
        for (existing in scene.spriteList) {
            if (existing.name == name) {
                Log.w(TAG, "Object with name '$name' already exists in scene '${scene.name}'")
                return
            }
        }

        // Create the new blank sprite and add to the scene model
        val newSprite = Sprite(name)
        scene.spriteList.add(newSprite)

        // If this scene is currently active on stage, initialize the sprite
        // so its Start scripts (if any) will execute on the next frame.
        val stageListener = StageActivity.getActiveStageListener()
        if (stageListener != null) {
            val activeScene = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (activeScene != null && activeScene.getSceneId() == scene.getSceneId()) {
                newSprite.initializeEventThreads(EventId.START)
                newSprite.initConditionScriptTriggers()
            }
        }
    }

    private fun resolveScene(project: org.catrobat.catroid.content.Project): Scene? {
        val sceneName = targetSceneName
        if (sceneName == null || sceneName.isEmpty()) {
            // Current scene
            val current = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (current != null) return current
            return project.getDefaultScene()
        }
        return project.getSceneByName(sceneName)
    }

    companion object {
        private const val TAG = "CreateObjectAction"
    }
}

