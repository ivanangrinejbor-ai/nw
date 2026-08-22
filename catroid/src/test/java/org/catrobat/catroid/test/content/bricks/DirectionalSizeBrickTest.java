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

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ChangeHeightDirectionAction;
import org.catrobat.catroid.content.actions.ChangeWidthDirectionAction;
import org.catrobat.catroid.content.actions.SetHeightDirectionAction;
import org.catrobat.catroid.content.actions.SetWidthDirectionAction;
import org.catrobat.catroid.content.bricks.ChangeHeightDirectionBrick;
import org.catrobat.catroid.content.bricks.ChangeWidthDirectionBrick;
import org.catrobat.catroid.content.bricks.SetHeightDirectionBrick;
import org.catrobat.catroid.content.bricks.SetWidthDirectionBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.catrobat.catroid.test.StaticSingletonInitializer.initializeStaticSingletonMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class DirectionalSizeBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
		sprite.look.setWidth(100f);
		sprite.look.setHeight(100f);
		sprite.look.setXInUserInterfaceDimensionUnit(0f);
		sprite.look.setYInUserInterfaceDimensionUnit(0f);
	}

	@Test
	public void testChangeWidthDirectionRight() {
		assertEquals(1.0f, sprite.look.getScaleX(), 0.001f);
		assertEquals(0.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);

		// Increase width by 50% towards RIGHT (base width = 100, deltaScale = 0.5 -> deltaX = 100 * 0.5 / 2 = +25)
		sprite.getActionFactory().createChangeWidthDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeWidthDirectionAction.DIRECTION_RIGHT).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleX(), 0.001f);
		assertEquals(25.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testChangeWidthDirectionLeft() {
		// Increase width by 50% towards LEFT -> deltaX = -25
		sprite.getActionFactory().createChangeWidthDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeWidthDirectionAction.DIRECTION_LEFT).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleX(), 0.001f);
		assertEquals(-25.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testChangeWidthDirectionCenter() {
		// Increase width by 50% towards CENTER -> deltaX = 0
		sprite.getActionFactory().createChangeWidthDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeWidthDirectionAction.DIRECTION_CENTER).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleX(), 0.001f);
		assertEquals(0.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testSetWidthDirection() {
		// Set width to 200% towards RIGHT (from 100% -> deltaScale = 1.0 -> deltaX = +50)
		sprite.getActionFactory().createSetWidthDirectionAction(sprite, new SequenceAction(),
				new Formula(200), SetWidthDirectionAction.DIRECTION_RIGHT).act(1.0f);

		assertEquals(2.0f, sprite.look.getScaleX(), 0.001f);
		assertEquals(50.0f, sprite.look.getXInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testChangeHeightDirectionUp() {
		assertEquals(1.0f, sprite.look.getScaleY(), 0.001f);
		assertEquals(0.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);

		// Increase height by 50% towards UP (base height = 100, deltaScale = 0.5 -> deltaY = +25)
		sprite.getActionFactory().createChangeHeightDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeHeightDirectionAction.DIRECTION_UP).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleY(), 0.001f);
		assertEquals(25.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testChangeHeightDirectionDown() {
		// Increase height by 50% towards DOWN -> deltaY = -25
		sprite.getActionFactory().createChangeHeightDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeHeightDirectionAction.DIRECTION_DOWN).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleY(), 0.001f);
		assertEquals(-25.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testChangeHeightDirectionCenter() {
		// Increase height by 50% towards CENTER -> deltaY = 0
		sprite.getActionFactory().createChangeHeightDirectionAction(sprite, new SequenceAction(),
				new Formula(50), ChangeHeightDirectionAction.DIRECTION_CENTER).act(1.0f);

		assertEquals(1.5f, sprite.look.getScaleY(), 0.001f);
		assertEquals(0.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testSetHeightDirection() {
		// Set height to 200% towards UP (deltaScale = 1.0 -> deltaY = +50)
		sprite.getActionFactory().createSetHeightDirectionAction(sprite, new SequenceAction(),
				new Formula(200), SetHeightDirectionAction.DIRECTION_UP).act(1.0f);

		assertEquals(2.0f, sprite.look.getScaleY(), 0.001f);
		assertEquals(50.0f, sprite.look.getYInUserInterfaceDimensionUnit(), 0.001f);
	}

	@Test
	public void testBricksConstructorsAndGetters() {
		ChangeWidthDirectionBrick changeWidthBrick = new ChangeWidthDirectionBrick(50, ChangeWidthDirectionAction.DIRECTION_LEFT);
		assertEquals(ChangeWidthDirectionAction.DIRECTION_LEFT, changeWidthBrick.getDirection());
		assertNotNull(changeWidthBrick.getViewResource());

		SetWidthDirectionBrick setWidthBrick = new SetWidthDirectionBrick(150, SetWidthDirectionAction.DIRECTION_RIGHT);
		assertEquals(SetWidthDirectionAction.DIRECTION_RIGHT, setWidthBrick.getDirection());
		assertNotNull(setWidthBrick.getViewResource());

		ChangeHeightDirectionBrick changeHeightBrick = new ChangeHeightDirectionBrick(30, ChangeHeightDirectionAction.DIRECTION_DOWN);
		assertEquals(ChangeHeightDirectionAction.DIRECTION_DOWN, changeHeightBrick.getDirection());
		assertNotNull(changeHeightBrick.getViewResource());

		SetHeightDirectionBrick setHeightBrick = new SetHeightDirectionBrick(120, SetHeightDirectionAction.DIRECTION_UP);
		assertEquals(SetHeightDirectionAction.DIRECTION_UP, setHeightBrick.getDirection());
		assertNotNull(setHeightBrick.getViewResource());
	}
}
