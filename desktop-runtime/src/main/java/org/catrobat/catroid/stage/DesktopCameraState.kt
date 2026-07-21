package org.catrobat.catroid.stage

class DesktopCameraState {
    var x: Float = 0f
    var y: Float = 0f
    var zoom: Float = 1f
    var rotation: Float = 0f

    var followTargetName: String? = null
    var followOffsetX: Float = 0f
    var followOffsetY: Float = 0f
    var followDistance: Float = 0f
    var followHeight: Float = 0f
    var followPitch: Float = 0f

    var fieldOfView: Float = 0f
    var shakeIntensity: Float = 0f
    var shakeDuration: Float = 0f
    var rangeNear: Float = 0f
    var rangeFar: Float = 0f
    var touchControlEnabled: Boolean = false

    val cameraPinned: MutableMap<String, Pair<Float, Float>> = mutableMapOf()
}
