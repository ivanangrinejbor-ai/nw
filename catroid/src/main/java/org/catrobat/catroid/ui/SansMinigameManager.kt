package org.catrobat.catroid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaPlayer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import org.catrobat.catroid.R
import kotlin.math.*

object SansMinigameManager {

    var isPlaying = false
    private var hp = 150
    private val maxHp = 150
    private var isInvulnerable = false
    private var isGameOver = false

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    fun startSans(activity: Activity, originalView: View) {
        if (isPlaying) return
        isPlaying = true
        hp = maxHp
        isInvulnerable = false
        isGameOver = false

        val decorView = activity.window.decorView as ViewGroup
        val overlay = FrameLayout(activity).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
        }
        decorView.addView(overlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        originalView.visibility = View.INVISIBLE

        val mediaPlayer = try {
            MediaPlayer.create(activity, R.raw.megalovania)?.apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) { null }

        overlay.post {
            val w = overlay.width.toFloat()
            val h = overlay.height.toFloat()


            val arenaSize = min(w, h) * 0.42f
            val centerY = h / 2f


            val sansSize = (arenaSize * 0.85f).toInt()
            val sans = ImageView(activity).apply {
                setImageResource(R.drawable.ut_sans)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val sansParams = FrameLayout.LayoutParams(sansSize, sansSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (centerY - arenaSize/2f - sansSize - 40f).toInt().coerceAtLeast(10)
            }
            overlay.addView(sans, sansParams)


            val idleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val v = it.animatedFraction
                    sans.translationX = sin(v * Math.PI.toFloat()) * 20f
                }
            }
            idleAnim.start()


            val arenaX = (w - arenaSize) / 2f
            val arenaY = centerY - arenaSize / 2f

            val arenaBorder = FrameLayout(activity).apply {
                setBackgroundColor(Color.WHITE)
                x = arenaX - 8f; y = arenaY - 8f
            }
            val arenaInner = FrameLayout(activity).apply {
                setBackgroundColor(Color.BLACK)
                x = arenaX; y = arenaY
            }
            overlay.addView(arenaBorder, (arenaSize + 16f).toInt(), (arenaSize + 16f).toInt())
            overlay.addView(arenaInner, arenaSize.toInt(), arenaSize.toInt())


            val uiY = arenaY + arenaSize + 25f


            val nameText = TextView(activity).apply {
                text = "CHARA  LV 19"
                setTextColor(Color.WHITE)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                x = arenaX
                y = uiY
            }
            overlay.addView(nameText, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)


            val hpBarMaxWidth = 180f
            val hpBarBg = View(activity).apply {
                setBackgroundColor(Color.RED)
                x = nameText.x + 130f
                y = uiY
            }
            val hpBarFill = View(activity).apply {
                setBackgroundColor(Color.YELLOW)
                x = hpBarBg.x
                y = uiY
            }
            overlay.addView(hpBarBg, hpBarMaxWidth.toInt(), 25)
            overlay.addView(hpBarFill, hpBarMaxWidth.toInt(), 25)


            val hpNumbers = TextView(activity).apply {
                text = "$hp / $maxHp"
                setTextColor(Color.WHITE)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                x = hpBarBg.x + hpBarMaxWidth + 15f
                y = uiY
            }
            overlay.addView(hpNumbers, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)


            val buttonsImg = ImageView(activity).apply {
                setImageResource(R.drawable.ut_ui_buttons)
                x = arenaX - 10f
                y = uiY + 50f
            }
            overlay.addView(buttonsImg, (arenaSize + 20f).toInt(), 80)


            val heartSize = 35f
            val heart = ImageView(activity).apply {
                setImageResource(R.drawable.ut_heart)
                x = w / 2f - heartSize / 2f
                y = arenaY + arenaSize / 2f - heartSize / 2f
            }
            overlay.addView(heart, heartSize.toInt(), heartSize.toInt())

            heart.setOnTouchListener { v, e ->
                if (!isGameOver && (e.action == MotionEvent.ACTION_MOVE || e.action == MotionEvent.ACTION_DOWN)) {
                    v.x = (e.rawX - heartSize / 2).coerceIn(arenaX, arenaX + arenaSize - heartSize)
                    v.y = (e.rawY - heartSize / 2).coerceIn(arenaY, arenaY + arenaSize - heartSize)
                }
                true
            }

            startGameLoop(activity, overlay, heart, hpBarFill, hpNumbers, sans, originalView, mediaPlayer, arenaX, arenaY, arenaSize, hpBarMaxWidth)
        }
    }

    private fun startGameLoop(
        activity: Activity, overlay: FrameLayout, heart: ImageView, bar: View, txt: TextView, sans: ImageView,
        orig: View, mp: MediaPlayer?, ax: Float, ay: Float, size: Float, hpMaxW: Float
    ) {
        val projs = mutableListOf<View>()
        val start = System.currentTimeMillis()
        var lastSpawn = 0L

        val gameLoop = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 100000; repeatCount = ValueAnimator.INFINITE
        }

        gameLoop.addUpdateListener {
            if (isGameOver) return@addUpdateListener
            val elapsed = System.currentTimeMillis() - start

            if (elapsed > 30000) {
                isGameOver = true
                triggerWin(activity, overlay, orig, mp)
                return@addUpdateListener
            }


            val spawnRate = if (elapsed > 15000) 140L else 300L

            if (System.currentTimeMillis() - lastSpawn > spawnRate) {
                lastSpawn = System.currentTimeMillis()
                if (Math.random() < 0.75) {
                    spawnBone(activity, overlay, ax, ay, size, projs)
                } else {
                    spawnGaster(activity, overlay, sans, ax, ay, size, projs)
                }
            }

            val heartRect = RectF(heart.x + 8f, heart.y + 8f, heart.x + heart.width - 8f, heart.y + heart.height - 8f)
            val it = projs.iterator()
            while (it.hasNext()) {
                val p = it.next()
                if (p.tag == "bone") p.y -= 18f
                if (p.y < ay - 100 || p.tag == "expired") {
                    overlay.removeView(p); it.remove(); continue
                }
                if (RectF.intersects(heartRect, RectF(p.x, p.y, p.x + p.width, p.y + p.height))) {
                    takeDamage(activity, heart, bar, txt, overlay, orig, mp, gameLoop, hpMaxW)
                }
            }
        }
        gameLoop.start()
    }

    private fun spawnBone(activity: Activity, overlay: FrameLayout, ax: Float, ay: Float, size: Float, projs: MutableList<View>) {
        val bone = ImageView(activity).apply {
            setImageResource(R.drawable.ut_bone)
            scaleType = ImageView.ScaleType.FIT_XY
            x = ax + (Math.random() * (size - 25f)).toFloat()
            y = ay + size
            tag = "bone"
        }
        overlay.addView(bone, 25, (70 + Math.random() * 130).toInt())
        projs.add(bone)
    }

    private fun spawnGaster(activity: Activity, overlay: FrameLayout, sans: ImageView, ax: Float, ay: Float, size: Float, projs: MutableList<View>) {
        val yPos = ay + (Math.random() * (size - 60f)).toFloat()
        sans.setColorFilter(Color.parseColor("#42f5f1"), PorterDuff.Mode.MULTIPLY)

        val blaster = ImageView(activity).apply {
            setImageResource(R.drawable.ut_blaster)
            x = ax - 160f; y = yPos - 40f
            scaleX = 0f; scaleY = 0f
        }
        overlay.addView(blaster, 100, 100)
        blaster.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()

        overlay.postDelayed({
            sans.clearColorFilter()
            val beam = View(activity).apply {
                setBackgroundColor(Color.WHITE)
                x = ax; y = yPos; tag = "beam"
            }
            overlay.addView(beam, size.toInt(), 55)
            projs.add(beam)

            overlay.postDelayed({
                overlay.removeView(beam); beam.tag = "expired"
                blaster.animate().alpha(0f).scaleX(0f).setDuration(150).withEndAction { overlay.removeView(blaster) }.start()
            }, 250)
        }, 400)
    }

    private fun takeDamage(activity: Activity, heart: ImageView, bar: View, txt: TextView, overlay: FrameLayout, orig: View, mp: MediaPlayer?, loop: ValueAnimator, hpMaxW: Float) {
        if (isInvulnerable || isGameOver) return
        hp = max(0, hp - 25)
        txt.text = "$hp / $maxHp"
        bar.layoutParams.width = (hpMaxW * (hp.toFloat() / maxHp)).toInt().coerceAtLeast(1)
        bar.requestLayout()

        try { MediaPlayer.create(activity, R.raw.snd_hurt)?.apply { start(); setOnCompletionListener { release() } } } catch (e: Exception) {}

        if (hp <= 0) {
            isGameOver = true
            loop.cancel()
            triggerDeath(activity, overlay, heart, orig, mp)
        } else {
            isInvulnerable = true
            val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 100; repeatCount = 5; repeatMode = ValueAnimator.REVERSE
                addUpdateListener { heart.alpha = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(a: Animator) { heart.alpha = 1f; isInvulnerable = false } })
            }
            anim.start()
        }
    }

    private fun triggerDeath(activity: Activity, overlay: FrameLayout, heart: ImageView, orig: View, mp: MediaPlayer?) {
        mp?.stop(); mp?.release()
        heart.setImageResource(R.drawable.ut_heart_break)
        try { MediaPlayer.create(activity, R.raw.snd_break)?.start() } catch (e: Exception) {}

        overlay.postDelayed({
            heart.visibility = View.INVISIBLE

            for (i in 0 until 6) {
                val shard = ImageView(activity).apply {
                    setImageResource(R.drawable.ut_heart_shard)
                    x = heart.x + 10f; y = heart.y + 10f
                }
                overlay.addView(shard, 15, 15)
                val vx = (Math.random() * 20 - 10).toFloat()
                var vy = (Math.random() * -30 - 10).toFloat()
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2000
                    addUpdateListener {
                        shard.x += vx; shard.y += vy
                        vy += 1.5f; shard.rotation += 15f
                    }
                    start()
                }
            }
            overlay.postDelayed({
                overlay.animate().alpha(0f).setDuration(1200).withEndAction {
                    (activity.window.decorView as ViewGroup).removeView(overlay)
                    orig.visibility = View.VISIBLE
                    isPlaying = false
                }.start()
            }, 1800)
        }, 1200)
    }

    private fun triggerWin(activity: Activity, overlay: FrameLayout, orig: View, mp: MediaPlayer?) {
        mp?.stop(); mp?.release()
        val flash = View(activity).apply { setBackgroundColor(Color.WHITE); alpha = 0f }
        overlay.addView(flash, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        flash.animate().alpha(1f).setDuration(600).withEndAction {
            (activity.window.decorView as ViewGroup).removeView(overlay)
            orig.visibility = View.VISIBLE
            isPlaying = false
            orig.performClick()
        }.start()
    }
}