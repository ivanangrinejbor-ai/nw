package org.catrobat.catroid.audio

import org.catrobat.catroid.pocketmusic.note.Drum
import org.catrobat.catroid.pocketmusic.note.MusicalInstrument
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiChannel
import javax.sound.midi.Synthesizer

/**
 * Desktop (Windows) implementation of [MidiService] using the JDK MIDI synthesizer
 * (`javax.sound.midi`). Approximate but functional: notes/drums play through the
 * default software synthesizer; instruments map to General MIDI programs.
 */
class DesktopMidiService : MidiService {
    private var synth: Synthesizer? = null
    private var channel: MidiChannel? = null
    private var tempo = 60f
    private var volume = 1.0f
    private var instrument = MusicalInstrument.ACOUSTIC_GRAND_PIANO

    init {
        ensureChannel()
    }

    private fun ensureChannel(): MidiChannel? {
        if (channel == null) {
            try {
                synth = MidiSystem.getSynthesizer().also {
                    it.open()
                    channel = it.channels.firstOrNull()
                }
                channel?.programChange(instrument.ordinal.coerceIn(0, 127))
            } catch (_: Exception) {
                synth = null
                channel = null
            }
        }
        return channel
    }

    override fun playSoundFile(filePath: String, spriteName: String) {
        AudioServiceHolder.audioService?.playSoundFile(filePath, spriteName)
    }

    override fun playSoundFileWithStartTime(filePath: String, spriteName: String, startTime: Int) {
        AudioServiceHolder.audioService?.playSoundFileWithStartTime(filePath, spriteName, startTime)
    }

    override fun playDrumForBeats(drum: Drum, beats: Float, spriteName: String) {
        val ch = ensureChannel() ?: return
        val note = 35 + (drum.ordinal % 47)
        playNote(ch, note, beats)
    }

    override fun playNoteForBeats(midiValue: Int, beats: Float) {
        val ch = ensureChannel() ?: return
        playNote(ch, midiValue.coerceIn(0, 127), beats)
    }

    private fun playNote(ch: MidiChannel, note: Int, beats: Float) {
        val velocity = (volume * 100).toInt().coerceIn(0, 127)
        ch.noteOn(note, velocity)
        // Не блокируем render-поток — пускаем noteOff через таймер
        val durationMs = (beats * 60000f / tempo.coerceAtLeast(1f)).toLong()
        Thread {
            try {
                Thread.sleep(durationMs)
            } catch (_: InterruptedException) {
                // interrupted — note останется звучать
            }
            ch.noteOff(note)
        }.apply { isDaemon = true }.start()
    }

    override fun stopSoundInSprite(filePath: String, spriteName: String) = stopAllSounds()

    override fun setInstrument(instrument: MusicalInstrument) {
        this.instrument = instrument
        ensureChannel()?.programChange(instrument.ordinal.coerceIn(0, 127))
    }

    override fun getInstrument(): MusicalInstrument = instrument

    override fun setTempo(t: Float) {
        tempo = t.coerceAtLeast(1f)
    }

    override fun getTempo(): Float = tempo

    override fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
    }

    override fun getVolume(): Float = volume

    override fun stopAllSounds() {
        ensureChannel()?.allNotesOff()
    }

    override fun pause() {}

    override fun resume() {}

    override fun getDurationForBeats(beats: Float): Long =
        (beats * 60000f / tempo.coerceAtLeast(1f)).toLong()

    override fun reset() = stopAllSounds()
}
