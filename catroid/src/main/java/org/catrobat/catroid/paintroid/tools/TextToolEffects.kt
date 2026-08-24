// NeoCatroid (fork of Pocket Code/Catrobat, AGPL-3.0): text effect settings for the text tool.
package org.catrobat.catroid.paintroid.tools

import android.graphics.Color
import java.io.Serializable

class TextToolEffects : Serializable {
    var strokeWidth: Float = 0f
    var strokeColor: Int = Color.BLACK
    var shadowEnabled: Boolean = false
    var shadowRadius: Float = 8f
    var shadowDx: Float = 4f
    var shadowDy: Float = 4f
    var shadowColor: Int = Color.BLACK
    var pixelCrisp: Boolean = false
    var useGradient: Boolean = false
    var gradientTopColor: Int = Color.YELLOW
    var gradientBottomColor: Int = Color.RED
    var glowIntensity: Int = 0
    var glowColor: Int = Color.CYAN
    var autoDimBackground: Boolean = false

    fun copyFrom(other: TextToolEffects) {
        strokeWidth = other.strokeWidth
        strokeColor = other.strokeColor
        shadowEnabled = other.shadowEnabled
        shadowRadius = other.shadowRadius
        shadowDx = other.shadowDx
        shadowDy = other.shadowDy
        shadowColor = other.shadowColor
        pixelCrisp = other.pixelCrisp
        useGradient = other.useGradient
        gradientTopColor = other.gradientTopColor
        gradientBottomColor = other.gradientBottomColor
        glowIntensity = other.glowIntensity
        glowColor = other.glowColor
        autoDimBackground = other.autoDimBackground
    }
}
