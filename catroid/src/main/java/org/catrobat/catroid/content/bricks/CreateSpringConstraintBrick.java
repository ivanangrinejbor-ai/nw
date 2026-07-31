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
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateSpringConstraintBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public CreateSpringConstraintBrick() {
		addAllowedBrickField(BrickField.VALUE_1, R.id.brick_spring_constraint_edit_id);
		addAllowedBrickField(BrickField.VALUE_2, R.id.brick_spring_constraint_edit_a);
		addAllowedBrickField(BrickField.VALUE_3, R.id.brick_spring_constraint_edit_b);
		addAllowedBrickField(BrickField.VALUE_4, R.id.brick_spring_constraint_edit_pax);
		addAllowedBrickField(BrickField.VALUE_5, R.id.brick_spring_constraint_edit_pay);
		addAllowedBrickField(BrickField.VALUE_6, R.id.brick_spring_constraint_edit_paz);
		addAllowedBrickField(BrickField.VALUE_7, R.id.brick_spring_constraint_edit_pbx);
		addAllowedBrickField(BrickField.VALUE_8, R.id.brick_spring_constraint_edit_pby);
		addAllowedBrickField(BrickField.VALUE_9, R.id.brick_spring_constraint_edit_pbz);
	}

	public CreateSpringConstraintBrick(String constraintId, String objectA, String objectB) {
		this();
		setFormulaWithBrickField(BrickField.VALUE_1, new Formula(constraintId));
		setFormulaWithBrickField(BrickField.VALUE_2, new Formula(objectA));
		setFormulaWithBrickField(BrickField.VALUE_3, new Formula(objectB));
		setFormulaWithBrickField(BrickField.VALUE_4, new Formula(0));
		setFormulaWithBrickField(BrickField.VALUE_5, new Formula(0));
		setFormulaWithBrickField(BrickField.VALUE_6, new Formula(0));
		setFormulaWithBrickField(BrickField.VALUE_7, new Formula(0));
		setFormulaWithBrickField(BrickField.VALUE_8, new Formula(0));
		setFormulaWithBrickField(BrickField.VALUE_9, new Formula(0));
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_spring_constraint;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory()
				.createSpringConstraintAction(sprite, sequence,
						getFormulaWithBrickField(BrickField.VALUE_1),
						getFormulaWithBrickField(BrickField.VALUE_2),
						getFormulaWithBrickField(BrickField.VALUE_3),
						getFormulaWithBrickField(BrickField.VALUE_4),
						getFormulaWithBrickField(BrickField.VALUE_5),
						getFormulaWithBrickField(BrickField.VALUE_6),
						getFormulaWithBrickField(BrickField.VALUE_7),
						getFormulaWithBrickField(BrickField.VALUE_8),
						getFormulaWithBrickField(BrickField.VALUE_9)));
	}
}
