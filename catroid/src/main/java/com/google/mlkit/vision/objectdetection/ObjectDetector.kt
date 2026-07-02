package com.google.mlkit.vision.objectdetection

class ObjectDetector {
    companion object {
        fun getClient(options: ObjectDetectorOptions): ObjectDetector = ObjectDetector()
    }

    fun process(image: com.google.mlkit.vision.common.InputImage) = com.google.android.gms.tasks.Tasks.forResult(emptyList<DetectedObject>())
    fun close() {}
}