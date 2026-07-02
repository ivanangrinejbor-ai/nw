package com.google.mlkit.vision.pose.defaults

import com.google.mlkit.vision.pose.PoseDetectorOptions

class PoseDetectorOptions {
    companion object {
        const val STREAM_MODE = 1
    }

    class Builder {
        fun setDetectorMode(mode: Int): Builder = this
        fun build(): PoseDetectorOptions = PoseDetectorOptions()
    }
}