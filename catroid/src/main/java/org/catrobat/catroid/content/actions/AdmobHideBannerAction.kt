package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.admob.AdMobManager
import org.catrobat.catroid.content.Scope

class AdmobHideBannerAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        if (scope == null) return
        AdMobManager.hideBanner()
    }
}
