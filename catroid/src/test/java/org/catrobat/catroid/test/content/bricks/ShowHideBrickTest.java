/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SetVisibleAction;
import org.catrobat.catroid.content.bricks.HideBrick;
import org.catrobat.catroid.content.bricks.ShowBrick;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class ShowHideBrickTest {

    private Sprite sprite;
    private Scene scene;

    @Before
    public void setUp() throws Exception {
        Project project = new Project(MockUtil.mockContextForProject(), "Project");
        scene = new Scene("Scene", project);
        sprite = new Sprite("TestSprite");
        scene.addSprite(sprite);
        project.addScene(scene);
        ProjectManager.getInstance().setCurrentProject(project);
        ProjectManager.getInstance().setCurrentlyEditedScene(scene);
        ProjectManager.getInstance().setCurrentlyPlayingScene(scene);
    }

    @Test
    public void testHideBrickDelegatesToActionFactory() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);

        HideBrick brick = new HideBrick();
        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createHideAction(sprite);
    }

    @Test
    public void testShowBrickDelegatesToActionFactory() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);

        ShowBrick brick = new ShowBrick();
        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createShowAction(sprite);
    }

    @Test
    public void testLookVisibilitySynchronization() {
        sprite.look.setLookVisible(false);
        assertFalse(sprite.look.isLookVisible());
        assertFalse(sprite.look.isVisible());

        sprite.look.setLookVisible(true);
        assertTrue(sprite.look.isLookVisible());
        assertTrue(sprite.look.isVisible());
    }

    @Test
    public void testSetVisibleActionExecution() {
        Action hideAction = sprite.getActionFactory().createHideAction(sprite);
        hideAction.act(0.1f);
        assertFalse(sprite.look.isLookVisible());
        assertFalse(sprite.look.isVisible());

        Action showAction = sprite.getActionFactory().createShowAction(sprite);
        showAction.act(0.1f);
        assertTrue(sprite.look.isLookVisible());
        assertTrue(sprite.look.isVisible());
    }
}
