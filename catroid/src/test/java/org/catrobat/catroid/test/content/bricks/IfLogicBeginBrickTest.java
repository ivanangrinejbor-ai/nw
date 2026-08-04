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
package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ElseIfBranch;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class IfLogicBeginBrickTest {

	private IfLogicBeginBrick newIfBrick() {
		return new IfLogicBeginBrick(new Formula("1 == 1"));
	}

	private ElseIfBranch newElseIfBranch(Formula condition) {
		return new ElseIfBranch(condition);
	}

	@Test
	public void testConsistsOfMultipleParts() {
		assertTrue(newIfBrick().consistsOfMultipleParts());
	}

	@Test
	public void testHasSecondaryList() {
		assertTrue(newIfBrick().hasSecondaryList());
	}

	@Test
	public void testAllPartsWithoutElseIfContainer() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		List<Brick> parts = ifBrick.getAllParts();
		assertEquals(3, parts.size());
		assertSame(ifBrick, parts.get(0));
		assertTrue(parts.get(1).getClass().getSimpleName().contains("Else"));
		assertTrue(parts.get(2).getClass().getSimpleName().contains("End"));
	}

	@Test
	public void testAllPartsOrderWithElseIfBranches() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ifBrick.addElseIfBranch();
		ifBrick.addElseIfBranch();

		List<Brick> parts = ifBrick.getAllParts();
		assertEquals(5, parts.size());
		assertSame(ifBrick, parts.get(0));
		assertTrue(parts.get(1).getClass().getSimpleName().contains("ElseIf"));
		assertTrue(parts.get(2).getClass().getSimpleName().contains("ElseIf"));
		assertTrue(parts.get(3).getClass().getSimpleName().contains("Else"));
		assertTrue(parts.get(4).getClass().getSimpleName().contains("End"));
	}

	@Test
	public void testElseIfBranchDefaultConditionIsTrue() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ifBrick.addElseIfBranch();
		ElseIfBranch branch = ifBrick.getElseIfBranches().get(0);
		assertNotNull(branch);
		assertNotNull(branch.getCondition());
	}

	@Test
	public void testRemoveElseIfBranchRemovesContentAndSeparator() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ElseIfBranch branch = newElseIfBranch(new Formula("2 == 2"));
		branch.getBranchBricks().add(new SetVariableBrick(2.0));
		ifBrick.getElseIfBranches().add(branch);
		assertEquals(1, ifBrick.getElseIfBranches().size());

		ifBrick.removeElseIfBranch(branch);
		assertEquals(0, ifBrick.getElseIfBranches().size());
		List<Brick> parts = ifBrick.getAllParts();
		assertEquals(3, parts.size());
	}

	@Test
	public void testFlatListContainsBranchesAndMarkers() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ifBrick.addBrickToIfBranch(new SetVariableBrick(1.0));

		ElseIfBranch firstElseIf = newElseIfBranch(new Formula("2 == 2"));
		firstElseIf.getBranchBricks().add(new SetVariableBrick(2.0));
		ifBrick.getElseIfBranches().add(firstElseIf);

		ElseIfBranch secondElseIf = newElseIfBranch(new Formula("3 == 3"));
		secondElseIf.getBranchBricks().add(new SetVariableBrick(3.0));
		ifBrick.getElseIfBranches().add(secondElseIf);

		ifBrick.addBrickToElseBranch(new SetVariableBrick(4.0));

		List<Brick> flat = new ArrayList<>();
		ifBrick.addToFlatList(flat);

		// [if, thenBrick, sep1, branch1 brick, sep2, branch2 brick, else, elseBrick, end]
		assertEquals(9, flat.size());
		assertSame(ifBrick, flat.get(0));
		assertTrue(flat.get(1) instanceof SetVariableBrick);
		assertTrue(flat.get(2).getClass().getSimpleName().contains("ElseIf"));
		assertTrue(flat.get(3) instanceof SetVariableBrick);
		assertTrue(flat.get(4).getClass().getSimpleName().contains("ElseIf"));
		assertTrue(flat.get(5) instanceof SetVariableBrick);
		assertTrue(flat.get(6).getClass().getSimpleName().contains("Else"));
		assertTrue(flat.get(7) instanceof SetVariableBrick);
		assertTrue(flat.get(8).getClass().getSimpleName().contains("End"));
	}

	@Test
	public void testSetParentPropagatesToAllBranches() throws Exception {
		IfLogicBeginBrick ifBrick = newIfBrick();
		Brick ifChild = new SetVariableBrick(1.0);
		Brick elseChild = new SetVariableBrick(2.0);
		Brick elseIfChild = new SetVariableBrick(3.0);
		ifBrick.addBrickToIfBranch(ifChild);
		ifBrick.addBrickToElseBranch(elseChild);

		ElseIfBranch branch = newElseIfBranch(new Formula("2 == 2"));
		branch.getBranchBricks().add(elseIfChild);
		ifBrick.getElseIfBranches().add(branch);

		ifBrick.setParent(null);

		assertSame(ifBrick, ifChild.getParent());
		assertSame(ifBrick.getAllParts().get(2), elseChild.getParent());
		assertSame(branch.getSeparatorBrick(ifBrick), elseIfChild.getParent());
	}

	@Test
	public void testClonePreservesAllBranches() throws Exception {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ifBrick.addBrickToIfBranch(new SetVariableBrick(1.0));
		ElseIfBranch branch = newElseIfBranch(new Formula("2 == 2"));
		branch.getBranchBricks().add(new SetVariableBrick(2.0));
		ifBrick.getElseIfBranches().add(branch);
		ifBrick.addBrickToElseBranch(new SetVariableBrick(3.0));

		IfLogicBeginBrick clone = (IfLogicBeginBrick) ifBrick.clone();
		assertEquals(1, clone.getNestedBricks().size());
		assertEquals(1, clone.getSecondaryNestedBricks().size());
		assertEquals(1, clone.getElseIfBranches().size());
		assertEquals(1, clone.getElseIfBranches().get(0).getBranchBricks().size());
		assertEquals(4, clone.getAllParts().size());
	}

	@Test
	public void testSetCommentedOutPropagatesToBranches() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		ifBrick.addBrickToIfBranch(new SetVariableBrick(1.0));
		ElseIfBranch branch = newElseIfBranch(new Formula("2 == 2"));
		branch.getBranchBricks().add(new SetVariableBrick(2.0));
		ifBrick.getElseIfBranches().add(branch);
		ifBrick.addBrickToElseBranch(new SetVariableBrick(3.0));

		ifBrick.setCommentedOut(true);
		assertTrue(ifBrick.getNestedBricks().get(0).isCommentedOut());
		assertTrue(ifBrick.getSecondaryNestedBricks().get(0).isCommentedOut());
		assertTrue(ifBrick.getElseIfBranches().get(0).getBranchBricks().get(0).isCommentedOut());
	}

	@Test
	public void testRemoveChildFromElseIfBranch() {
		IfLogicBeginBrick ifBrick = newIfBrick();
		SetVariableBrick child = new SetVariableBrick(2.0);
		ElseIfBranch branch = newElseIfBranch(new Formula("2 == 2"));
		branch.getBranchBricks().add(child);
		ifBrick.getElseIfBranches().add(branch);

		assertTrue(ifBrick.removeChild(child));
		assertEquals(0, branch.getBranchBricks().size());
	}
}
