package com.google.mlkit.vision.objects

import com.google.mlkit.vision.objectdetection.ObjectDetector

class ObjectDetection {
    companion object {
        fun getClient(options: com.google.mlkit.vision.objectdetection.defaults.ObjectDetectorOptions): ObjectDetector = ObjectDetector()
    }
}