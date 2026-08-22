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
import org.catrobat.catroid.content.bricks.FirebaseGetUserIdBrick;
import org.catrobat.catroid.content.bricks.FirebaseIsSignedInBrick;
import org.catrobat.catroid.content.bricks.FirebaseSignInAnonymouslyBrick;
import org.catrobat.catroid.content.bricks.FirebaseSignInEmailPasswordBrick;
import org.catrobat.catroid.content.bricks.FirebaseSignOutBrick;
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
public class FirebaseAuthBricksTest {

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
    public void testSignInAnonymouslyBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        FirebaseSignInAnonymouslyBrick brick = new FirebaseSignInAnonymouslyBrick();

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createFirebaseSignInAnonymouslyAction(eq(sprite),
                any(SequenceAction.class), any(Boolean.class));
    }

    @Test
    public void testSignInEmailPasswordBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        FirebaseSignInEmailPasswordBrick brick = new FirebaseSignInEmailPasswordBrick("a@b.c", "pass");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createFirebaseSignInEmailPasswordAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Boolean.class));
    }

    @Test
    public void testSignOutBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        FirebaseSignOutBrick brick = new FirebaseSignOutBrick();

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createFirebaseSignOutAction(eq(sprite),
                any(SequenceAction.class), any(Boolean.class));
    }

    @Test
    public void testGetUserIdBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        FirebaseGetUserIdBrick brick = new FirebaseGetUserIdBrick();

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createFirebaseGetUserIdAction(eq(sprite),
                any(SequenceAction.class), any());
    }

    @Test
    public void testIsSignedInBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        FirebaseIsSignedInBrick brick = new FirebaseIsSignedInBrick();

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createFirebaseIsSignedInAction(eq(sprite),
                any(SequenceAction.class), any());
    }
}