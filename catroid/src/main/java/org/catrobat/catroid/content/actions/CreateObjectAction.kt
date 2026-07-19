package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.RuntimeMutationTracker
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.eventids.EventId
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.io.asynctask.ProjectSaver
import android.util.Log

class CreateObjectAction : TemporalAction() {
    var scope: Scope? = null
    var objectName: Formula? = null
    var sceneName: Formula? = null  // null = Current scene
    var persist: Boolean = false

    private var executed = false

    override fun update(percent: Float) {
        if (executed) return
        executed = true

        val project = scope?.project ?: return
        val name = objectName?.interpretString(scope) ?: return
        if (name.isBlank()) return

        // Resolve scene
        val sceneStr = sceneName?.interpretString(scope)
        val scene = resolveScene(project, sceneStr)
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

        // Track mutation so the editor can reload on Stage exit
        if (persist) {
            RuntimeMutationTracker.hasPersistentMutations = true
            try {
                ProjectSaver(project, CatroidApplication.getAppContext()).saveProjectAsync(onSaveProjectComplete = { success ->
                    if (!success) Log.e(TAG, "Failed to persist project after creating object '$name'")
                })
            } catch (e: Exception) {
                Log.e(TAG, "Could not persist newly created object '$name'", e)
            }
        } else {
            RuntimeMutationTracker.hasTemporaryMutations = true
        }

        // If this scene is currently active on stage, initialize the sprite
        val stageListener = StageActivity.getActiveStageListener()
        if (stageListener != null) {
            val activeScene = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (activeScene != null && activeScene.getSceneId() == scene.getSceneId()) {
                newSprite.initializeEventThreads(EventId.START)
                newSprite.initConditionScriptTriggers()
            }
        }
    }

    private fun resolveScene(project: org.catrobat.catroid.content.Project, sceneStr: String?): Scene? {
        if (sceneStr.isNullOrEmpty()) {
            val current = ProjectManager.getInstance().getCurrentlyPlayingScene()
            if (current != null) return current
            return project.getDefaultScene()
        }
        return project.getSceneByName(sceneStr)
    }

    companion object {
        private const val TAG = "CreateObjectAction"
    }
}
