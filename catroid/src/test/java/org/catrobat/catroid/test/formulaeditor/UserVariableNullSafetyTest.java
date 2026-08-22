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

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UserVariableNullSafetyTest {

	@Test
	public void testHasSameValueWithNullTransientValue() {
		UserVariable loaded = new UserVariable("var");
		loaded.setValue(null);
		UserVariable saved = new UserVariable("var", 5d);

		assertFalse(loaded.hasSameValue(saved));
		assertFalse(saved.hasSameValue(loaded));
		assertTrue(loaded.hasSameValue(new UserVariable("other", null)));
	}

	@Test
	public void testHasSameValueNullVariable() {
		UserVariable variable = new UserVariable("var", 1d);
		assertTrue(variable.hasSameValue(null) == false || variable.getValue() == null);
		assertFalse(variable.hasSameValue(null));
	}

	@Test
	public void testEqualsWithNullName() {
		UserVariable unnamed = new UserVariable();
		unnamed.setName(null);
		UserVariable named = new UserVariable("var", 0d);

		assertTrue(unnamed.equals(new UserVariable()));
		assertFalse(unnamed.equals(named));
		assertFalse(named.equals(unnamed));
	}

	@Test
	public void testProjectHasUserDataChangedNullLists() {
		List<UserVariable> variables = new ArrayList<>(Arrays.asList(new UserVariable("a", 1d)));

		assertTrue(new Project().hasUserDataChanged(variables, null));
		assertTrue(new Project().hasUserDataChanged(null, variables));
		assertFalse(new Project().hasUserDataChanged(null, null));
	}

	@Test
	public void testSpriteHasUserDataChangedNullLists() {
		Sprite sprite = new Sprite("sprite");
		List<UserVariable> variables = new ArrayList<>(Arrays.asList(new UserVariable("a", 1d)));

		assertTrue(sprite.hasUserDataChanged(variables, null));
		assertTrue(sprite.hasUserDataChanged(null, variables));
		assertFalse(sprite.hasUserDataChanged(null, null));
	}

	@Test
	public void testUserListHasSameListSizeWithNullTransientList() {
		UserList loaded = new UserList("list");
		loaded.setValue(null);
		UserList saved = new UserList("list", Arrays.asList(1, 2, 3));

		assertFalse(loaded.hasSameListSize(saved));
		assertFalse(saved.hasSameListSize(loaded));
	}
}
