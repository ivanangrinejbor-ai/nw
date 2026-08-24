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
import org.catrobat.catroid.content.actions.SetRagdollAction;
import org.catrobat.catroid.content.bricks.SetRagdollBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class SetRagdollBrickTest {

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
    public void testBrickDelegatesToActionFactory() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);

        SetRagdollBrick brick = new SetRagdollBrick(1);
        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createSetRagdollAction(
                eq(sprite),
                any(SequenceAction.class),
                any(Formula.class));
    }

    @Test
    public void testBrickWithZeroParamDelegatesToActionFactory() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);

        SetRagdollBrick brick = new SetRagdollBrick(0);
        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createSetRagdollAction(
                eq(sprite),
                any(SequenceAction.class),
                any(Formula.class));
    }

    @Test
    public void testSpriteRagdollModeDefaultsZero() {
        assertTrue("New sprite should not be in ragdoll mode", sprite.ragdollMode == 0);
    }

    @Test
    public void testSpriteRagdollModeCanBeSetToRagdoll() {
        sprite.ragdollMode = 1;
        assertTrue("Mode 1 = ragdoll", sprite.ragdollMode == 1);
    }

    @Test
    public void testSpriteRagdollModeCanBeSetToFollow() {
        sprite.ragdollMode = 2;
        assertTrue("Mode 2 = ragdoll follow", sprite.ragdollMode == 2);
    }

    @Test
    public void testSpriteRagdollModeCanBeCleared() {
        sprite.ragdollMode = 1;
        sprite.ragdollMode = 0;
        assertTrue("Mode 0 = ragdoll off", sprite.ragdollMode == 0);
    }

    @Test
    public void testBrickConstructorWithIntFormulaSetsField() {
        SetRagdollBrick brickOn = new SetRagdollBrick(1);
        SetRagdollBrick brickOff = new SetRagdollBrick(0);

        Formula formulaOn = brickOn.getFormulaWithBrickField(
                org.catrobat.catroid.content.bricks.Brick.BrickField.PHYSICS_TOGGLE);
        Formula formulaOff = brickOff.getFormulaWithBrickField(
                org.catrobat.catroid.content.bricks.Brick.BrickField.PHYSICS_TOGGLE);

        assertTrue("Formula(1) should not be null", formulaOn != null);
        assertTrue("Formula(0) should not be null", formulaOff != null);
    }

    private SetRagdollAction createAction(double formulaValue) {
        SetRagdollAction action = new SetRagdollAction();
        action.setEnable(new Formula(formulaValue));
        org.catrobat.catroid.content.Scope scope = Mockito.mock(org.catrobat.catroid.content.Scope.class);
        Mockito.when(scope.getSprite()).thenReturn(sprite);
        action.setScope(scope);
        return action;
    }

    @Test
    public void testActionSetsRagdollModeOne() {
        SetRagdollAction action = createAction(1);

        sprite.ragdollMode = 0;
        action.act(1.0f);

        assertTrue("Non-zero value should enable ragdoll", sprite.ragdollMode == 1);
    }

    @Test
    public void testActionClearsRagdollMode() {
        SetRagdollAction action = createAction(0);

        sprite.ragdollMode = 1;
        action.act(1.0f);

        assertTrue("Zero value should disable ragdoll", sprite.ragdollMode == 0);
    }

    @Test
    public void testActionAnyNonZeroEnablesRagdoll() {
        SetRagdollAction action = createAction(0.5);

        sprite.ragdollMode = 0;
        action.act(1.0f);

        assertTrue("Any non-zero value should enable ragdoll", sprite.ragdollMode == 1);
    }

    @Test
    public void testActionValueTwoEnablesFollowMode() {
        SetRagdollAction action = createAction(2);

        sprite.ragdollMode = 0;
        action.act(1.0f);

        assertTrue("Value 2 should enable ragdoll follow mode", sprite.ragdollMode == 2);
    }

    @Test
    public void testActionValueAboveTwoClampsToFollowMode() {
        SetRagdollAction action = createAction(3);

        sprite.ragdollMode = 0;
        action.act(1.0f);

        assertTrue("Value >2 should clamp to ragdoll follow mode", sprite.ragdollMode == 2);
    }

    @Test
    public void testActionFormulaExceptionClearsMode() {
        SetRagdollAction action = new SetRagdollAction();
        action.setEnable(new Formula(Double.NaN));
        org.catrobat.catroid.content.Scope scope = Mockito.mock(org.catrobat.catroid.content.Scope.class);
        Mockito.when(scope.getSprite()).thenReturn(sprite);
        action.setScope(scope);

        sprite.ragdollMode = 2;
        action.act(1.0f);

        assertTrue("Exception should fall back to ragdoll off", sprite.ragdollMode == 0);
    }
}
