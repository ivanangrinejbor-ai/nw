package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class CloneAndNameAction : TemporalAction() {
    var scope: Scope? = null
    var sprite: Sprite? = null
    var cloneName: Formula? = null

    override fun update(percent: Float) {
        val spriteToClone = sprite ?: return
        val stageActivity = StageActivity.activeStageActivity.get() ?: return
        val name = cloneName?.interpretString(scope)
        
        // Fallback: if name is null or empty, generate a default name
        val safeName = if (name.isNullOrBlank()) {
            "${spriteToClone.name}-clone-${System.currentTimeMillis() % 10000}"
        } else {
            name
        }

        stageActivity.stageListener?.cloneSpriteAndAddToStage(spriteToClone, safeName)
    }
}