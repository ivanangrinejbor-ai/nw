package com.google.mlkit.vision.pose

class Pose {
    val allPoseLandmarks: List<PoseLandmark> = emptyList()

    class Builder {
        fun build(): Pose = Pose()
    }
}
