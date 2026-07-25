package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GlobalManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundManager

/**
 * Sets the master game volume. When gameVolume is set (non-null),
 * all other volume-changing actions are ignored — this one overrides.
 */
class SetGameVolumeAction : TemporalAction() {
    var scope: Scope? = null
    var volume: Formula? = null

    override fun update(percent: Float) {
        val vol = volume?.interpretFloat(scope)?.coerceIn(0f, 100f)?.toInt() ?: 100
        GlobalManager.gameVolume = vol
        // Apply master volume directly to SoundManager
        SoundManager.getInstance().setVolume(vol / 100f)
    }
}
