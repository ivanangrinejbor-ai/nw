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

import org.catrobat.catroid.content.WhenTouchingSpriteByNameScript;
import org.catrobat.catroid.content.bricks.WhenTouchingSpriteByNameBrick;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class WhenTouchingSpriteByNameBrickTest {

	@Test
	public void testBrickReturnsItsScript() {
		WhenTouchingSpriteByNameScript script = new WhenTouchingSpriteByNameScript();
		WhenTouchingSpriteByNameBrick brick = new WhenTouchingSpriteByNameBrick(script);
		assertSame(script, brick.getScript());
	}

	@Test
	public void testNameConstructor() {
		WhenTouchingSpriteByNameBrick brick = new WhenTouchingSpriteByNameBrick("Player");
		WhenTouchingSpriteByNameScript script = (WhenTouchingSpriteByNameScript) brick.getScript();
		assertEquals("Player", script.getSpriteToTouchName());
		assertEquals(false, script.isReactToBackground());
	}

	@Test
	public void testCloneCreatesIndependentScriptWithSameName() throws CloneNotSupportedException {
		WhenTouchingSpriteByNameBrick brick = new WhenTouchingSpriteByNameBrick("Enemy");
		WhenTouchingSpriteByNameBrick clone = (WhenTouchingSpriteByNameBrick) brick.clone();
		assertNotSame(brick.getScript(), clone.getScript());
		assertEquals("Enemy", ((WhenTouchingSpriteByNameScript) clone.getScript()).getSpriteToTouchName());
		assertSame(clone, clone.getScript().getScriptBrick());
	}
}
