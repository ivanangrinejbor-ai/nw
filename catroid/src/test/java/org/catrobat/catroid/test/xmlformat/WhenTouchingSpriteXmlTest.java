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

package org.catrobat.catroid.test.xmlformat;

import org.catrobat.catroid.content.WhenTouchingSpriteByNameScript;
import org.catrobat.catroid.content.WhenTouchingSpriteScript;
import org.catrobat.catroid.content.bricks.WhenTouchingSpriteBrick;
import org.catrobat.catroid.content.bricks.WhenTouchingSpriteByNameBrick;
import org.catrobat.catroid.io.XstreamSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

@RunWith(JUnit4.class)
public class WhenTouchingSpriteXmlTest {

	@Test
	public void testUniversalScriptSerialization() {
		WhenTouchingSpriteScript script = new WhenTouchingSpriteScript(true);
		String xml = XstreamSerializer.getInstance().getXstream().toXML(script);
		assertThat(xml, startsWith("<script type=\"WhenTouchingSpriteScript\""));
		assertThat(xml, not(containsString("org.catrobat.catroid")));

		WhenTouchingSpriteScript restored = (WhenTouchingSpriteScript)
				XstreamSerializer.getInstance().getXstream().fromXML(xml);
		assertTrue(restored.isReactToBackground());
	}

	@Test
	public void testByNameScriptSerialization() {
		WhenTouchingSpriteByNameScript script = new WhenTouchingSpriteByNameScript("Player", true);
		String xml = XstreamSerializer.getInstance().getXstream().toXML(script);
		assertThat(xml, startsWith("<script type=\"WhenTouchingSpriteByNameScript\""));
		assertThat(xml, not(containsString("org.catrobat.catroid")));

		WhenTouchingSpriteByNameScript restored = (WhenTouchingSpriteByNameScript)
				XstreamSerializer.getInstance().getXstream().fromXML(xml);
		assertEquals("Player", restored.getSpriteToTouchName());
		assertTrue(restored.isReactToBackground());
	}

	@Test
	public void testUniversalBrickSerialization() {
		WhenTouchingSpriteBrick brick = new WhenTouchingSpriteBrick(new WhenTouchingSpriteScript(true));
		String xml = XstreamSerializer.getInstance().getXstream().toXML(brick);
		assertThat(xml, not(containsString("org.catrobat.catroid")));

		WhenTouchingSpriteBrick restored = (WhenTouchingSpriteBrick)
				XstreamSerializer.getInstance().getXstream().fromXML(xml);
		assertNotNull(restored);
		WhenTouchingSpriteScript restoredScript = (WhenTouchingSpriteScript) restored.getScript();
		assertTrue(restoredScript.isReactToBackground());
	}

	@Test
	public void testByNameBrickSerialization() {
		WhenTouchingSpriteByNameBrick brick = new WhenTouchingSpriteByNameBrick("Enemy");
		String xml = XstreamSerializer.getInstance().getXstream().toXML(brick);
		assertThat(xml, not(containsString("org.catrobat.catroid")));

		WhenTouchingSpriteByNameBrick restored = (WhenTouchingSpriteByNameBrick)
				XstreamSerializer.getInstance().getXstream().fromXML(xml);
		assertNotNull(restored);
		assertEquals("Enemy", ((WhenTouchingSpriteByNameScript) restored.getScript()).getSpriteToTouchName());
		assertFalse(((WhenTouchingSpriteByNameScript) restored.getScript()).isReactToBackground());
	}
}
