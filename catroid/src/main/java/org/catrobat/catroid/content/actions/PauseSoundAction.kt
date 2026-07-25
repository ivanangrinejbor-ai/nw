package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.SoundManager

class PauseSoundAction : TemporalAction() {
    var sprite: Sprite? = null
    var sound: SoundInfo? = null

    override fun update(percent: Float) {
        val s = sound ?: return
        val sp = sprite ?: return
        val mediaPlayers = SoundManager.getInstance().mediaPlayers
        for (mp in mediaPlayers) {
            if (mp.isPlaying) {
                // SoundManager maps sounds by file path
                try { mp.pause() } catch (_: Exception) {}
            }
        }
    }
}
