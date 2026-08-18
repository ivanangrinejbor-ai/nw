/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.SetVariableByNameBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class SetVariableByNameBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		sprite = new Sprite("Sprite");
	}

	@Test
	public void testNoArgsConstructorHasFormulas() {
		SetVariableByNameBrick brick = new SetVariableByNameBrick();
		assertNotNull(brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VARNAME));
		assertNotNull(brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VALUE));
	}

	@Test
	public void testStringConstructor() {
		SetVariableByNameBrick brick = new SetVariableByNameBrick("myVar", "42");
		assertEquals("myVar",
				brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VARNAME).getRoot().getValue());
		assertEquals("42",
				brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VALUE).getRoot().getValue());
	}

	@Test
	public void testFormulaConstructor() {
		SetVariableByNameBrick brick = new SetVariableByNameBrick(new Formula("myVar"), new Formula("42"));
		assertEquals("myVar",
				brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VARNAME).getRoot().getValue());
		assertEquals("42",
				brick.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VALUE).getRoot().getValue());
	}

	@Test
	public void testAddActionToSequenceUsesSetVariableByNameAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		SetVariableByNameBrick brick = new SetVariableByNameBrick("myVar", "42");

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createSetVariableByNameAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class), any(Formula.class));
	}

	@Test
	public void testClonePreservesFormulas() throws CloneNotSupportedException {
		SetVariableByNameBrick brick = new SetVariableByNameBrick("myVar", "42");
		SetVariableByNameBrick clone = (SetVariableByNameBrick) brick.clone();
		assertNotNull(clone);
		assertEquals("myVar",
				clone.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VARNAME).getRoot().getValue());
		assertEquals("42",
				clone.getFormulaWithBrickField(SetVariableByNameBrick.BrickField.VALUE).getRoot().getValue());
	}
}
