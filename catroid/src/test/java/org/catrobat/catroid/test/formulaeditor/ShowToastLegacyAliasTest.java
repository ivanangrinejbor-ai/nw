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
package org.catrobat.catroid.test.formulaeditor;

import org.catrobat.catroid.content.bricks.ShowToastBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.io.XstreamSerializer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ShowToastLegacyAliasTest {

	private static final String LEGACY_XML = "<org.catrobat.catroid.content.Sprite>"
			+ "  <scriptList>"
			+ "    <org.catrobat.catroid.content.StartScript>"
			+ "      <brickList>"
			+ "        <brick type=\"ShowToastBlock\">"
			+ "          <formulaMap/>"
			+ "        </brick>"
			+ "      </brickList>"
			+ "    </org.catrobat.catroid.content.StartScript>"
			+ "  </scriptList>"
			+ "  <lookList/>"
			+ "  <soundList/>"
			+ "</org.catrobat.catroid.content.Sprite>";

	@Test
	public void testShowToastBlockRemappedToShowToastBrick() {
		XstreamSerializer serializer = XstreamSerializer.getInstance();
		Object result = serializer.getXstream().fromXML(LEGACY_XML);

		assertTrue(result instanceof org.catrobat.catroid.content.Sprite);
		org.catrobat.catroid.content.Sprite sprite = (org.catrobat.catroid.content.Sprite) result;
		assertEquals(1, sprite.getScriptList().size());

		Object brick = sprite.getScriptList().get(0).getBrickList().get(0);
		assertEquals(ShowToastBrick.class, brick.getClass());
	}

	@Test
	public void testNewShowToastBrickRoundTrip() {
		XstreamSerializer serializer = XstreamSerializer.getInstance();
		ShowToastBrick brick = new ShowToastBrick(new Formula("Hello"));

		String xml = serializer.getXstream().toXML(brick);
		assertTrue(xml.contains("type=\"ShowToastBrick\""));

		Object restored = serializer.getXstream().fromXML(xml);
		assertEquals(ShowToastBrick.class, restored.getClass());
	}
}
