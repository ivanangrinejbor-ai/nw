package com.google.mlkit.vision.objects

import com.google.mlkit.vision.objectdetection.ObjectDetector
import com.google.mlkit.vision.objectdetection.ObjectDetectorOptions

class ObjectDetection {
    companion object {
        fun getClient(options: ObjectDetectorOptions): ObjectDetector = ObjectDetector()
    }
}
