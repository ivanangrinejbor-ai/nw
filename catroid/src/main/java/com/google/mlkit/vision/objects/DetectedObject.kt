package com.google.mlkit.vision.objects

class DetectedObject {
    companion object {
        const val CATEGORY_UNKNOWN = -1
    }

    fun getTrackingId(): Int = 0
    fun getBoundingBox(): android.graphics.Rect = android.graphics.Rect()
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