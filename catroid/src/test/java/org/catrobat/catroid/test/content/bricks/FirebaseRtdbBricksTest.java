/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.PushBaseBrick;
import org.catrobat.catroid.content.bricks.QueryBaseBrick;
import org.catrobat.catroid.content.bricks.UpdateBaseBrick;
import org.catrobat.catroid.content.bricks.WhenFirebaseChildChangedBrick;
import org.catrobat.catroid.formulaeditor.Formula;
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
public class FirebaseRtdbBricksTest {

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
    public void testPushBaseBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        PushBaseBrick brick = new PushBaseBrick("firebase_id", "messages", "hello");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createPushBaseAction(eq(sprite), any(SequenceAction.class),
                any(Formula.class), any(Formula.class), any(Formula.class), any(), any(Boolean.class));
    }

    @Test
    public void testUpdateBaseBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        UpdateBaseBrick brick = new UpdateBaseBrick("firebase_id", "user/name", "{\"nick\":\"Tom\"}");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createUpdateBaseAction(eq(sprite), any(SequenceAction.class),
                any(Formula.class), any(Formula.class), any(Formula.class), any(Boolean.class));
    }

    @Test
    public void testQueryBaseBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        QueryBaseBrick brick = new QueryBaseBrick("firebase_id", "messages", "score", "10", "");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createQueryBaseAction(eq(sprite), any(SequenceAction.class),
                any(Formula.class), any(Formula.class), any(Formula.class), any(Formula.class),
                any(Formula.class), any());
    }

    @Test
    public void testWhenFirebaseChildChangedBrickScriptLinkage() {
        WhenFirebaseChildChangedBrick brick = new WhenFirebaseChildChangedBrick();
        org.catrobat.catroid.content.WhenFirebaseChildChangedScript script =
                (org.catrobat.catroid.content.WhenFirebaseChildChangedScript) brick.getScript();

        brick.setEventTypeSelection(2);
        org.catrobat.catroid.content.WhenFirebaseChildChangedScript script2 =
                (org.catrobat.catroid.content.WhenFirebaseChildChangedScript) brick.getScript();
        script2.setEventTypeSelection(2);
    }
}