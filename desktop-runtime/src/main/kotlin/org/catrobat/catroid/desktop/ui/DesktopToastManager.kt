package org.catrobat.catroid.desktop.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import java.util.concurrent.CopyOnWriteArrayList

data class ToastMessage(
    val text: String,
    var remainingSeconds: Float = 3.0f,
    val maxSeconds: Float = 3.0f
)

object DesktopToastManager {
    private val activeToasts = CopyOnWriteArrayList<ToastMessage>()

    fun showToast(message: String, durationSeconds: Float = 3.0f) {
        if (message.isBlank()) return
        activeToasts.add(ToastMessage(message, durationSeconds, durationSeconds))
    }

    fun render(batch: SpriteBatch, font: BitmapFont, shapeRenderer: ShapeRenderer, screenWidth: Float, screenHeight: Float, delta: Float) {
        if (activeToasts.isEmpty()) return

        val iterator = activeToasts.iterator()
        var yOffset = 80f

        while (iterator.hasNext()) {
            val toast = iterator.next()
            toast.remainingSeconds -= delta
            if (toast.remainingSeconds <= 0f) {
                activeToasts.remove(toast)
                continue
            }

            val alpha = (toast.remainingSeconds / 0.5f).coerceIn(0f, 1f)
            val padding = 20f
            val textWidth = toast.text.length * 10f
            val textHeight = 30f
            val boxWidth = textWidth + padding * 2
            val boxHeight = textHeight + padding
            val boxX = (screenWidth - boxWidth) / 2f
            val boxY = yOffset

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapeRenderer.color = Color(0f, 0f, 0f, 0.75f * alpha)
            shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight)
            shapeRenderer.end()

            batch.begin()
            font.color = Color(1f, 1f, 1f, alpha)
            font.draw(batch, toast.text, boxX + padding, boxY + boxHeight - padding / 2)
            batch.end()

            yOffset += boxHeight + 10f
        }
    }
}
