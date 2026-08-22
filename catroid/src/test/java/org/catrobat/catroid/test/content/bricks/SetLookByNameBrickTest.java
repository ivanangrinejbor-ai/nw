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

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SetLookByNameAction;
import org.catrobat.catroid.content.bricks.SetLookByNameBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.catrobat.catroid.test.StaticSingletonInitializer.initializeStaticSingletonMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(JUnit4.class)
public class SetLookByNameBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() {
		initializeStaticSingletonMethods();
		sprite = new Sprite("TestSprite");
	}

	@Test
	public void testSetLookByNameBrickWiring() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		SetLookByNameBrick brick = new SetLookByNameBrick("costume1");

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createSetLookByNameAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class));
	}

	@Test
	public void testSetLookByNameActionExecution() {
		LookData look1 = Mockito.mock(LookData.class);
		Mockito.when(look1.getName()).thenReturn("idle");
		LookData look2 = Mockito.mock(LookData.class);
		Mockito.when(look2.getName()).thenReturn("walk");

		sprite.getLookList().add(look1);
		sprite.getLookList().add(look2);

		Action action = sprite.getActionFactory().createSetLookByNameAction(sprite,
				new SequenceAction(), new Formula("walk"));
		assertTrue(action instanceof SetLookByNameAction);
	}
}
