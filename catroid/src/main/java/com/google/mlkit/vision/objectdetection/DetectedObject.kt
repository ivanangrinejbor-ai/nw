package com.google.mlkit.vision.objectdetection

class DetectedObject {
    companion object {
        const val CATEGORY_UNKNOWN = -1
    }

    val trackingId: Int = 0
    val boundingBox: android.graphics.Rect = android.graphics.Rect()

    fun getCategory(): Category = Category()
    fun getLabels(): List<Label> = emptyList()

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
        fun getText(): String = ""
        fun getConfidence(): Float = 0f
    }
}