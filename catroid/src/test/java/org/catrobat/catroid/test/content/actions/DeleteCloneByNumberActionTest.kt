/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions

import com.badlogic.gdx.utils.GdxNativesLoader
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.actions.DeleteCloneByNumberAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageListener
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(GdxNativesLoader::class, StageActivity::class)
class DeleteCloneByNumberActionTest {

    private lateinit var sprite: Sprite
    private lateinit var scope: Scope
    private lateinit var stageListener: StageListener

    @Before
    fun setUp() {
        PowerMockito.mockStatic(GdxNativesLoader::class.java)
        PowerMockito.mockStatic(StageActivity::class.java)

        sprite = Sprite("testSprite")
        sprite.cloneIndex = 0
        scope = Scope(null, sprite, ScriptSequenceAction(Mockito.mock(Script::class.java)))

        stageListener = Mockito.mock(StageListener::class.java)
        PowerMockito.`when`(StageActivity.getActiveStageListener()).thenReturn(stageListener)
    }

    @Test
    fun testDeleteCloneByNumber() {
        val action = DeleteCloneByNumberAction()
        action.scope = scope
        action.cloneNumber = Formula(5)

        action.act(1.0f)

        Mockito.verify(stageListener).removeCloneByIndex(5)
    }

    @Test
    fun testDeleteCloneByNumberWithNullFormula() {
        val action = DeleteCloneByNumberAction()
        action.scope = scope
        action.cloneNumber = null

        action.act(1.0f)

        Mockito.verify(stageListener, Mockito.never()).removeCloneByIndex(Mockito.anyInt())
    }

    @Test
    fun testDeleteCloneByNumberWithNullStageListener() {
        PowerMockito.`when`(StageActivity.getActiveStageListener()).thenReturn(null)

        val action = DeleteCloneByNumberAction()
        action.scope = scope
        action.cloneNumber = Formula(1)

        action.act(1.0f)
        // Should not throw NPE
    }
}
