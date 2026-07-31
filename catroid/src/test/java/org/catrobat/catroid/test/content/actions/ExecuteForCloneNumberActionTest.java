/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExecuteForCloneNumberAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ExecuteForCloneNumberActionTest {

    private ExecuteForCloneNumberAction action;
    private Sprite sprite;
    private Action mockInnerAction;

    @Before
    public void setUp() {
        sprite = new Sprite("testSprite");
        sprite.cloneIndex = 0;
        mockInnerAction = Mockito.mock(Action.class);
        Mockito.when(mockInnerAction.act(Mockito.anyFloat())).thenReturn(true);
    }

    private void createAction(int targetCloneNumber) {
        action = new ExecuteForCloneNumberAction();
        Scope scope = new Scope(null, sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));
        action.setScope(scope);
        action.setCloneNumber(new Formula(targetCloneNumber));
        action.setCloneAction(mockInnerAction);
    }

    @Test
    public void testExecutesForMatchingCloneNumber() {
        sprite.cloneIndex = 3;
        createAction(3);

        boolean result = action.act(1.0f);

        assertTrue(result);
        Mockito.verify(mockInnerAction).act(1.0f);
    }

    @Test
    public void testDoesNotExecuteForDifferentCloneNumber() {
        sprite.cloneIndex = 3;
        createAction(5);

        boolean result = action.act(1.0f);

        assertTrue(result);
        Mockito.verify(mockInnerAction, Mockito.never()).act(Mockito.anyFloat());
    }

    @Test
    public void testOriginalWithCloneIndex0() {
        sprite.cloneIndex = 0;
        createAction(0);

        boolean result = action.act(1.0f);

        assertTrue(result);
        Mockito.verify(mockInnerAction).act(1.0f);
    }

    @Test
    public void testNullScopeReturnsTrue() {
        action = new ExecuteForCloneNumberAction();
        action.setCloneNumber(new Formula(1));
        action.setCloneAction(mockInnerAction);

        boolean result = action.act(1.0f);

        assertTrue(result);
        Mockito.verify(mockInnerAction, Mockito.never()).act(Mockito.anyFloat());
    }

    @Test
    public void testNullFormulaReturnsTrue() {
        action = new ExecuteForCloneNumberAction();
        Scope scope = new Scope(null, sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));
        action.setScope(scope);
        action.setCloneAction(mockInnerAction);

        boolean result = action.act(1.0f);

        assertTrue(result);
        Mockito.verify(mockInnerAction, Mockito.never()).act(Mockito.anyFloat());
    }

    @Test
    public void testRestartResetsInitialized() {
        sprite.cloneIndex = 3;
        createAction(3);

        action.act(1.0f);
        Mockito.verify(mockInnerAction, Mockito.times(1)).act(1.0f);

        action.restart();
        action.act(1.0f);
        Mockito.verify(mockInnerAction, Mockito.times(2)).act(1.0f);
    }

    @Test
    public void testInnerActionDelay() {
        sprite.cloneIndex = 1;
        createAction(1);

        Action delayedInner = Mockito.mock(Action.class);
        Mockito.when(delayedInner.act(Mockito.anyFloat())).thenReturn(false);
        Mockito.when(delayedInner.act(Mockito.anyFloat())).thenReturn(false).thenReturn(true);

        action.setCloneAction(delayedInner);

        boolean first = action.act(1.0f);
        assertFalse(first);

        Mockito.verify(delayedInner).act(1.0f);
    }
}
