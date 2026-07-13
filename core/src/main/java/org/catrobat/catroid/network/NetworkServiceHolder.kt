package org.catrobat.catroid.network

/**
 * Global injection point for the active [NetworkService] implementation.
 *
 * Initialized once in [org.catrobat.catroid.stage.StageActivity.onCreate]
 * (Android) or [org.catrobat.catroid.stage.DesktopStage.main] (desktop).
 */
object NetworkServiceHolder {
    lateinit var service: NetworkService
}
