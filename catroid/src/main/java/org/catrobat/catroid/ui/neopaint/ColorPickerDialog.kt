package org.catrobat.catroid.ui.neopaint

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class ColorPickerDialog(
    context: Context,
    initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : Dialog(context) {

    private var hue: Float = 0f
    private var sat: Float = 0f
    private var value: Float = 0f
    private lateinit var preview: View
    private lateinit var svPicker: SVPicker

    init {
        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        setContentView(buildLayout())
    }

    private fun currentColor(): Int {
        return Color.HSVToColor(floatArrayOf(hue, sat, value))
    }

    private fun buildLayout(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = android.view.Gravity.CENTER
        }

        root.addView(TextView(context).apply {
            text = "Select Color"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        })

        svPicker = SVPicker(context) { s, v ->
            sat = s
            value = v
            updatePreview()
        }
        svPicker.setColor(hue, sat, value)
        root.addView(svPicker)

        root.addView(SeekBar(context).apply {
            max = 360
            progress = hue.toInt()
            setPadding(0, 24, 0, 24)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    hue = p.toFloat()
                    svPicker.setHue(hue)
                    updatePreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })

        preview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { topMargin = 16 }
            setBackgroundColor(currentColor())
        }
        root.addView(preview)

        root.addView(Button(context).apply {
            text = "OK"
            setOnClickListener {
                onColorSelected(currentColor())
                dismiss()
            }
        })
        return root
    }

    private fun updatePreview() {
        preview.setBackgroundColor(currentColor())
    }

    private class SVPicker(
        context: Context,
        private val onChange: (Float, Float) -> Unit
    ) : View(context) {

        private var hue: Float = 0f
        private var sat: Float = 0.5f
        private var value: Float = 1f
        private val paint = Paint()

        init {
            layoutParams = LinearLayout.LayoutParams(400, 400)
        }

        fun setColor(h: Float, s: Float, v: Float) {
            hue = h
            sat = s
            value = v
            invalidate()
        }

        fun setHue(h: Float) {
            hue = h
            invalidate()
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val base = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            paint.color = base
            canvas.drawRect(0f, 0f, w, h, paint)
            val whiteGrad = android.graphics.LinearGradient(
                0f, 0f, w, 0f, 0xffffffff.toInt(), 0x00ffffff, android.graphics.Shader.TileMode.CLAMP)
            paint.shader = whiteGrad
            canvas.drawRect(0f, 0f, w, h, paint)
            val blackGrad = android.graphics.LinearGradient(
                0f, 0f, 0f, h, 0x00000000, 0xff000000.toInt(), android.graphics.Shader.TileMode.CLAMP)
            paint.shader = blackGrad
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            paint.color = Color.WHITE
            canvas.drawCircle(sat * w, (1f - value) * h, 8f, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                sat = (event.x / width.toFloat()).coerceIn(0f, 1f)
                value = (1f - event.y / height.toFloat()).coerceIn(0f, 1f)
                invalidate()
                onChange(sat, value)
                return true
            }
            return super.onTouchEvent(event)
        }
    }
}
