package com.google.mlkit.vision.objects.defaults

import com.google.mlkit.vision.objects.ObjectDetectorOptions

class ObjectDetectorOptions {
    companion object {
        fun defaultOptions(): ObjectDetectorOptions = ObjectDetectorOptions()
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var enableMultipleObjects: Boolean = false
        private var enableClassification: Boolean = false

        fun enableMultipleObjects(): Builder {
            this.enableMultipleObjects = true
            return this
        }

        fun enableClassification(): Builder {
            this.enableClassification = true
            return this
        }

        fun build(): ObjectDetectorOptions = ObjectDetectorOptions()
    }
}