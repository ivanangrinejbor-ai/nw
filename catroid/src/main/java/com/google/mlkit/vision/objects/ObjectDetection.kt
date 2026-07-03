package com.google.mlkit.vision.objects

import com.google.mlkit.vision.objectdetection.ObjectDetector

class ObjectDetection {
    companion object {
        // Accept either the legacy `objectdetection.ObjectDetectorOptions` or
        // the newer `objects.defaults.ObjectDetectorOptions` (or any options type)
        fun getClient(options: Any): ObjectDetector = ObjectDetector()
    }
}
