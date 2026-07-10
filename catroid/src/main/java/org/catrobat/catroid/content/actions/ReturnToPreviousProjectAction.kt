package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.ProjectManager
import android.content.Intent
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

        val intent = Intent(stage, StageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        }
        stage.startActivity(intent)
        stage.finish()
        return true
    }

    override fun restart() {
        started = false
        super.restart()
    }
}
