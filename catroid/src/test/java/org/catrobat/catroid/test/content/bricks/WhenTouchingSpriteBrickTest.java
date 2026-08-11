/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2025 The Catrobat Team
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

import org.catrobat.catroid.content.WhenTouchingSpriteScript;
import org.catrobat.catroid.content.bricks.WhenTouchingSpriteBrick;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class WhenTouchingSpriteBrickTest {

	@Test
	public void testBrickReturnsItsScript() {
		WhenTouchingSpriteScript script = new WhenTouchingSpriteScript();
		WhenTouchingSpriteBrick brick = new WhenTouchingSpriteBrick(script);
		assertSame(script, brick.getScript());
	}

	@Test
	public void testDefaultConstructorCreatesScriptWithNoBackgroundReaction() {
		WhenTouchingSpriteBrick brick = new WhenTouchingSpriteBrick();
		WhenTouchingSpriteScript script = (WhenTouchingSpriteScript) brick.getScript();
		assertEquals(false, script.isReactToBackground());
	}

	@Test
	public void testReactToBackgroundConstructor() {
		WhenTouchingSpriteBrick brick = new WhenTouchingSpriteBrick(true);
		WhenTouchingSpriteScript script = (WhenTouchingSpriteScript) brick.getScript();
		assertEquals(true, script.isReactToBackground());
	}

	@Test
	public void testCloneCreatesIndependentScriptWithSameBackgroundReaction() throws CloneNotSupportedException {
		WhenTouchingSpriteBrick brick = new WhenTouchingSpriteBrick(true);
		WhenTouchingSpriteBrick clone = (WhenTouchingSpriteBrick) brick.clone();
		assertNotSame(brick.getScript(), clone.getScript());
		assertEquals(true, ((WhenTouchingSpriteScript) clone.getScript()).isReactToBackground());
		assertSame(clone, clone.getScript().getScriptBrick());
	}
}
