package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.SoundManager

class SetSoundLoopAction : TemporalAction() {
    var sprite: Sprite? = null
    var sound: SoundInfo? = null
    var loop: Boolean = true

    override fun update(percent: Float) {
        val s = sound ?: return
        val mediaPlayers = SoundManager.getInstance().mediaPlayers
        for (mp in mediaPlayers) {
            try { mp.isLooping = loop } catch (_: Exception) {}
        }
    }
}
