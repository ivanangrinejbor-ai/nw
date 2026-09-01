package org.catrobat.catroid.audio

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager
import org.catrobat.catroid.pocketmusic.note.Drum
import org.catrobat.catroid.pocketmusic.note.MusicalInstrument

class AndroidMidiService : MidiService {

    private val midiSoundManager = MidiSoundManager.getInstance()

    override fun playSoundFile(filePath: String, spriteName: String) {
        resolveSprite(spriteName)?.let { midiSoundManager.playSoundFile(filePath, it) }
    }

    override fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int) {
        resolveSprite(spriteName)?.let { midiSoundManager.playSoundFileWithStartTime(filePath, it, startTime) }
    }

    override fun playDrumForBeats(drum: Drum, beats: Float, spriteName: String) {
        resolveSprite(spriteName)?.let { midiSoundManager.playDrumForBeats(drum, beats, it) }
    }

    override fun playNoteForBeats(midiValue: Int, beats: Float) {
        midiSoundManager.playNoteForBeats(midiValue, beats)
    }

    override fun stopSoundInSprite(filePath: String, spriteName: String) {
        resolveSprite(spriteName)?.let { midiSoundManager.stopSameSoundInSprite(filePath, it) }
    }

    override fun setInstrument(instrument: MusicalInstrument) = midiSoundManager.setInstrument(instrument)
    override fun getInstrument(): MusicalInstrument = midiSoundManager.getInstrument()
    override fun setTempo(tempo: Float) = midiSoundManager.setTempo(tempo)
    override fun getTempo(): Float = midiSoundManager.getTempo()
    override fun setVolume(volume: Float) = midiSoundManager.setVolume(volume)
    override fun getVolume(): Float = midiSoundManager.getVolume()
    override fun stopAllSounds() = midiSoundManager.stopAllSounds()
    override fun pause() = midiSoundManager.pause()
    override fun resume() = midiSoundManager.resume()
    override fun getDurationForBeats(beats: Float): Long = midiSoundManager.getDurationForBeats(beats)
    override fun reset() = midiSoundManager.reset()

    private fun resolveSprite(name: String): Sprite? {
        val pm = ProjectManager.getInstance()
        val playingScene = pm.currentlyPlayingScene
        if (playingScene != null) {
            val sprite = playingScene.spriteList?.firstOrNull { it.name == name }
            if (sprite != null) return sprite
        }
        val editedScene = pm.currentlyEditedScene
        if (editedScene != null) {
            val sprite = editedScene.spriteList?.firstOrNull { it.name == name }
            if (sprite != null) return sprite
        }
        val currentStage = org.catrobat.catroid.stage.StageActivity.activeStageActivity?.get()
        if (currentStage != null && currentStage.stageListener != null) {
            val stageSprites = currentStage.stageListener.spritesFromStage
            if (stageSprites != null) {
                val sprite = stageSprites.firstOrNull { it.name == name }
                if (sprite != null) return sprite
            }
        }
        val project = pm.currentProject ?: return null
        return project.getSpriteListWithClones().firstOrNull { it.name == name }
    }
}
