package com.google.mlkit.vision.objects

class ObjectDetection {
    companion object {
        fun getClient(options: com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions): ObjectDetector = ObjectDetector()
    }
}