package org.catrobat.catroid.ui.neopaint

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import org.catrobat.catroid.R

class ColorPickerDialog(
    context: Context,
    initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : Dialog(context) {

    private var hue: Float = 0f
    private var sat: Float = 0f
    private var value: Float = 0f
    private var currentAlpha: Int = 255
    private lateinit var preview: View
    private lateinit var svPicker: SVPicker
    private lateinit var lblAlpha: TextView
    private lateinit var seekAlpha: SeekBar

    // Store initial to reset on cancel
    private val initialHsv = FloatArray(3)
    private var initialAlpha: Int = 255

    private val presets = intArrayOf(
        Color.BLACK, Color.WHITE, Color.RED, Color.parseColor("#FF9800"),
        Color.YELLOW, Color.GREEN, Color.parseColor("#00BCD4"), Color.BLUE,
        Color.parseColor("#3F51B5"), Color.parseColor("#9C27B0"), Color.GRAY, Color.parseColor("#795548"),
        Color.parseColor("#F44336"), Color.parseColor("#FFEB3B"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3")
    )

    init {
        Color.colorToHSV(initialColor, initialHsv)
        hue = initialHsv[0]; sat = initialHsv[1]; value = initialHsv[2]
        initialAlpha = Color.alpha(initialColor)
        currentAlpha = initialAlpha
        setContentView(buildLayout())
        setCanceledOnTouchOutside(false)
    }

    private fun currentColor(): Int = Color.HSVToColor(currentAlpha, floatArrayOf(hue, sat, value))

    private val dp: Float get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)

    private fun buildLayout(): View {
        val padPx = (16 * dp).toInt()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padPx, padPx, padPx, padPx)
            gravity = Gravity.CENTER
        }

        root.addView(TextView(context).apply {
            text = context.getString(R.string.neopaint_color_picker_title)
            textSize = 20f
            setPadding(0, 0, 0, (12 * dp).toInt())
        })

        svPicker = SVPicker(context) { s, v ->
            sat = s; value = v; updatePreview()
        }
        svPicker.setColor(hue, sat, value)
        root.addView(svPicker)

        // Hue slider
        root.addView(SeekBar(context).apply {
            max = 360
            progress = hue.toInt()
            setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    hue = p.toFloat(); svPicker.setHue(hue); updatePreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })

        // Alpha slider
        val currentAlphaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * dp).toInt(), 0, 0)
        }
        currentAlphaRow.addView(TextView(context).apply {
            text = "Alpha"
            textSize = 12f
            setPadding(0, 0, (8 * dp).toInt(), 0)
        })
        lblAlpha = TextView(context).apply {
            text = "255"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins((8 * dp).toInt(), 0, 0, 0) }
        }
        seekAlpha = SeekBar(context).apply {
            max = 255
            progress = currentAlpha
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    currentAlpha = p; lblAlpha.text = "$p"; updatePreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        currentAlphaRow.addView(seekAlpha)
        currentAlphaRow.addView(lblAlpha)
        root.addView(currentAlphaRow)

        // Presets row
        val presetRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * dp).toInt(), 0, (4 * dp).toInt())
        }
        for (c in presets) {
            presetRow.addView(Button(context).apply {
                setBackgroundColor(c)
                val btnSize = (36 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                }
                setOnClickListener {
                    val hsv = FloatArray(3)
                    Color.colorToHSV(c, hsv)
                    hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                    currentAlpha = 255
                    lblAlpha.text = "255"
                    seekAlpha.progress = 255
                    svPicker.setColor(hue, sat, value)
                    updatePreview()
                }
            })
        }
        val scroll = HorizontalScrollView(context).apply { addView(presetRow) }
        root.addView(scroll)

        // Preview with checkerboard
        preview = object : View(context) {
            private val checkerPaint = Paint()
            private val checkerBmp: Bitmap
            init {
                val cs = (8 * dp).toInt()
                checkerBmp = Bitmap.createBitmap(cs * 2, cs * 2, Bitmap.Config.ARGB_8888)
                val cc = Canvas(checkerBmp)
                cc.drawColor(Color.WHITE)
                val gray = Paint().apply { color = Color.LTGRAY }
                cc.drawRect(0f, 0f, cs.toFloat(), cs.toFloat(), gray)
                cc.drawRect(cs.toFloat(), cs.toFloat(), (cs * 2).toFloat(), (cs * 2).toFloat(), gray)
                checkerPaint.shader = BitmapShader(checkerBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
            override fun onDraw(c: Canvas) {
                super.onDraw(c)
                if (width <= 0 || height <= 0) return
                // Draw checkerboard
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), checkerPaint)
                // Draw color
                c.drawColor(currentColor())
            }
        }.apply {
            val previewSize = (56 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(previewSize, previewSize).apply {
                topMargin = (8 * dp).toInt()
                gravity = Gravity.CENTER
            }
        }
        root.addView(preview)

        // Buttons row
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, (12 * dp).toInt(), 0, 0)
        }
        btnRow.addView(Button(context).apply {
            text = "Cancel"
            setOnClickListener {
                // Reset to initial color
                hue = initialHsv[0]; sat = initialHsv[1]; value = initialHsv[2]; currentAlpha = initialAlpha
                dismiss()
            }
        })
        btnRow.addView(Button(context).apply {
            text = context.getString(R.string.neopaint_text_ok)
            setOnClickListener {
                onColorSelected(currentColor())
                dismiss()
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins((16 * dp).toInt(), 0, 0, 0) }
        })
        root.addView(btnRow)

        return root
    }

    private fun updatePreview() {
        preview.invalidate()
    }

    // ── SV Picker ────────────────────────────────────────

    private class SVPicker(
        context: Context,
        private val onChange: (Float, Float) -> Unit
    ) : View(context) {

        private var hue: Float = 0f
        private var sat: Float = 0.5f
        private var value: Float = 1f
        private val paint = Paint()
        private var cachedHueForShader: Float = -1f
        private var saturationShader: Shader? = null
        private var valueShader: Shader? = null

        init {
            val dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)
            val sizeDp = (250 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sizeDp, sizeDp)
        }

        private fun ensureShaders(w: Float, h: Float) {
            if (hue != cachedHueForShader || saturationShader == null) {
                val fullColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                saturationShader = LinearGradient(0f, 0f, w, 0f,
                    0xffffffff.toInt(), fullColor and 0x00ffffff or 0xff000000.toInt(),
                    Shader.TileMode.CLAMP)
                valueShader = LinearGradient(0f, 0f, 0f, h,
                    0x00000000, 0xff000000.toInt(), Shader.TileMode.CLAMP)
                cachedHueForShader = hue
            }
        }

        fun setColor(h: Float, s: Float, v: Float) {
            hue = h; sat = s; value = v; cachedHueForShader = -1f; invalidate()
        }

        fun setHue(h: Float) {
            hue = h; cachedHueForShader = -1f; invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0 || h <= 0) return
            ensureShaders(w, h)
            paint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = saturationShader; canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = valueShader; canvas.drawRect(0f, 0f, w, h, paint)
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
