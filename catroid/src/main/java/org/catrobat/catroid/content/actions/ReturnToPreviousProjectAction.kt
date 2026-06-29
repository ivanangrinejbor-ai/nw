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

        val previousProjectPath = ProjectManager.popProjectHistory()
        if (previousProjectPath != null) {
            try {
                ProjectManager.getInstance().loadProject(File(previousProjectPath))
            } catch (e: Exception) {
                Log.e("ReturnAction", "Failed to reload previous project", e)
            }
        }

        stage?.finish()
        return true
    }

    override fun restart() {
        started = false
        super.restart()
    }
}
