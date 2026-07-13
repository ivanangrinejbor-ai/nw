package org.catrobat.catroid.audio

/**
 * Global injection point for the active [AudioService] implementation.
 *
 * Initialized once in [org.catrobat.catroid.stage.StageActivity.onCreate] with the
 * Android-backed implementation. The desktop runtime will install its own.
 */
object AudioServiceHolder {
    lateinit var audioService: AudioService
}
