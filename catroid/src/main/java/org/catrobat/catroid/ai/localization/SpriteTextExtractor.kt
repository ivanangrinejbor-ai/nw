package org.catrobat.catroid.ai.localization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import java.io.File

object SpriteTextExtractor {

    private val recognizer by lazy { TextRecognition.getClient() }

    fun extractTextRegions(context: Context, imageFile: File): List<TextRegion> {
        return try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return emptyList()
            val regions = mutableListOf<TextRegion>()
            val inputImage = InputImage.fromBitmap(bitmap)

            val task = recognizer.process(inputImage)
            var done = false
            task.addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    for (line in block.getLines()) {
                        val box = line.getBoundingBox() ?: continue
                        val text = line.getText().trim()
                        if (text.isEmpty() || text.length > 200) continue

                        val bgColor = TextRenderer.detectBackgroundColor(bitmap, box)
                        val textColor = TextRenderer.detectTextColor(bitmap, box)
                        val fontSize = TextRenderer.detectFontSize(box)
                        val outlineColor = TextRenderer.detectOutlineColor(bitmap, box, textColor)
                        val outlineWidth = if (outlineColor != 0)
                            TextRenderer.detectOutlineWidth(bitmap, box, textColor) else 0f
                        val rotation = TextRenderer.detectRotationAngle(line, bitmap)

                        regions.add(TextRegion(
                            originalText = text, translatedText = text,
                            boundingBox = box, textColor = textColor,
                            backgroundColor = bgColor, estimatedFontSize = fontSize,
                            confidence = 1.0f, outlineColor = outlineColor,
                            outlineWidth = outlineWidth, rotationAngle = rotation,
                            lineRef = line
                        ))
                    }
                }
                synchronized(this) { done = true; (this as Object).notifyAll() }
            }
            task.addOnFailureListener {
                synchronized(this) { done = true; (this as Object).notifyAll() }
            }

            var waited = 0L
            while (!done && waited < 10_000) {
                synchronized(this) { (this as Object).wait(100) }
                waited += 100
            }

            bitmap.recycle()
            regions
        } catch (e: Exception) {
            emptyList()
        }
    }
}
