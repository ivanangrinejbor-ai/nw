package org.catrobat.catroid.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator

object OtherPranksManager {

    var isPrankRunning = false

    fun runAway(activity: Activity, view: View) {
        val decorView = activity.window.decorView as ViewGroup

        val maxX = decorView.width - view.width
        val maxY = decorView.height - view.height

        val newX = (Math.random() * maxX).toFloat()
        val newY = (Math.random() * maxY).toFloat()

        view.animate()
            .x(newX)
            .y(newY)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun doBarrelRoll(activity: Activity, originalView: View) {
        if (isPrankRunning) return
        isPrankRunning = true

        val decorView = activity.window.decorView

        decorView.animate()
            .rotationBy(360f)
            .setDuration(1200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                isPrankRunning = false
                originalView.performClick()
            }
            .start()
    }

    fun gravityDrop(activity: Activity, view: View) {
        if (isPrankRunning) return
        isPrankRunning = true

        val decorView = activity.window.decorView as ViewGroup
        val originalY = view.y
        val dropY = (decorView.height - view.height).toFloat()

        view.animate()
            .y(dropY)
            .setDuration(800)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                view.postDelayed({
                    view.animate()
                        .y(originalY)
                        .setDuration(300)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            isPrankRunning = false
                            view.performClick()
                        }.start()
                }, 500)
            }.start()
    }
}