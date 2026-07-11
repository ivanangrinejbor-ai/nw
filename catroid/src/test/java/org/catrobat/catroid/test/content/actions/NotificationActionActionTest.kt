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
import org.catrobat.catroid.content.notification.ActionBehavior
import org.catrobat.catroid.content.notification.NotificationStorage
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class NotificationActionActionTest {

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
    fun testNotificationActionStoresActionData() {
        // First prepare a notification
        val prepareAction = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(1), Formula("ch"), Formula("T"), Formula("X"), Formula(""), 2, false
        )
        prepareAction.act(1.0f)

        // Now add an action
        val action = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            Formula(1), Formula("action_1"), Formula("Tap here"),
            Formula(""), Formula(""), 0, false
        )
        action.act(1.0f)

        val actions = NotificationStorage.getActions(1)
        assertEquals(1, actions.size)
        assertEquals("action_1", actions[0].actionId)
        assertEquals("Tap here", actions[0].text)
        assertEquals(ActionBehavior.LAUNCH_APP, actions[0].behavior)
        assertEquals(false, actions[0].hasInput)
    }

    @Test
    fun testNotificationActionWithInput() {
        val prepareAction = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(2), Formula("ch"), Formula("T"), Formula("X"), Formula(""), 2, false
        )
        prepareAction.act(1.0f)

        val action = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            Formula(2), Formula("reply_action"), Formula("Reply"),
            Formula(""), Formula("Type your answer"), 3, true
        )
        action.act(1.0f)

        val actions = NotificationStorage.getActions(2)
        assertEquals(1, actions.size)
        assertEquals("reply_action", actions[0].actionId)
        assertEquals("Reply", actions[0].text)
        assertEquals(ActionBehavior.ADD_INPUT_FIELD, actions[0].behavior)
        assertEquals(true, actions[0].hasInput)
        assertEquals("Type your answer", actions[0].inputHint)
    }

    @Test
    fun testMultipleActionsAreCollected() {
        val prepareAction = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(3), Formula("ch"), Formula("T"), Formula("X"), Formula(""), 2, false
        )
        prepareAction.act(1.0f)

        val action1 = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            Formula(3), Formula("act1"), Formula("One"), Formula(""), Formula(""), 0, false
        )
        action1.act(1.0f)

        val action2 = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            Formula(3), Formula("act2"), Formula("Two"), Formula(""), Formula(""), 1, false
        )
        action2.act(1.0f)

        val actions = NotificationStorage.getActions(3)
        assertEquals(2, actions.size)
    }

    @Test
    fun testActionOnlyExecutesOnce() {
        val prepareAction = sprite.actionFactory.createPrepareNotificationAction(
            sprite, scriptSequence,
            Formula(4), Formula("ch"), Formula("T"), Formula("X"), Formula(""), 2, false
        )
        prepareAction.act(1.0f)

        val action = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            Formula(4), Formula("unique"), Formula("Only once"),
            Formula(""), Formula(""), 0, false
        )
        // Call act twice - should only add one action
        action.act(1.0f)
        action.act(1.0f)

        val actions = NotificationStorage.getActions(4)
        assertEquals(1, actions.size)
    }

    @Test
    fun testNullNotificationIdDoesNothing() {
        val action = sprite.actionFactory.createNotificationActionAction(
            sprite, scriptSequence,
            null, Formula("act"), Formula("Text"),
            Formula(""), Formula(""), 0, false
        )
        action.act(1.0f)

        assertTrue(NotificationStorage.getAll().isEmpty())
    }
}
