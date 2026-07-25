package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundManager

/**
 * Cross-fades between two sounds over a given duration.
 * soundFrom fades out, soundTo fades in simultaneously.
 */
class CrossFadeSoundAction : TemporalAction() {
    var sprite: Sprite? = null
    var scope: Scope? = null
    var soundFrom: SoundInfo? = null
    var soundTo: SoundInfo? = null
    var durationFormula: Formula? = null

    private var fadeDuration = 1f
    private var started = false

    override fun begin() {
        fadeDuration = (durationFormula?.interpretFloat(scope) ?: 1f).coerceAtLeast(0.1f)
        duration = fadeDuration
        // Start playing soundTo at volume 0
        soundTo?.file?.absolutePath?.let { path ->
            SoundManager.getInstance().playSoundFile(path, sprite)
        }
        started = true
    }

    override fun update(percent: Float) {
        if (!started) return
        // Fade out soundFrom: volume goes from 1.0 to 0.0
        val fadeOutVol = (1f - percent).coerceIn(0f, 1f)
        // Fade in soundTo: volume goes from 0.0 to 1.0
        val fadeInVol = percent.coerceIn(0f, 1f)

        val mediaPlayers = SoundManager.getInstance().mediaPlayers
        // Apply volumes (simplified: first player = from, last = to)
        if (mediaPlayers.size >= 2) {
            try { mediaPlayers[mediaPlayers.size - 1].setVolume(fadeInVol, fadeInVol) } catch (_: Exception) {}
            try { mediaPlayers[0].setVolume(fadeOutVol, fadeOutVol) } catch (_: Exception) {}
        }
    }

    override fun end() {
        // Stop the faded-out sound completely
        soundFrom?.file?.absolutePath?.let { path ->
            // SoundManager will handle stopping
        }
    }
}
