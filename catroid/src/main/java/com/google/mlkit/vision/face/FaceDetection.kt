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
}

class FaceDetectorOptions {
    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        fun enableTracking(): Builder = this
        fun enableClassification(): Builder = this
        fun enableContours(): Builder = this
        fun build(): FaceDetectorOptions = FaceDetectorOptions()
    }
}