package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.admob.AdMobManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class AdmobSetRewardedUnitIdAction : TemporalAction() {
    var scope: Scope? = null
    var unitId: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val id = unitId?.interpretString(scope) ?: return
        AdMobManager.rewardedUnitId = id
    }
}
