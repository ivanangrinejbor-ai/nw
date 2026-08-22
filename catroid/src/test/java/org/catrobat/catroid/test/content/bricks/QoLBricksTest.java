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
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.BlinkSpriteAction;
import org.catrobat.catroid.content.actions.ClampPositionAction;
import org.catrobat.catroid.content.actions.CooldownAction;
import org.catrobat.catroid.content.actions.FlashColorAction;
import org.catrobat.catroid.content.actions.FlipLookAction;
import org.catrobat.catroid.content.actions.MoveTowardsPointAction;
import org.catrobat.catroid.content.actions.RotateTowardsTargetAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.WaitFramesAction;
import org.catrobat.catroid.content.bricks.BlinkSpriteBrick;
import org.catrobat.catroid.content.bricks.ClampPositionBrick;
import org.catrobat.catroid.content.bricks.CooldownBrick;
import org.catrobat.catroid.content.bricks.FlashColorBrick;
import org.catrobat.catroid.content.bricks.FlipLookBrick;
import org.catrobat.catroid.content.bricks.MoveTowardsPointBrick;
import org.catrobat.catroid.content.bricks.RotateTowardsTargetBrick;
import org.catrobat.catroid.content.bricks.WaitFramesBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.catrobat.catroid.test.StaticSingletonInitializer.initializeStaticSingletonMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class QoLBricksTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
		sprite.look.setWidth(100f);
		sprite.look.setHeight(100f);
		sprite.look.setXInUserInterfaceDimensionUnit(0f);
		sprite.look.setYInUserInterfaceDimensionUnit(0f);
		sprite.look.setMotionDirectionInUserInterfaceDimensionUnit(0f);
	}

	@Test
	public void testMoveTowardsPointAction() {
		sprite.look.setXInUserInterfaceDimensionUnit(0f);
		sprite.look.setYInUserInterfaceDimensionUnit(0f);

		Action action = sprite.getActionFactory().createMoveTowardsPointAction(sprite, new SequenceAction(),
				new Formula(100), new Formula(0), new Formula(25));
		action.act(1.0f);

		assertEquals(25.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
		assertEquals(0.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);

		action.restart();
		action.act(1.0f);
		assertEquals(50.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);

		Action reachAction = sprite.getActionFactory().createMoveTowardsPointAction(sprite, new SequenceAction(),
				new Formula(60), new Formula(0), new Formula(25));
		reachAction.act(1.0f);
		assertEquals(60.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testRotateTowardsTargetAction() {
		sprite.look.setXInUserInterfaceDimensionUnit(0f);
		sprite.look.setYInUserInterfaceDimensionUnit(0f);
		sprite.look.setMotionDirectionInUserInterfaceDimensionUnit(0f);

		Action action = sprite.getActionFactory().createRotateTowardsTargetAction(sprite, new SequenceAction(),
				new Formula(100), new Formula(0), new Formula(30));
		action.act(1.0f);

		assertEquals(30.0f, sprite.look.getMotionDirectionInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testClampPositionAction() {
		sprite.look.setXInUserInterfaceDimensionUnit(600f);
		sprite.look.setYInUserInterfaceDimensionUnit(-1200f);

		Action action = sprite.getActionFactory().createClampPositionAction(sprite, new SequenceAction(),
				new Formula(-500), new Formula(500), new Formula(-800), new Formula(800));
		action.act(1.0f);

		assertEquals(500.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
		assertEquals(-800.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testWaitFramesAction() {
		WaitFramesAction action = (WaitFramesAction) sprite.getActionFactory().createWaitFramesAction(sprite, new SequenceAction(), new Formula(3));

		assertFalse(action.act(0.016f));
		assertFalse(action.act(0.016f));
		assertTrue(action.act(0.016f));
	}

	@Test
	public void testCooldownAction() {
		CooldownAction action = (CooldownAction) sprite.getActionFactory().createCooldownAction(sprite, new SequenceAction(), new Formula(1.0));
		final int[] executionCount = new int[]{0};
		action.setInnerAction(new Action() {
			@Override
			public boolean act(float delta) {
				executionCount[0]++;
				return true;
			}
		});

		action.restart();
		action.act(0.016f);
		assertEquals(1, executionCount[0]);

		action.restart();
		action.act(0.016f);
		assertEquals(1, executionCount[0]);
	}

	@Test
	public void testFlipLookBrick() {
		FlipLookBrick brick = new FlipLookBrick(FlipLookBrick.FLIP_HORIZONTAL);
		assertEquals(FlipLookBrick.FLIP_HORIZONTAL, brick.getFlipMode());
		brick.setFlipMode(FlipLookBrick.FLIP_VERTICAL);
		assertEquals(FlipLookBrick.FLIP_VERTICAL, brick.getFlipMode());
	}

	@Test
	public void testFlashColorBrick() {
		FlashColorBrick brick = new FlashColorBrick(50, 2.0);
		assertNotNull(brick);
	}

	@Test
	public void testBlinkSpriteBrick() {
		BlinkSpriteBrick brick = new BlinkSpriteBrick(5, 0.1);
		assertNotNull(brick);
	}

	@Test
	public void testMoveTowardsPointBrick() {
		MoveTowardsPointBrick brick = new MoveTowardsPointBrick(100, 200, 10);
		assertNotNull(brick);
	}

	@Test
	public void testRotateTowardsTargetBrick() {
		RotateTowardsTargetBrick brick = new RotateTowardsTargetBrick(100, 200, 15);
		assertNotNull(brick);
	}

	@Test
	public void testClampPositionBrick() {
		ClampPositionBrick brick = new ClampPositionBrick(-100, 100, -200, 200);
		assertNotNull(brick);
	}

	@Test
	public void testCooldownBrick() {
		CooldownBrick brick = new CooldownBrick(2.5);
		assertNotNull(brick.getEndBrick());
		assertEquals(2, brick.getAllParts().size());
	}

	@Test
	public void testWaitFramesBrick() {
		WaitFramesBrick brick = new WaitFramesBrick(10);
		assertNotNull(brick);
	}
}
