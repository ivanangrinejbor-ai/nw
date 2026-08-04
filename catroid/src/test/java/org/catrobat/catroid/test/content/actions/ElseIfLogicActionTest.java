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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.content.actions;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.ElseIfBranch;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ElseIfLogicActionTest {

	private static final String TEST_USERVARIABLE = "testUservariable";
	private static final int IF_VALUE = 1;
	private static final int ELSEIF_VALUE = 2;
	private static final int ELSE_VALUE = 3;

	private Sprite testSprite;
	private StartScript testScript;
	private UserVariable userVariable;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "testProject");
		testSprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(testSprite);
		testSprite.removeAllScripts();
		testScript = new StartScript();
		testSprite.addScript(testScript);

		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentSprite(testSprite);

		project.removeUserVariable(TEST_USERVARIABLE);
		userVariable = new UserVariable(TEST_USERVARIABLE);
		project.addUserVariable(userVariable);
	}

	private SetVariableBrick setVar(int value) {
		return new SetVariableBrick(new Formula(value), userVariable);
	}

	@Test
	public void testIfBranchExecutesWhenConditionTrueAndElseIfSkipped() {
		IfLogicBeginBrick ifBrick = new IfLogicBeginBrick(new Formula(true));
		ifBrick.addBrickToIfBranch(setVar(IF_VALUE));

		ElseIfBranch branch = new ElseIfBranch(new Formula(true));
		branch.getBranchBricks().add(setVar(ELSEIF_VALUE));
		ifBrick.getElseIfBranches().add(branch);
		ifBrick.addBrickToElseBranch(setVar(ELSE_VALUE));
		testScript.addBrick(ifBrick);

		testSprite.initializeEventThreads(EventId.START);
		testSprite.look.act(100f);

		assertEquals((double) IF_VALUE, userVariable.getValue());
	}

	@Test
	public void testElseIfBranchExecutesWhenIfFalseAndElseIfTrue() {
		IfLogicBeginBrick ifBrick = new IfLogicBeginBrick(new Formula(false));
		ifBrick.addBrickToIfBranch(setVar(IF_VALUE));

		ElseIfBranch branch = new ElseIfBranch(new Formula(true));
		branch.getBranchBricks().add(setVar(ELSEIF_VALUE));
		ifBrick.getElseIfBranches().add(branch);
		ifBrick.addBrickToElseBranch(setVar(ELSE_VALUE));
		testScript.addBrick(ifBrick);

		testSprite.initializeEventThreads(EventId.START);
		testSprite.look.act(100f);

		assertEquals((double) ELSEIF_VALUE, userVariable.getValue());
	}

	@Test
	public void testElseBranchExecutesWhenAllConditionsFalse() {
		IfLogicBeginBrick ifBrick = new IfLogicBeginBrick(new Formula(false));
		ifBrick.addBrickToIfBranch(setVar(IF_VALUE));

		ElseIfBranch branch = new ElseIfBranch(new Formula(false));
		branch.getBranchBricks().add(setVar(ELSEIF_VALUE));
		ifBrick.getElseIfBranches().add(branch);
		ifBrick.addBrickToElseBranch(setVar(ELSE_VALUE));
		testScript.addBrick(ifBrick);

		testSprite.initializeEventThreads(EventId.START);
		testSprite.look.act(100f);

		assertEquals((double) ELSE_VALUE, userVariable.getValue());
	}

	@Test
	public void testMultipleElseIfPicksFirstTrueBranch() {
		IfLogicBeginBrick ifBrick = new IfLogicBeginBrick(new Formula(false));
		ifBrick.addBrickToIfBranch(setVar(IF_VALUE));

		ElseIfBranch first = new ElseIfBranch(new Formula(true));
		first.getBranchBricks().add(setVar(ELSEIF_VALUE));
		ifBrick.getElseIfBranches().add(first);

		ElseIfBranch second = new ElseIfBranch(new Formula(true));
		second.getBranchBricks().add(setVar(ELSE_VALUE));
		ifBrick.getElseIfBranches().add(second);
		ifBrick.addBrickToElseBranch(setVar(ELSE_VALUE));
		testScript.addBrick(ifBrick);

		testSprite.initializeEventThreads(EventId.START);
		testSprite.look.act(100f);

		assertEquals((double) ELSEIF_VALUE, userVariable.getValue());
	}

	@Test
	public void testNothingExecutesWhenAllFalseAndNoElse() {
		IfLogicBeginBrick ifBrick = new IfLogicBeginBrick(new Formula(false));
		ifBrick.addBrickToIfBranch(setVar(IF_VALUE));

		ElseIfBranch branch = new ElseIfBranch(new Formula(false));
		branch.getBranchBricks().add(setVar(ELSEIF_VALUE));
		ifBrick.getElseIfBranches().add(branch);
		testScript.addBrick(ifBrick);

		testSprite.initializeEventThreads(EventId.START);
		testSprite.look.act(100f);

		assertEquals(0.0, userVariable.getValue());
	}
}
