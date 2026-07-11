/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class PrepareNotificationActionTest {

    private lateinit var sprite: Sprite
    private lateinit var scriptSequence: ScriptSequenceAction

    @Before
    fun setUp() {
        NotificationStorage.clear()
        val project = Project(MockUtil.mockContextForProject(), "Project")
        val scene = Scene("test", project)
        sprite = Sprite("testSprite")
        scene.addSprite(sprite)
        project.addScene(scene)
        ProjectManager.getInstance().setCurrentProject(project)
        scriptSequence = ScriptSequenceAction(Mockito.mock(Script::class.java))
    }

    @Test
    fun testPrepareNotificationStoresData() {
        val action = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(42), Formula("my_channel"), Formula("My Title"),
            Formula("My Text"), Formula(""), 3, true
        )
        action.act(1.0f)

        val data = NotificationStorage.get(42)
        assertNotNull(data)
        assertEquals("my_channel", data!!.channelName)
        assertEquals("My Title", data.title)
        assertEquals("My Text", data.text)
        assertEquals(3, data.importanceLevel)
        assertEquals(true, data.isPinned)
    }

    @Test
    fun testPrepareNotificationDefaultImportance() {
        val action = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(1), Formula("ch"), Formula("T"), Formula("X"), Formula(""), 0, false
        )
        action.act(1.0f)

        val data = NotificationStorage.get(1)
        assertNotNull(data)
        assertEquals("ch", data!!.channelName)
        assertEquals(0, data.importanceLevel)
        assertEquals(false, data.isPinned)
    }

    @Test
    fun testPrepareNotificationWithNullIdDoesNothing() {
        val action = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            null, Formula("ch"), Formula("T"), Formula("X"), Formula(""), 2, false
        )
        action.act(1.0f)

        assertTrue(NotificationStorage.getAll().isEmpty())
    }

    @Test
    fun testPrepareNotificationOverwritesExisting() {
        val action1 = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(5), Formula("ch1"), Formula("Old"), Formula("Old text"),
            Formula(""), 1, false
        )
        action1.act(1.0f)

        val action2 = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(5), Formula("ch2"), Formula("New"), Formula("New text"),
            Formula(""), 3, false
        )
        action2.act(1.0f)

        val data = NotificationStorage.get(5)
        assertEquals("ch2", data!!.channelName)
        assertEquals("New", data.title)
        assertEquals(3, data.importanceLevel)
    }
}
