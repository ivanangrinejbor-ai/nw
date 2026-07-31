package org.catrobat.catroid.ui

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.catrobat.catroid.R
import java.util.Random

class ProjectReloadOverlay private constructor(private val activity: Activity) {

    private var overlayView: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var facts: Array<String> = emptyArray()
    private val random = Random()
    private var factTextView: TextView? = null

    private val rotateFactRunnable = object : Runnable {
        override fun run() {
            showNextFact()
            handler.postDelayed(this, 5000)
        }
    }

    init {
        val res = activity.resources
        facts = try {
            res.getStringArray(R.array.project_reload_facts)
        } catch (e: Exception) {
            emptyArray()
        }
        setupViews()
    }

    private fun setupViews() {
        val context = activity
        
        overlayView = FrameLayout(context).apply {
            setBackgroundColor(0xDD000000.toInt())
            isClickable = true
            isFocusable = true
            id = View.generateViewId()
        }

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = dpToPx(context, 24f)
            setPadding(padding, padding, padding, padding)
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
        }
        val progressParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 24f)
        }
        contentLayout.addView(progressBar, progressParams)

        val statusTextView = TextView(context).apply {
            text = context.getString(R.string.project_reload_status)
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
        }
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 16f)
        }
        contentLayout.addView(statusTextView, statusParams)

        factTextView = TextView(context).apply {
            setTextColor(0xFFCCCCCC.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
        }
        val factParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        contentLayout.addView(factTextView, factParams)

        val contentParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        overlayView?.addView(contentLayout, contentParams)
    }

    private fun showNextFact() {
        if (facts.isNotEmpty()) {
            val fact = facts[random.nextInt(facts.size)]
            factTextView?.text = fact
        }
    }

    fun show() {
        val decorView = activity.window.decorView as? ViewGroup ?: return

        hideInternal()

        decorView.addView(overlayView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        showNextFact()
        handler.postDelayed(rotateFactRunnable, 5000)
    }

    internal fun hideInternal() {
        handler.removeCallbacks(rotateFactRunnable)
        val decorView = activity.window.decorView as? ViewGroup ?: return
        overlayView?.let {
            decorView.removeView(it)
        }
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    companion object {
        private var currentOverlay: ProjectReloadOverlay? = null

        @JvmStatic
        fun show(activity: Activity) {
            activity.runOnUiThread {
                try {
                    val overlay = ProjectReloadOverlay(activity)
                    currentOverlay = overlay
                    overlay.show()
                } catch (e: Exception) {
                }
            }
        }

        @JvmStatic
        fun hide() {
            currentOverlay?.let {
                it.activity.runOnUiThread {
                    try {
                        it.hideInternal()
                    } catch (e: Exception) {
                    }
                }
                currentOverlay = null
            }
        }
    }
}
