/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.TouchDirectionAction;
import org.catrobat.catroid.utils.TouchUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TouchUtil.class})
public class TouchDirectionActionTest {

    private Sprite sprite;
    private Scope scope;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TouchUtil.class);
        sprite = new Sprite("testSprite");
        sprite.look.setXInUserInterfaceDimensionUnit(0f);
        sprite.look.setYInUserInterfaceDimensionUnit(0f);
        scope = new Scope(null, sprite, new SequenceAction());
    }

    @Test
    public void testDirectionAboveSprite() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(0f);
        when(TouchUtil.getY(anyInt())).thenReturn(100f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(0f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testDirectionBelowSprite() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(0f);
        when(TouchUtil.getY(anyInt())).thenReturn(-100f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(180f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testDirectionRightOfSprite() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(100f);
        when(TouchUtil.getY(anyInt())).thenReturn(0f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(90f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testDirectionLeftOfSprite() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(-100f);
        when(TouchUtil.getY(anyInt())).thenReturn(0f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(-90f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testDirectionAtSamePosition() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(0f);
        when(TouchUtil.getY(anyInt())).thenReturn(0f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(90f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testNoTouchDoesNothing() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(0);

        sprite.look.setMotionDirectionInUserInterfaceDimensionUnit(45f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        assertEquals(45f, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }

    @Test
    public void testNullScopeDoesNothing() {
        TouchDirectionAction action = new TouchDirectionAction();
        action.act(1.0f);
        // Should not throw NPE
    }

    @Test
    public void testDiagonalDirection() {
        when(TouchUtil.getNumberOfCurrentTouches()).thenReturn(1);
        when(TouchUtil.getX(anyInt())).thenReturn(100f);
        when(TouchUtil.getY(anyInt())).thenReturn(100f);

        TouchDirectionAction action = new TouchDirectionAction();
        action.setScope(scope);
        action.act(1.0f);

        float expected = 90f - (float) Math.toDegrees(Math.atan2(100, 100));
        assertEquals(expected, sprite.look.getLookDirectionInUserInterfaceDimensionUnit(), 0.01f);
    }
}
