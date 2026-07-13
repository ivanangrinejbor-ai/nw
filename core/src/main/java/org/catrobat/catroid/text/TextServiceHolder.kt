package org.catrobat.catroid.text

/**
 * Global injection point for the active [TextService] implementation.
 *
 * Initialized once in [org.catrobat.catroid.stage.StageActivity.onCreate] with the
 * Android-backed implementation. The desktop runtime installs its own.
 */
object TextServiceHolder {
    lateinit var textService: TextService
}
