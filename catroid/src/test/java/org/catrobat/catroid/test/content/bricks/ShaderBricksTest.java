/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.DarkGrayscaleShaderAction;
import org.catrobat.catroid.content.actions.InvertColorsShaderAction;
import org.catrobat.catroid.content.actions.NormalizeSpriteShaderAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SetFilterBlurAction;
import org.catrobat.catroid.content.actions.SetFilterPixelateAction;
import org.catrobat.catroid.content.actions.SetFilterSepiaAction;
import org.catrobat.catroid.content.actions.TintShaderAction;
import org.catrobat.catroid.content.bricks.DarkGrayscaleShaderBrick;
import org.catrobat.catroid.content.bricks.InvertColorsShaderBrick;
import org.catrobat.catroid.content.bricks.NormalizeSpriteShaderBrick;
import org.catrobat.catroid.content.bricks.SetFilterBlurBrick;
import org.catrobat.catroid.content.bricks.SetFilterPixelateBrick;
import org.catrobat.catroid.content.bricks.SetFilterSepiaBrick;
import org.catrobat.catroid.content.bricks.TintShaderBrick;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.catrobat.catroid.test.StaticSingletonInitializer.initializeStaticSingletonMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ShaderBricksTest {

    private Sprite sprite;

    @Before
    public void setUp() {
        initializeStaticSingletonMethods();
        sprite = new Sprite("TestSprite");
    }

    @Test
    public void testNormalizeSpriteShaderBrick() {
        NormalizeSpriteShaderBrick brick = new NormalizeSpriteShaderBrick();
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof NormalizeSpriteShaderAction);
    }

    @Test
    public void testDarkGrayscaleShaderBrick() {
        DarkGrayscaleShaderBrick brick = new DarkGrayscaleShaderBrick();
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof DarkGrayscaleShaderAction);
    }

    @Test
    public void testInvertColorsShaderBrick() {
        InvertColorsShaderBrick brick = new InvertColorsShaderBrick();
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof InvertColorsShaderAction);
    }

    @Test
    public void testTintShaderBrick() {
        TintShaderBrick brick = new TintShaderBrick(255.0, 128.0, 0.0, 75.0);
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof TintShaderAction);
    }

    @Test
    public void testSetFilterPixelateBrick() {
        SetFilterPixelateBrick brick = new SetFilterPixelateBrick(8.0f);
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof SetFilterPixelateAction);
    }

    @Test
    public void testSetFilterBlurBrick() {
        SetFilterBlurBrick brick = new SetFilterBlurBrick(4.0f);
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof SetFilterBlurAction);
    }

    @Test
    public void testSetFilterSepiaBrick() {
        SetFilterSepiaBrick brick = new SetFilterSepiaBrick(0.8f);
        ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
        brick.addActionToSequence(sprite, sequence);
        assertEquals(1, sequence.getActions().size);
        Action action = sequence.getActions().first();
        assertTrue(action instanceof SetFilterSepiaAction);
    }
}
