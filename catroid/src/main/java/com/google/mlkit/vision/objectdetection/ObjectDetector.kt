package com.google.mlkit.vision.objectdetection

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage

class ObjectDetector {
    fun process(image: InputImage): Task<List<DetectedObject>> = com.google.android.gms.tasks.Tasks.forResult(emptyList())
    fun close() {}
}