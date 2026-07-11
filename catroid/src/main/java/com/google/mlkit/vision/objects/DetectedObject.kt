package com.google.mlkit.vision.objects

import android.graphics.Rect

class DetectedObject {
    val trackingId: Int = 0
    val boundingBox: Rect = Rect()

    constructor()

    constructor(boundingBox: Rect, trackingId: Int, labels: List<Label>) {
        this.labelsInternal = labels
    }

    private var labelsInternal: List<Label> = emptyList()

    fun getCategory(): Category = Category()
    fun getLabels(): List<Label> = labelsInternal

    class Category {
        companion object {
            const val CATEGORY_UNKNOWN = -1
        }
        fun getText(): String = ""
        fun getConfidence(): Float = 0f
    }

    class Label {
        companion object {
            const val CATEGORY_UNKNOWN = -1
        }
        constructor()
        constructor(text: String, confidence: Float, index: Int) {
            this.textInternal = text
            this.confidenceInternal = confidence
        }

        private var textInternal: String = ""
        private var confidenceInternal: Float = 0f

        fun getText(): String = textInternal
        fun getConfidence(): Float = confidenceInternal
    }
}
