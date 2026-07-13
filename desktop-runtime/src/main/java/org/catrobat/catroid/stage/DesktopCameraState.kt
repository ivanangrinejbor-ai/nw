package org.catrobat.catroid.stage

/**
 * 2D-emulated camera state exposed by the Camera 3D bricks.
 *
 * The desktop player renders with a libGDX [com.badlogic.gdx.graphics.OrthographicCamera].
 * The Catrobat "3D camera" bricks (SetCameraPosition / SetCameraRotation / SetCameraZoom /
 * RotateCameraBy / PinToCamera / AttachToCamera / SetBufferCamera) are mapped onto this
 * orthographic camera as pan (x, y), zoom and rotation of the whole view. Z / pitch / roll
 * are accepted but have no visual effect in the 2D renderer.
 */
class DesktopCameraState {
    /** Camera world X offset (Catrobat X axis). */
    var x: Float = 0f
    /** Camera world Y offset (Catrobat Y axis, inverted on screen). */
    var y: Float = 0f
    /** Zoom factor (1 = default). */
    var zoom: Float = 1f
    /** View rotation in degrees around the Z axis. */
    var rotation: Float = 0f

    /** Objects pinned/attached to the camera, with their world offset captured at attach time. */
    val cameraPinned: MutableMap<String, Pair<Float, Float>> = mutableMapOf()
}
