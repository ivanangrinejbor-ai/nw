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
    private var currentStep = 0
    private val shapeRenderer = ShapeRenderer()
    private var font: BitmapFont = BitmapFont()
    private var loaded = false

    private var looksToLoad: List<org.catrobat.catroid.common.LookData>? = null
    private var lookLoadIndex = 0
    private val LOOKS_PER_FRAME = 20

    init {
        val header = project.xmlHeader
        setSize(header.virtualScreenWidth.toFloat(), header.virtualScreenHeight.toFloat())
        setPosition(-header.virtualScreenWidth / 2f, -header.virtualScreenHeight / 2f)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (loaded) return

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val wasDrawing = batch.isDrawing
        if (wasDrawing) {
            batch.end()
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.DARK_GRAY
        val barW = width * 0.6f
        val barH = 20f
        val barX = (width - barW) / 2f + x
        val barY = height * 0.3f + y

        shapeRenderer.color = Color(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(barX, barY, barW, barH)

        shapeRenderer.color = Color(0f, 0.8f, 0f, 1f)
        shapeRenderer.rect(barX, barY, barW * progress, barH)
        shapeRenderer.end()

        batch.begin()
        font.color = Color.WHITE
        val text = "Loading..."
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text)
        font.draw(batch, text, x + (width - layout.width) / 2f, barY + barH + 40f)

        font.color = Color.LIGHT_GRAY
        val pct = "${(progress * 100).toInt()}%"
        val pctLayout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, pct)
        font.draw(batch, pct, x + (width - pctLayout.width) / 2f, barY - 10f)

        if (!wasDrawing) {
            batch.end()
        }

        if (!loaded) {
            stepLoad()
        }
    }

    private fun stepLoad() {
        when (currentStep) {
            0 -> { preloadRuntime(); currentStep++; progress = 0.1f }
            1 -> { preloadScenes(); currentStep++; progress = 0.2f }
            2 -> {
                if (looksToLoad == null) {
                    val allLooks = mutableListOf<org.catrobat.catroid.common.LookData>()
                    for (scene in project.sceneList) {
                        for (sprite in scene.spriteList) {
                            allLooks.addAll(sprite.lookList)
                        }
                    }
                    looksToLoad = allLooks
                    lookLoadIndex = 0
                }
                val looks = looksToLoad!!
                val end = minOf(lookLoadIndex + LOOKS_PER_FRAME, looks.size)
                for (i in lookLoadIndex until end) {
                    try { looks[i].pixmap } catch (_: Exception) {}
                }
                lookLoadIndex = end
                progress = 0.2f + 0.6f * (lookLoadIndex.toFloat() / maxOf(looks.size, 1))
                if (lookLoadIndex >= looks.size) {
                    currentStep++
                    looksToLoad = null
                }
            }
            3 -> { preloadSounds(); currentStep++; progress = 0.9f }
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

    fun dispose() {
        font.dispose()
        shapeRenderer.dispose()
    }
}
