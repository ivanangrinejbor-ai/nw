package com.google.mlkit.vision.face

class FaceDetection {
    companion object {
        fun getClient(options: FaceDetectorOptions): FaceDetector = FaceDetector()
    }
}

class FaceDetector {
    companion object {
        fun getClient(options: FaceDetectorOptions): FaceDetector = FaceDetector()
    }

    fun process(image: com.google.mlkit.vision.common.InputImage) = com.google.android.gms.tasks.Tasks.forResult(emptyList<Face>())
    fun close() {}
}

class FaceDetectorOptions {
    companion object {
        const val CLASSIFICATION_MODE_NONE = 1
        const val CLASSIFICATION_MODE_ALL = 2
        fun builder(): Builder = Builder()
    }

    class Builder {
        fun setClassificationMode(mode: Int): Builder = this
        fun enableTracking(): Builder = this
        fun enableClassification(): Builder = this
        fun enableContours(): Builder = this
        fun build(): FaceDetectorOptions = FaceDetectorOptions()
    }
}
