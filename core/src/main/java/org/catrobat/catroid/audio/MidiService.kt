package org.catrobat.catroid.audio

import org.catrobat.catroid.pocketmusic.note.Drum
import org.catrobat.catroid.pocketmusic.note.MusicalInstrument

interface MidiService {
    fun playSoundFile(filePath: String, spriteName: String)
    fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int)
    fun playDrumForBeats(drum: Drum, beats: Float, spriteName: String)
    fun playNoteForBeats(midiValue: Int, beats: Float)
    fun stopSoundInSprite(filePath: String, spriteName: String)
    fun setInstrument(instrument: MusicalInstrument)
    fun getInstrument(): MusicalInstrument
    fun setTempo(tempo: Float)
    fun getTempo(): Float
    fun setVolume(volume: Float)
    fun getVolume(): Float
    fun stopAllSounds()
    fun pause()
    fun resume()
    fun getDurationForBeats(beats: Float): Long
    fun reset()
}
