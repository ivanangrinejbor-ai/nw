package org.catrobat.catroid.audio

import org.catrobat.catroid.pocketmusic.note.Drum
import org.catrobat.catroid.pocketmusic.note.MusicalInstrument

/**
 * Platform-independent MIDI surface used by note/drum/instrument/tempo bricks.
 *
 * Mirrors the primitives of the Android
 * [org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager] so the desktop
 * player can supply an alternative implementation (e.g. a software synth) without
 * touching the action classes.
 */
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
