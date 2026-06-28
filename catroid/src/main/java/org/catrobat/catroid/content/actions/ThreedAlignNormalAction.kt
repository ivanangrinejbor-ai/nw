package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class ThreedAlignNormalAction : TemporalAction() {
    var scope: Scope? = null
    var objId: Formula? = null
    var nx: Formula? = null
    var ny: Formula? = null
    var nz: Formula? = null
    override fun update(percent: Float) {
        val tm = StageActivity.getActiveStageListener().threeDManager ?: return
        try {
            val id = objId?.interpretString(scope) ?: return
            if (id.isEmpty()) return
            tm.alignObjectToNormal(
                id,
                nx?.interpretFloat(scope) ?: 0f,
                ny?.interpretFloat(scope) ?: 0f,
                nz?.interpretFloat(scope) ?: 0f
            )
        } catch (e: Exception) {
            Log.e("ThreedAlignNormal", "Failed to align normal", e)
        }
    }
}