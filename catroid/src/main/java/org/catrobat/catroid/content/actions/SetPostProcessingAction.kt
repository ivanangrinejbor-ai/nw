package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.PostProcessingComponent
import org.catrobat.catroid.raptor.PostProcessingData
import org.catrobat.catroid.stage.StageActivity
import kotlin.math.max
import kotlin.math.min

class SetPostProcessingAction : TemporalAction() {

    var scope: Scope? = null
    var effectIndex: Int = 0
    var paramIndex: Int = 0
    var valueFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return

        var config = threeDManager.currentConfig
        if (config == null) {
            config = PostProcessingComponent()
            threeDManager.updatePostProcessing(config)
        }

        val floatVal = valueFormula?.interpretFloat(scope) ?: 0f
        val boolVal = floatVal > 0.5f

        if (effectIndex == 0) {
            when (paramIndex) {
                0 -> config.isActive = boolVal
                5 -> config.qualityScale = floatVal.coerceIn(0.1f, 1.0f)
            }
        } else {
            val targetEffect = findOrCreateEffect(config, effectIndex)
            if (targetEffect != null) {
                applyParam(targetEffect, paramIndex, floatVal, boolVal)
            }
        }

        threeDManager.updatePostProcessing(config)
    }

    private fun findOrCreateEffect(config: PostProcessingComponent, typeIndex: Int): PostProcessingData? {
        val targetClass: Class<out PostProcessingData> = when (typeIndex) {
            1 -> PostProcessingData.Bloom::class.java
            2 -> PostProcessingData.Vignette::class.java
            3 -> PostProcessingData.Levels::class.java
            4 -> PostProcessingData.Grain::class.java
            5 -> PostProcessingData.Fxaa::class.java
            6 -> PostProcessingData.Chromatic::class.java
            7 -> PostProcessingData.RadialBlur::class.java
            8 -> PostProcessingData.OldTv::class.java
            9 -> PostProcessingData.Crt::class.java
            10 -> PostProcessingData.Fisheye::class.java
            11 -> PostProcessingData.Water::class.java
            12 -> PostProcessingData.MotionBlur::class.java
            13 -> PostProcessingData.LensFlare::class.java
            14 -> PostProcessingData.Gaussian::class.java
            15 -> PostProcessingData.Zoom::class.java
            16 -> PostProcessingData.ACES::class.java
            17 -> PostProcessingData.EyeAdaptation::class.java
            18 -> PostProcessingData.RayTracing::class.java
            else -> return null
        }

        val existing = config.effects.find { targetClass.isInstance(it) }
        if (existing != null) return existing

        return try {
            val newEffect = targetClass.newInstance()
            newEffect.isEnabled = true
            config.effects.add(newEffect)
            newEffect
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyParam(data: PostProcessingData, paramIdx: Int, valFloat: Float, valBool: Boolean) {
        if (paramIdx == 0) {
            data.isEnabled = valBool
            return
        }

        when (data) {
            is PostProcessingData.RayTracing -> {
                when (paramIdx) {
                    1 -> data.reflectivity = valFloat
                    2 -> data.steps = valFloat.toInt()
                    13 -> data.jitter = valFloat
                    10 -> data.stride = valFloat
                    9 -> data.thickness = valFloat
                    14 -> data.maxDistance = valFloat
                }
            }
            is PostProcessingData.Bloom -> {
                when (paramIdx) {
                    1 -> data.intensity = valFloat
                    2 -> data.threshold = valFloat
                    3 -> data.blurAmount = valFloat
                    4 -> data.blurPasses = max(1, valFloat.toInt())
                }
            }
            is PostProcessingData.Levels -> {
                when (paramIdx) {
                    6 -> data.contrast = valFloat
                    7 -> data.saturation = valFloat
                    8 -> data.gamma = valFloat
                }
            }
            is PostProcessingData.Vignette -> {
                when (paramIdx) {
                    1 -> data.intensity = valFloat
                    7 -> data.saturation = valFloat
                }
            }
            is PostProcessingData.Grain -> {
                if (paramIdx == 1) data.amount = valFloat
            }
            is PostProcessingData.Chromatic -> {
                when (paramIdx) {
                    1 -> data.strength = valFloat
                    10 -> data.maxDistortion = valFloat
                }
            }
            is PostProcessingData.RadialBlur -> {
                when (paramIdx) {
                    1 -> data.strength = valFloat
                    4 -> data.blurPasses = max(1, valFloat.toInt())
                    3 -> data.size = valFloat
                }
            }
            is PostProcessingData.OldTv -> {
                if (paramIdx == 13 || paramIdx == 1) data.strength = valFloat
            }
            is PostProcessingData.Crt -> {
            }
            is PostProcessingData.Fisheye -> {
            }
            is PostProcessingData.Water -> {
                when (paramIdx) {
                    1 -> data.amount = valFloat
                    9 -> data.speed = valFloat
                }
            }
            is PostProcessingData.MotionBlur -> {
                if (paramIdx == 1) data.blurOpacity = valFloat.coerceIn(0f, 0.99f)
            }
            is PostProcessingData.LensFlare -> {
                when (paramIdx) {
                    1 -> data.intensity = valFloat
                    2 -> data.threshold = valFloat
                }
            }
            is PostProcessingData.Gaussian -> {
                when (paramIdx) {
                    1 -> data.amount = valFloat
                    4 -> data.passes = max(1, valFloat.toInt())
                    3 -> data.size = valFloat
                }
            }
            is PostProcessingData.Zoom -> {
                when (paramIdx) {
                    1 -> data.zoom = valFloat
                    11 -> data.originX = valFloat
                    12 -> data.originY = valFloat
                }
            }
            is PostProcessingData.ACES -> {
            }
            is PostProcessingData.EyeAdaptation -> {
                when (paramIdx) {
                    1 -> data.targetLuminance = valFloat
                    9 -> data.speed = valFloat
                    14 -> data.maxExposure = valFloat
                    15 -> data.minExposure = valFloat
                }
            }
        }
    }
}