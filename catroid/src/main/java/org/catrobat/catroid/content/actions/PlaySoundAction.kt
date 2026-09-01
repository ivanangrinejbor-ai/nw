/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.audio.AudioServiceHolder
import org.catrobat.catroid.audio.MidiServiceHolder
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Sprite
import java.io.File

class PlaySoundAction : TemporalAction() {
    lateinit var sprite: Sprite
    var sound: SoundInfo? = null

    override fun update(percent: Float) {
        if (!::sprite.isInitialized) return
        val currentSound = sound ?: return
        var soundToPlay = if (sprite.soundList.contains(currentSound)) currentSound else {
            sprite.soundList.firstOrNull {
                it.name == currentSound.name || it.fileName == currentSound.fileName || (it.soundId != null && it.soundId == currentSound.soundId)
            } ?: currentSound
        }
        var soundFile: File? = soundToPlay.file
        if (soundFile == null || !soundFile.exists()) {
            val scene = ProjectManager.getInstance().currentlyPlayingScene ?: ProjectManager.getInstance().currentlyEditedScene
            if (scene != null) {
                val soundDir = File(scene.directory, Constants.SOUND_DIRECTORY_NAME)
                val name = soundToPlay.fileName ?: soundToPlay.name
                if (name != null) {
                    val candidate = File(soundDir, name)
                    if (candidate.exists()) {
                        soundFile = candidate
                        soundToPlay.file = candidate
                    }
                }
            }
        }
        if (soundFile != null && soundFile.exists()) {
            if (soundToPlay.isMidiFile) {
                MidiServiceHolder.midiService.playSoundFile(soundFile.absolutePath, sprite.name)
            } else {
                AudioServiceHolder.audioService.playSoundFile(soundFile.absolutePath, sprite.name)
            }
        }
    }
}
