/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite

class SystemLoadingActor(
    private val project: Project
) : Actor() {
    private var progress = 0f
    private val totalSteps = 5f
    private var currentStep = 0
    private val shapeRenderer = ShapeRenderer()
    private var font: BitmapFont = BitmapFont()
    private var loaded = false

    init {
        val header = project.xmlHeader
        setSize(header.virtualScreenWidth.toFloat(), header.virtualScreenHeight.toFloat())
        setPosition(-header.virtualScreenWidth / 2f, -header.virtualScreenHeight / 2f)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (loaded) return
        batch.end()

        // Black background
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.DARK_GRAY
        val barW = width * 0.6f
        val barH = 20f
        val barX = (width - barW) / 2f + x
        val barY = height * 0.3f + y

        // Background bar
        shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(barX, barY, barW, barH)

        // Progress bar
        shapeRenderer.color = Color(0f, 0.8f, 0f, 1f)
        shapeRenderer.rect(barX, barY, barW * progress, barH)
        shapeRenderer.end()

        batch.begin()
        font.color = Color.WHITE
        val text = "Loading..."
        val tw = font.getBounds(text).width
        font.draw(batch, text, x + (width - tw) / 2f, barY + barH + 40f)

        font.color = Color.LIGHT_GRAY
        val pct = "${(progress * 100).toInt()}%"
        val pw = font.getBounds(pct).width
        font.draw(batch, pct, x + (width - pw) / 2f, barY - 10f)

        if (!loaded) {
            stepLoad()
        }
    }

    private fun stepLoad() {
        when (currentStep) {
            0 -> { preloadRuntime(); currentStep++; progress = 0.2f }
            1 -> { preloadScenes(); currentStep++; progress = 0.4f }
            2 -> { preloadLooks(); currentStep++; progress = 0.6f }
            3 -> { preloadSounds(); currentStep++; progress = 0.8f }
            4 -> { currentStep++; progress = 1f; loaded = true }
        }
    }

    private fun preloadRuntime() {
        project.sceneList.forEach { it.resetPhysicsWorld() }
    }

    private fun preloadScenes() {
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                sprite.resetSprite()
                sprite.look?.setRenderingContext(null, null, null)
            }
        }
    }

    private fun preloadLooks() {
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (look in sprite.lookList) {
                    look.pixmap?.let { it.width } // force load
                }
            }
        }
    }

    private fun preloadSounds() {
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                for (sound in sprite.soundList) {
                    sound.file?.let { it.exists() } // verify existence
                }
            }
        }
    }

    fun isComplete(): Boolean = loaded
        font.dispose()
        shapeRenderer.dispose()
    }
}
