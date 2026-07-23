package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ObjectLookAtCorrectAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var targetObjectId: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val id = objectId?.interpretString(scope) ?: return
        val targetId = targetObjectId?.interpretString(scope) ?: return
        if (id.isEmpty() || targetId.isEmpty()) return

        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return
        val sceneManager = stageListener.sceneManager

        val targetPos = Vector3()
        if (sceneManager != null) {
            val targetGo = sceneManager.findGameObject(targetId)
            if (targetGo != null) {
                targetGo.transform.worldTransform.getTranslation(targetPos)
            } else {
                Log.w("ObjectLookAtCorrect", "Target GameObject not found: $targetId")
                return
            }
        } else {
            Log.w("ObjectLookAtCorrect", "SceneManager not available")
            return
        }

        threeDManager.objectLookAt(id, targetPos.x, targetPos.y, targetPos.z)
    }
}
