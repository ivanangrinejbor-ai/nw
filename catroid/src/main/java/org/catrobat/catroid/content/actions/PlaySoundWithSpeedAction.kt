package org.catrobat.catroid.content.actions

import android.os.Build
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundManager

class PlaySoundWithSpeedAction : TemporalAction() {
    var sprite: Sprite? = null
    var scope: Scope? = null
    var sound: SoundInfo? = null
    var speed: Formula? = null

    override fun update(percent: Float) {
        val s = sound ?: return
        val sp = sprite ?: return
        SoundManager.getInstance().playSoundFile(s.file?.absolutePath ?: return, sp)
        val speedVal = speed?.interpretFloat(scope) ?: 1f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mediaPlayers = SoundManager.getInstance().mediaPlayers
            for (mp in mediaPlayers) {
                try {
                    val params = mp.playbackParams.setSpeed(speedVal.coerceIn(0.25f, 4f))
                    mp.playbackParams = params
                } catch (_: Exception) {}
            }
        }
    }
}
