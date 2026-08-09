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

    // ---- Brick wiring ----

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

    // ---- Sprite flag ----

    @Test
    public void testSpriteIsRagdolledDefaultsFalse() {
        assertFalse("New sprite should not be in ragdoll mode", sprite.isRagdolled);
    }

    @Test
    public void testSpriteRagdollFlagCanBeSetTrue() {
        sprite.isRagdolled = true;
        assertTrue(sprite.isRagdolled);
    }

    @Test
    public void testSpriteRagdollFlagCanBeSetFalse() {
        sprite.isRagdolled = true;
        sprite.isRagdolled = false;
        assertFalse(sprite.isRagdolled);
    }

    // ---- Formula round-trip ----

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

    // ---- SetRagdollAction runtime effect ----

    private SetRagdollAction createAction(double formulaValue) {
        SetRagdollAction action = new SetRagdollAction();
        action.setEnable(new Formula(formulaValue));
        org.catrobat.catroid.content.Scope scope = Mockito.mock(org.catrobat.catroid.content.Scope.class);
        Mockito.when(scope.getSprite()).thenReturn(sprite);
        action.setScope(scope);
        return action;
    }

    @Test
    public void testActionSetsRagdollFlagTrue() {
        SetRagdollAction action = createAction(1);

        sprite.isRagdolled = false;
        action.act(1.0f);

        assertTrue("Non-zero value should enable ragdoll", sprite.isRagdolled);
    }

    @Test
    public void testActionSetsRagdollFlagFalse() {
        SetRagdollAction action = createAction(0);

        sprite.isRagdolled = true;
        action.act(1.0f);

        assertFalse("Zero value should disable ragdoll", sprite.isRagdolled);
    }

    @Test
    public void testActionAnyNonZeroEnablesRagdoll() {
        SetRagdollAction action = createAction(0.5);

        sprite.isRagdolled = false;
        action.act(1.0f);

        assertTrue("Any non-zero value should enable ragdoll", sprite.isRagdolled);
    }
}
