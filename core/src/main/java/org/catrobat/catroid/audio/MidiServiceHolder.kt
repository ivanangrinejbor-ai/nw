package org.catrobat.catroid.audio

/**
 * Global injection point for the active [MidiService] implementation.
 *
 * Initialized once in [org.catrobat.catroid.stage.StageActivity.onCreate] with the
 * Android-backed implementation. The desktop runtime installs its own.
 */
object MidiServiceHolder {
    lateinit var midiService: MidiService
}
