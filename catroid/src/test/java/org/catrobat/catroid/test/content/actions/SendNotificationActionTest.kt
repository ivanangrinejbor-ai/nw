/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions

import android.app.NotificationManager
import android.content.Context
import com.badlogic.gdx.utils.GdxNativesLoader
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.notification.NotificationData
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox

@RunWith(PowerMockRunner::class)
@PrepareForTest(GdxNativesLoader::class, StageActivity::class)
class SendNotificationActionTest {

    private lateinit var sprite: Sprite
    private lateinit var scope: Scope
    private lateinit var scriptSequence: ScriptSequenceAction

    @Before
    fun setUp() {
        PowerMockito.mockStatic(GdxNativesLoader::class.java)
        PowerMockito.mockStatic(StageActivity::class.java)

        NotificationStorage.clear()

        sprite = Sprite("testSprite")
        scope = Scope(null, sprite, ScriptSequenceAction(Mockito.mock(Script::class.java)))
        scriptSequence = ScriptSequenceAction(Mockito.mock(Script::class.java))
    }

    @After
    fun tearDown() {
        NotificationStorage.clear()
    }

    @Test
    fun testNoNotificationDataDoesNothing() {
        val action = sprite.actionFactory.createSendNotificationAction(
            sprite, scriptSequence, Formula(1)
        )
        action.act(1.0f)
        // No data in storage -> should return early without NPE
    }

    @Test
    fun testNullIdDoesNothing() {
        val action = sprite.actionFactory.createSendNotificationAction(
            sprite, scriptSequence, null
        )
        action.act(1.0f)
        // Should not throw NPE
    }

    @Test
    fun testNullActivityDoesNothing() {
        NotificationStorage.save(1, NotificationData(
            id = 1,
            channelName = "test_channel",
            title = "Test Title",
            text = "Test Text",
            iconPath = "",
            importanceLevel = NotificationManager.IMPORTANCE_DEFAULT,
            isPinned = false
        ))

        Whitebox.setInternalState(StageActivity::class.java, "activeStageActivity", null as Any?)

        val action = sprite.actionFactory.createSendNotificationAction(
            sprite, scriptSequence, Formula(1)
        )
        action.act(1.0f)
        // Should return early because activity is null
    }

    @Test
    fun testRestartAllowsReExecution() {
        NotificationStorage.save(1, NotificationData(
            id = 1, channelName = "ch", title = "T", text = "X",
            iconPath = "", importanceLevel = NotificationManager.IMPORTANCE_DEFAULT, isPinned = false
        ))

        val action = sprite.actionFactory.createSendNotificationAction(
            sprite, scriptSequence, Formula(1)
        )
        action.act(1.0f)

        // Mock activity to avoid NPE from getSystemService
        val mockActivity = Mockito.mock(StageActivity::class.java)
        val mockNotificationManager = Mockito.mock(NotificationManager::class.java)
        Mockito.`when`(mockActivity.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)
        Whitebox.setInternalState(StageActivity::class.java, "activeStageActivity", java.lang.ref.WeakReference(mockActivity))

        // After restart should re-execute
        action.restart()
        action.act(1.0f)
        // Should reach notification code path without exception
    }
}
