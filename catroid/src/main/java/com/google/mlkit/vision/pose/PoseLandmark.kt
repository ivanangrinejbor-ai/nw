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

        // Backwards/alternate constant names used in other code (no TYPE_ prefix)
        const val NOSE = TYPE_NOSE
        const val LEFT_EYE_INNER = TYPE_LEFT_EYE_INNER
        const val LEFT_EYE = TYPE_LEFT_EYE
        const val LEFT_EYE_OUTER = TYPE_LEFT_EYE_OUTER
        const val RIGHT_EYE_INNER = TYPE_RIGHT_EYE_INNER
        const val RIGHT_EYE = TYPE_RIGHT_EYE
        const val RIGHT_EYE_OUTER = TYPE_RIGHT_EYE_OUTER
        const val LEFT_EAR = TYPE_LEFT_EAR
        const val RIGHT_EAR = TYPE_RIGHT_EAR
        const val LEFT_MOUTH = TYPE_LEFT_MOUTH
        const val RIGHT_MOUTH = TYPE_RIGHT_MOUTH
        const val LEFT_SHOULDER = TYPE_LEFT_SHOULDER
        const val RIGHT_SHOULDER = TYPE_RIGHT_SHOULDER
        const val LEFT_ELBOW = TYPE_LEFT_ELBOW
        const val RIGHT_ELBOW = TYPE_RIGHT_ELBOW
        const val LEFT_WRIST = TYPE_LEFT_WRIST
        const val RIGHT_WRIST = TYPE_RIGHT_WRIST
        const val LEFT_PINKY = TYPE_LEFT_PINKY
        const val RIGHT_PINKY = TYPE_RIGHT_PINKY
        const val LEFT_INDEX = TYPE_LEFT_INDEX
        const val RIGHT_INDEX = TYPE_RIGHT_INDEX
        const val LEFT_THUMB = TYPE_LEFT_THUMB
        const val RIGHT_THUMB = TYPE_RIGHT_THUMB
        const val LEFT_HIP = TYPE_LEFT_HIP
        const val RIGHT_HIP = TYPE_RIGHT_HIP
        const val LEFT_KNEE = TYPE_LEFT_KNEE
        const val RIGHT_KNEE = TYPE_RIGHT_KNEE
        const val LEFT_ANKLE = TYPE_LEFT_ANKLE
        const val RIGHT_ANKLE = TYPE_RIGHT_ANKLE
        const val LEFT_HEEL = TYPE_LEFT_HEEL
        const val RIGHT_HEEL = TYPE_RIGHT_HEEL
        const val LEFT_FOOT_INDEX = TYPE_LEFT_FOOT_INDEX
        const val RIGHT_FOOT_INDEX = TYPE_RIGHT_FOOT_INDEX
    }

    val type: Int = 0
    val position: android.graphics.PointF = android.graphics.PointF()
    val inFrameLikelihood: Float = 0f

    // Compatibility alias used in Kotlin code: `landmarkType`
    val landmarkType: Int
        get() = type
}
