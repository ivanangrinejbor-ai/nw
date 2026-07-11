/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.TouchDirectionBrick;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class TouchDirectionBrickTest {

    private Sprite sprite;

    @Before
    public void setUp() throws Exception {
        Project project = new Project(MockUtil.mockContextForProject(), "Project");
        Scene currentlyPlayingScene = new Scene("Currently playing scene", project);
        sprite = new Sprite("Sprite");
        currentlyPlayingScene.addSprite(sprite);
        project.addScene(currentlyPlayingScene);
        ProjectManager.getInstance().setCurrentProject(project);
        ProjectManager.getInstance().setCurrentlyEditedScene(new Scene());
        ProjectManager.getInstance().setCurrentlyPlayingScene(currentlyPlayingScene);
    }

    @Test
    public void testTouchDirectionBrickCreatesActionWithCorrectSprite() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        TouchDirectionBrick brick = new TouchDirectionBrick();

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createTouchDirectionAction(eq(sprite),
                any(ScriptSequenceAction.class));
    }

    @Test
    public void testTouchDirectionBrickReturnsCorrectViewResource() {
        TouchDirectionBrick brick = new TouchDirectionBrick();
        org.junit.Assert.assertEquals(
                org.catrobat.catroid.R.layout.brick_touch_direction,
                brick.getViewResource());
    }
}
