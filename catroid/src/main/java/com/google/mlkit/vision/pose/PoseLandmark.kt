package com.google.mlkit.vision.pose

class PoseLandmark {
    companion object {
        const val TYPE_NOSE = 0
        const val TYPE_LEFT_EYE_INNER = 1
        const val TYPE_LEFT_EYE = 2
        const val TYPE_LEFT_EYE_OUTER = 3
        const val TYPE_RIGHT_EYE_INNER = 4
        const val TYPE_RIGHT_EYE = 5
        const val TYPE_RIGHT_EYE_OUTER = 6
        const val TYPE_LEFT_EAR = 7
        const val TYPE_RIGHT_EAR = 8
        const val TYPE_LEFT_MOUTH = 9
        const val TYPE_RIGHT_MOUTH = 10
        const val TYPE_LEFT_SHOULDER = 11
        const val TYPE_RIGHT_SHOULDER = 12
        const val TYPE_LEFT_ELBOW = 13
        const val TYPE_RIGHT_ELBOW = 14
        const val TYPE_LEFT_WRIST = 15
        const val TYPE_RIGHT_WRIST = 16
        const val TYPE_LEFT_PINKY = 17
        const val TYPE_RIGHT_PINKY = 18
        const val TYPE_LEFT_INDEX = 19
        const val TYPE_RIGHT_INDEX = 20
        const val TYPE_LEFT_THUMB = 21
        const val TYPE_RIGHT_THUMB = 22
        const val TYPE_LEFT_HIP = 23
        const val TYPE_RIGHT_HIP = 24
        const val TYPE_LEFT_KNEE = 25
        const val TYPE_RIGHT_KNEE = 26
        const val TYPE_LEFT_ANKLE = 27
        const val TYPE_RIGHT_ANKLE = 28
        const val TYPE_LEFT_HEEL = 29
        const val TYPE_RIGHT_HEEL = 30
        const val TYPE_LEFT_FOOT_INDEX = 31
        const val TYPE_RIGHT_FOOT_INDEX = 32
    }

    fun getPosition(): android.graphics.PointF = position
    fun getInFrameLikelihood(): Float = inFrameLikelihood
    fun getLandmarkType(): Int = type

    val type: Int = 0
    val position: android.graphics.PointF = android.graphics.PointF()
    val inFrameLikelihood: Float = 0f
}
