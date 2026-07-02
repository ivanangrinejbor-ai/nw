package com.google.mlkit.vision.common

class InputImage {
    companion object {
        fun fromFilePath(context: android.content.Context, path: String): InputImage = InputImage()
        fun fromBitmap(bitmap: android.graphics.Bitmap): InputImage = InputImage()
        fun fromMediaImage(mediaImage: android.media.Image, rotation: Int): InputImage = InputImage()
    }
}