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
import org.catrobat.catroid.content.actions.TryCatchFinallyAction;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TryCatchFinallyActionTest {

	private Sprite testSprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "testProject");
		testSprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(testSprite);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentSprite(testSprite);
	}

	private static class ThrowingAction extends Action {
		@Override
		public boolean act(float delta) {
			throw new RuntimeException("boom");
		}
	}

	@Test
	public void testExceptionInCatchAndFinallyIsHandled() {
		ScriptSequenceAction trySeq = new ScriptSequenceAction();
		trySeq.addAction(new ThrowingAction());
		ScriptSequenceAction catchSeq = new ScriptSequenceAction();
		catchSeq.addAction(new ThrowingAction());
		ScriptSequenceAction finallySeq = new ScriptSequenceAction();

		UserVariable errorVariable = new UserVariable("err");
		TryCatchFinallyAction action = (TryCatchFinallyAction) testSprite.getActionFactory()
				.createTryCatchFinallyAction(trySeq, catchSeq, finallySeq, errorVariable);

		boolean finished = false;
		try {
			for (int i = 0; i < 10 && !finished; i++) {
				finished = action.act(1f);
			}
		} catch (Throwable t) {
			org.junit.Assert.fail("TryCatchFinally leaked exception from CATCH/FINALLY: " + t);
		}
		assertTrue("action should finish without crashing", finished);
		assertNotNull("errorVariable should have captured the exception message", errorVariable.getValue());
	}
}
