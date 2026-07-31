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

public class Create3dJumpButtonBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public Create3dJumpButtonBrick() {
		addAllowedBrickField(BrickField.NAME, R.id.brick_create_3d_jump_object);
		addAllowedBrickField(BrickField.X_POSITION, R.id.brick_create_3d_jump_x);
		addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_create_3d_jump_y);
		addAllowedBrickField(BrickField.STRING_1, R.id.brick_create_3d_jump_active);
		addAllowedBrickField(BrickField.STRING_2, R.id.brick_create_3d_jump_inactive);
		addAllowedBrickField(BrickField.JUMP_POWER, R.id.brick_create_3d_jump_power);
	}

	public Create3dJumpButtonBrick(double posX, double posY, String activeFile, String inactiveFile, double power) {
		this("", posX, posY, activeFile, inactiveFile, power);
	}

	public Create3dJumpButtonBrick(String objectName, double posX, double posY, String activeFile, String inactiveFile, double power) {
		this(new Formula(objectName), new Formula(posX), new Formula(posY), new Formula(activeFile), new Formula(inactiveFile), new Formula(power));
	}

	public Create3dJumpButtonBrick(Formula objectName, Formula posX, Formula posY, Formula activeFile, Formula inactiveFile, Formula power) {
		this();
		setFormulaWithBrickField(BrickField.NAME, objectName);
		setFormulaWithBrickField(BrickField.X_POSITION, posX);
		setFormulaWithBrickField(BrickField.Y_POSITION, posY);
		setFormulaWithBrickField(BrickField.STRING_1, activeFile);
		setFormulaWithBrickField(BrickField.STRING_2, inactiveFile);
		setFormulaWithBrickField(BrickField.JUMP_POWER, power);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_3d_jump_button;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCreate3dJumpButtonAction(
				sprite,
				sequence,
				getFormulaWithBrickField(BrickField.NAME),
				getFormulaWithBrickField(BrickField.X_POSITION),
				getFormulaWithBrickField(BrickField.Y_POSITION),
				getFormulaWithBrickField(BrickField.STRING_1),
				getFormulaWithBrickField(BrickField.STRING_2),
				getFormulaWithBrickField(BrickField.JUMP_POWER)
		));
	}
}
