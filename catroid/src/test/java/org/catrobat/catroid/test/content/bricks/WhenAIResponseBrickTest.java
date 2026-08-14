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

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenAIResponseScript;
import org.catrobat.catroid.content.bricks.WhenAIResponseBrick;
import org.catrobat.catroid.content.eventids.AiResponseEventId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class WhenAIResponseBrickTest {

	@Test
	public void testBrickReturnsItsScript() {
		WhenAIResponseScript script = new WhenAIResponseScript("gemini");
		WhenAIResponseBrick brick = new WhenAIResponseBrick(script);
		assertSame(script, brick.getScript());
	}

	@Test
	public void testDefaultConstructorCreatesScriptWithAnyProvider() {
		WhenAIResponseBrick brick = new WhenAIResponseBrick();
		WhenAIResponseScript script = (WhenAIResponseScript) brick.getScript();
		assertEquals("", script.getProvider());
	}

	@Test
	public void testProviderConstructor() {
		WhenAIResponseBrick brick = new WhenAIResponseBrick("openai");
		WhenAIResponseScript script = (WhenAIResponseScript) brick.getScript();
		assertEquals("openai", script.getProvider());
	}

	@Test
	public void testCloneCreatesIndependentScriptWithSameProvider() throws CloneNotSupportedException {
		WhenAIResponseBrick brick = new WhenAIResponseBrick("deepseek");
		WhenAIResponseBrick clone = (WhenAIResponseBrick) brick.clone();
		assertNotSame(brick.getScript(), clone.getScript());
		assertEquals("deepseek", ((WhenAIResponseScript) clone.getScript()).getProvider());
		assertSame(clone, clone.getScript().getScriptBrick());
	}

	@Test
	public void testCreateEventIdMatchingProvider() {
		WhenAIResponseScript script = new WhenAIResponseScript("gemini");
		Sprite sprite = new Sprite("sprite");
		AiResponseEventId eventId = (AiResponseEventId) script.createEventId(sprite);
		assertEquals(sprite, eventId.sprite);
		assertEquals("gemini", eventId.provider);
	}

	@Test
	public void testEventIdEqualsWithWildcardAnyProvider() {
		Sprite sprite = new Sprite("sprite");
		AiResponseEventId any = new AiResponseEventId(sprite, "");
		AiResponseEventId gemini = new AiResponseEventId(sprite, "gemini");
		AiResponseEventId openai = new AiResponseEventId(sprite, "openai");
		assertEquals(any, gemini);
		assertEquals(any, openai);
		assertEquals(any, any);
		assertEquals(gemini, gemini);
		assertEquals(gemini, new AiResponseEventId(sprite, "gemini"));
		assertEquals(gemini.hashCode(), any.hashCode());
	}
}
