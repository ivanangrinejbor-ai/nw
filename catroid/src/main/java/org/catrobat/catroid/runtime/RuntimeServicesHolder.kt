/*
 * NeoCatroid — global accessor for the active RuntimeServices implementation.
 *
 * Acts as a tiny dependency-injection point. The Android app installs
 * `AndroidRuntimeServices` at startup; the desktop player installs its own
 * implementation. Brick/action code should call through this holder rather
 * than touching platform APIs directly.
 */

package org.catrobat.catroid.runtime

object RuntimeServicesHolder {
    lateinit var services: RuntimeServices
}
