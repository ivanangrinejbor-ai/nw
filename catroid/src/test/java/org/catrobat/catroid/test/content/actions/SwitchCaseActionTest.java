/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.UserVariable;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SwitchCaseAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.catrobat.catroid.test.utils.Reflection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SwitchCaseActionTest {

	private Sprite testSprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "testProject");
		testSprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(testSprite);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentSprite(testSprite);
	}

	private SwitchCaseAction buildSwitch(Formula expression, List<Formula> cases) {
		List<ScriptSequenceAction> bodies = new ArrayList<>();
		for (int i = 0; i < cases.size(); i++) {
			bodies.add(new ScriptSequenceAction());
		}
		return (SwitchCaseAction) testSprite.getActionFactory()
				.createSwitchCaseAction(testSprite, new ScriptSequenceAction(), expression, cases, bodies);
	}

	@Test
	public void testNumericStringMatch() throws Exception {
		SwitchCaseAction action = buildSwitch(new Formula("1"), Arrays.asList(new Formula("1.0")));
		action.act(1f);
		Object matched = Reflection.getPrivateField(action, "matchedAction");
		assertNotNull("'1' should match case '1.0' numerically", matched);
	}

	@Test
	public void testStringMatch() throws Exception {
		SwitchCaseAction action = buildSwitch(new Formula("hello"), Arrays.asList(new Formula("hello")));
		action.act(1f);
		Object matched = Reflection.getPrivateField(action, "matchedAction");
		assertNotNull("string 'hello' should match case 'hello'", matched);
	}

	@Test
	public void testNoMatch() throws Exception {
		SwitchCaseAction action = buildSwitch(new Formula("1"), Arrays.asList(new Formula("2")));
		action.act(1f);
		Object matched = Reflection.getPrivateField(action, "matchedAction");
		assertNull("'1' should NOT match case '2'", matched);
	}

	@Test
	public void testNullScopeNoException() throws Exception {
		SwitchCaseAction action = buildSwitch(new Formula("1"), Arrays.asList(new Formula("1")));
		Reflection.setPrivateField(action, "scope", null);
		boolean finished = false;
		try {
			finished = action.act(1f);
		} catch (Throwable t) {
			org.junit.Assert.fail("act() threw with null scope: " + t);
		}
		assertTrue(finished);
	}
}
