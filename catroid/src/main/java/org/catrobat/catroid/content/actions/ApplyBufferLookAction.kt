package org.catrobat.catroid.content.actions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.DynamicLookData
import org.catrobat.catroid.content.RenderTextureManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class ApplyBufferLookAction : TemporalAction() {
    var scope: Scope? = null
    var nameFormula: Formula? = null

    override fun update(percent: Float) {
        val name = nameFormula?.interpretString(scope) ?: return
        val sprite = scope?.sprite ?: return

        val currentLookData = sprite.look.lookData
        if (currentLookData is DynamicLookData && currentLookData.bufferName == name) {
            return
        }

        val target = RenderTextureManager.renderTextures[name] ?: return
        val region = target.textureRegion
        val w = target.width
        val h = target.height

        Gdx.app.postRunnable {
            val dynamicData = DynamicLookData(name, region, w, h)
            sprite.look.setLookData(dynamicData)
        }
    }
}