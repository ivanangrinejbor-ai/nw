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

public class Apply3dTorqueBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public Apply3dTorqueBrick() {
		addAllowedBrickField(BrickField.VALUE_1, R.id.brick_apply_3d_torque_edit_id);
		addAllowedBrickField(BrickField.VALUE_2, R.id.brick_apply_3d_torque_edit_x);
		addAllowedBrickField(BrickField.VALUE_3, R.id.brick_apply_3d_torque_edit_y);
		addAllowedBrickField(BrickField.VALUE_4, R.id.brick_apply_3d_torque_edit_z);
	}

	public Apply3dTorqueBrick(String objectId, double x, double y, double z) {
		this(new Formula(objectId), new Formula(x), new Formula(y), new Formula(z));
	}

	public Apply3dTorqueBrick(Formula objectId, Formula x, Formula y, Formula z) {
		this();
		setFormulaWithBrickField(BrickField.VALUE_1, objectId);
		setFormulaWithBrickField(BrickField.VALUE_2, x);
		setFormulaWithBrickField(BrickField.VALUE_3, y);
		setFormulaWithBrickField(BrickField.VALUE_4, z);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_apply_3d_torque;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory()
				.createApply3dTorqueAction(sprite, sequence,
						getFormulaWithBrickField(BrickField.VALUE_1),
						getFormulaWithBrickField(BrickField.VALUE_2),
						getFormulaWithBrickField(BrickField.VALUE_3),
						getFormulaWithBrickField(BrickField.VALUE_4)));
	}
}
