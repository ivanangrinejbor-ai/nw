package org.catrobat.catroid.content

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity

object HttpRequestEventDispatcher {
    fun dispatch(requestId: String, failed: Boolean) {
        val activity = StageActivity.activeStageActivity.get() ?: return
        val listener = StageActivity.getActiveStageListener() ?: return
        val project = ProjectManager.getInstance().currentProject ?: return
        activity.runOnUiThread {
            for (sprite in listener.spritesFromStage) {
                for (script in sprite.scriptList) {
                    val matches = when {
                        failed && script is WhenHttpRequestFailedScript -> script.matches(project, sprite, requestId)
                        !failed && script is WhenHttpResponseReceivedScript -> script.matches(project, sprite, requestId)
                        else -> false
                    }
                    if (matches) {
                        sprite.look.addAction(sprite.createSequenceAction(script))
                    }
                }
            }
        }
    }
}
