package org.catrobat.catroid.test.localization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.catrobat.catroid.ai.localization.TextRegion
import org.catrobat.catroid.ai.localization.TextRenderer
import org.junit.Assert.*
import org.junit.Test

class TextRendererTest {

    private fun createTextBitmap(
        width: Int = 200, height: Int = 100,
        bgColor: Int = Color.WHITE,
        text: String = "Hello",
        textColor: Int = Color.BLACK,
        textSize: Float = 24f,
        textX: Int = 50, textY: Int = 50,
        hasOutline: Boolean = false,
        outlineColor: Int = Color.BLACK
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; this.textSize = textSize
        }
        if (hasOutline) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = outlineColor
            canvas.drawText(text, textX.toFloat(), textY.toFloat(), paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = textColor
        canvas.drawText(text, textX.toFloat(), textY.toFloat(), paint)
        return bitmap
    }

    @Test
    fun `detectBackgroundColor returns dominant edge color`() {
        val bitmap = createTextBitmap(bgColor = Color.rgb(200, 200, 255))
        val box = Rect(10, 10, 190, 90)
        val bg = TextRenderer.detectBackgroundColor(bitmap, box)
        val b = Color.blue(bg)
        assertTrue("Blue should be ~255, was $b", b in 240..255)
    }

    @Test
    fun `detectTextColor finds foreground text color`() {
        val bitmap = createTextBitmap(textColor = Color.rgb(255, 0, 0))
        val box = Rect(30, 20, 170, 80)
        val tc = TextRenderer.detectTextColor(bitmap, box)
        val r = Color.red(tc)
        assertTrue("Red should be >200, was $r", r > 200)
    }

    @Test
    fun `detectOutlineColor finds outline around text`() {
        val bitmap = createTextBitmap(
            textColor = Color.WHITE, hasOutline = true, outlineColor = Color.BLACK
        )
        val box = Rect(30, 20, 170, 80)
        val outline = TextRenderer.detectOutlineColor(bitmap, box, Color.WHITE)
        assertTrue("Outline should be dark", Color.red(outline) < 80)
    }

    @Test
    fun `detectFontSize returns height-based estimate`() {
        val box = Rect(10, 10, 100, 60)
        val fs = TextRenderer.detectFontSize(box)
        assertEquals(31f, fs, 6f)
    }

    @Test
    fun `replaceText handles multi-line text`() {
        val bitmap = createTextBitmap(300, 150, text = "Hello")
        val box = Rect(20, 20, 280, 130)
        val bg = TextRenderer.detectBackgroundColor(bitmap, box)
        val tc = TextRenderer.detectTextColor(bitmap, box)
        val fs = TextRenderer.detectFontSize(box)

        val region = TextRegion("Hello", "Very Long Translated Text Here", box, tc, bg, fs)
        val result = TextRenderer.replaceText(bitmap, region)
        assertNotNull(result)
        assertEquals(bitmap.width, result.width)
        assertEquals(bitmap.height, result.height)
        bitmap.recycle(); result.recycle()
    }

    @Test
    fun `replaceText handles text with outline`() {
        val bitmap = createTextBitmap(300, 100, text = "Hello",
            textColor = Color.WHITE, hasOutline = true, outlineColor = Color.BLACK)
        val box = Rect(30, 20, 270, 80)
        val bg = TextRenderer.detectBackgroundColor(bitmap, box)
        val tc = TextRenderer.detectTextColor(bitmap, box)
        val fs = TextRenderer.detectFontSize(box)
        val oc = TextRenderer.detectOutlineColor(bitmap, box, tc)
        val ow = TextRenderer.detectOutlineWidth(bitmap, box, tc)

        val region = TextRegion("Hello", "Hola", box, tc, bg, fs,
            outlineColor = oc, outlineWidth = ow)
        val result = TextRenderer.replaceText(bitmap, region)
        assertNotNull(result)
        bitmap.recycle(); result.recycle()
    }

    @Test
    fun `replaceText preserves dimensions for rotated text`() {
        val bitmap = createTextBitmap(300, 200)
        val box = Rect(50, 40, 250, 160)
        val region = TextRegion("Test", "Тест", box,
            Color.BLACK, Color.WHITE, 20f, rotationAngle = 15f)
        val result = TextRenderer.replaceText(bitmap, region)
        assertEquals(300, result.width)
        assertEquals(200, result.height)
        bitmap.recycle(); result.recycle()
    }

    @Test
    fun `brightness calculation returns correct ordering`() {
        assertTrue(TextRenderer.brightness(Color.BLACK) < 50)
        assertTrue(TextRenderer.brightness(Color.WHITE) > 200)
    }

    @Test
    fun `detectOutlineWidth returns plausible value for outlined text`() {
        val bitmap = createTextBitmap(300, 100, textColor = Color.WHITE,
            hasOutline = true, outlineColor = Color.BLACK)
        val box = Rect(30, 20, 270, 80)
        val tc = TextRenderer.detectTextColor(bitmap, box)
        val ow = TextRenderer.detectOutlineWidth(bitmap, box, tc)
        assertTrue("Outline width should be >0 for outlined text, was $ow", ow > 0f)
        bitmap.recycle()
    }

    @Test
    fun `detectRotationAngle returns 0 for horizontal text`() {
        val angle = TextRenderer.detectRotationAngle(null, null)
        assertEquals(0f, angle)
    }
}
