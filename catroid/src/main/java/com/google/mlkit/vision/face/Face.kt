package com.google.mlkit.vision.face

class Face {
    companion object {
        const val UNKNOWN_TRACKING_ID = -1
    }

    val trackingId: Int = 0
    val boundingBox: android.graphics.Rect = android.graphics.Rect()

    fun getTrackingId(): Int = trackingId
    fun getBoundingBox(): android.graphics.Rect = boundingBox
    fun getLandmarks(): Map<Landmark, android.graphics.PointF> = emptyMap()
    fun getContours(): Map<Contour, android.graphics.PointF> = emptyMap()
    fun getClassifications(): Map<Classification, Float> = emptyMap()
    fun getYawAngle(): Float = 0f
    fun getRollAngle(): Float = 0f
    fun getPitchAngle(): Float = 0f

    class Landmark {
        companion object {
            const val TYPE_LEFT_EYE = 0
            const val TYPE_RIGHT_EYE = 1
            const val TYPE_NOSE = 2
            const val TYPE_MOUTH = 3
            const val TYPE_LEFT_EAR = 4
            const val TYPE_RIGHT_EAR = 5
            const val TYPE_LEFT_CHEEK = 6
            const val TYPE_RIGHT_CHEEK = 7
        }
    }

    class Contour {
        companion object {
            const val TYPE_ALL = -1
            const val TYPE_FACE = 0
            const val TYPE_LEFT_EYEBROW_TOP = 1
            const val TYPE_LEFT_EYEBROW_BOTTOM = 2
            const val TYPE_RIGHT_EYEBROW_TOP = 3
            const val TYPE_RIGHT_EYEBROW_BOTTOM = 4
            const val TYPE_LEFT_EYE = 5
            const val TYPE_RIGHT_EYE = 6
            const val TYPE_NOSE_BRIDGE = 7
            const val TYPE_NOSE_BOTTOM = 8
            const val TYPE_UPPER_LIP_TOP = 9
            const val TYPE_UPPER_LIP_BOTTOM = 10
            const val TYPE_LOWER_LIP_TOP = 11
            const val TYPE_LOWER_LIP_BOTTOM = 12
        }
    }

    class Classification {
        companion object {
            const val SMILE_PROBABILITY = 0
        }
    }
}