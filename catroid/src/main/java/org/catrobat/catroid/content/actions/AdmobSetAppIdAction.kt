package org.catrobat.catroid.content.actions

import android.util.Log
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
        Log.w("AdMobManager", "Set App ID brick has no effect at runtime: the Google Mobile Ads SDK " +
            "reads the APPLICATION_ID from AndroidManifest.xml once at app start. " +
            "Set a real App ID in the manifest to show production ads.")
    }
}
