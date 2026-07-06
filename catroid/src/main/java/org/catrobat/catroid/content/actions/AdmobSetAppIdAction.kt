package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.admob.AdMobManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class AdmobSetAppIdAction : TemporalAction() {
    var scope: Scope? = null
    var appId: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val id = appId?.interpretString(scope) ?: return
        AdMobManager.appId = id
    }
}
