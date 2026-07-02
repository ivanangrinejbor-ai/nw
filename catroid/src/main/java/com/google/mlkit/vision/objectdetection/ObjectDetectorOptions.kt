package com.google.mlkit.vision.objectdetection

class ObjectDetectorOptions {
    companion object {
        const val CLASSIFICATION_MODE_NONE = 1
    }

    class Builder {
        fun setClassificationMode(mode: Int): Builder = this
        fun enableMultipleObjects(): Builder = this
        fun enableClassification(): Builder = this
        fun build(): ObjectDetectorOptions = ObjectDetectorOptions()
    }
}