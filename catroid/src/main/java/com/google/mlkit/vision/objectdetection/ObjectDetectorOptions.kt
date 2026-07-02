package com.google.mlkit.vision.objectdetection

class ObjectDetectorOptions {
    companion object {
        fun defaultOptions(): ObjectDetectorOptions = ObjectDetectorOptions()
        fun builder(): Builder = Builder()
    }

    class Builder {
        fun build(): ObjectDetectorOptions = ObjectDetectorOptions()
    }
}