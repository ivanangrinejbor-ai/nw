package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity
import android.util.Log
import java.io.File

class ReturnToPreviousProjectAction : Action() {
    private var started = false

    override fun act(delta: Float): Boolean {
        if (started) return true
        started = true

        val stage = StageActivity.activeStageActivity?.get()
        if (stage == null) {
            Log.e("ReturnAction", "Stage is null, cannot return to previous project.")
            return true
        }

        val previousProjectPath = ProjectManager.popProjectHistory()
        if (previousProjectPath == null) {
            Log.e("ReturnAction", "No previous project in history.")
            stage.finish()
            return true
        }

        val projectDir = File(previousProjectPath)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            Log.e("ReturnAction", "Previous project directory does not exist: $previousProjectPath")
            stage.finish()
            return true
        }

        try {
            ProjectManager.getInstance().loadProject(projectDir)
        } catch (e: Exception) {
            Log.e("ReturnAction", "Failed to reload previous project", e)
        }

        stage.finish()
        return true
    }

    override fun restart() {
        started = false
        super.restart()
    }
}
