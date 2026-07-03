package com.google.mlkit.vision.text

class TextRecognition {
    companion object {
        fun getClient(options: Options): TextRecognizer = TextRecognizer()
        // Backwards-compatible overload used in code: getClient()
        fun getClient(): TextRecognizer = TextRecognizer()
    }
}

class Options {
    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        fun build(): Options = Options()
    }
}

class TextRecognizer {
    fun process(image: com.google.mlkit.vision.common.InputImage) = com.google.android.gms.tasks.Tasks.forResult(Text())
    fun close() {}
}

class Text(val text: String = "", val textBlocks: List<Text.TextBlock> = emptyList()) {
    class TextBlock(val text: String = "", val boundingBox: android.graphics.Rect = android.graphics.Rect()) {
        fun getLines(): List<Line> = emptyList()
    }
}

class Line {
    fun getText(): String = ""
    fun getBoundingBox(): android.graphics.Rect = android.graphics.Rect()
    fun getElements(): List<Element> = emptyList()
}

class Element {
    fun getText(): String = ""
    fun getBoundingBox(): android.graphics.Rect = android.graphics.Rect()
}