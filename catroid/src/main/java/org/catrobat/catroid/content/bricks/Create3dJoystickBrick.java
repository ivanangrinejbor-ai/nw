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

public class Create3dJoystickBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public Create3dJoystickBrick() {
		addAllowedBrickField(BrickField.NAME, R.id.brick_create_3d_joystick_object);
		addAllowedBrickField(BrickField.X_POSITION, R.id.brick_create_3d_joystick_x);
		addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_create_3d_joystick_y);
		addAllowedBrickField(BrickField.STRING_1, R.id.brick_create_3d_joystick_bg);
		addAllowedBrickField(BrickField.STRING_2, R.id.brick_create_3d_joystick_thumb);
		addAllowedBrickField(BrickField.SPEED, R.id.brick_create_3d_joystick_speed);
	}

	public Create3dJoystickBrick(double posX, double posY, String bgFile, String thumbFile, double speed) {
		this("", posX, posY, bgFile, thumbFile, speed);
	}

	public Create3dJoystickBrick(String objectName, double posX, double posY, String bgFile, String thumbFile, double speed) {
		this(new Formula(objectName), new Formula(posX), new Formula(posY), new Formula(bgFile), new Formula(thumbFile), new Formula(speed));
	}

	public Create3dJoystickBrick(Formula objectName, Formula posX, Formula posY, Formula bgFile, Formula thumbFile, Formula speed) {
		this();
		setFormulaWithBrickField(BrickField.NAME, objectName);
		setFormulaWithBrickField(BrickField.X_POSITION, posX);
		setFormulaWithBrickField(BrickField.Y_POSITION, posY);
		setFormulaWithBrickField(BrickField.STRING_1, bgFile);
		setFormulaWithBrickField(BrickField.STRING_2, thumbFile);
		setFormulaWithBrickField(BrickField.SPEED, speed);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_3d_joystick;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCreate3dJoystickAction(
				sprite,
				sequence,
				getFormulaWithBrickField(BrickField.NAME),
				getFormulaWithBrickField(BrickField.X_POSITION),
				getFormulaWithBrickField(BrickField.Y_POSITION),
				getFormulaWithBrickField(BrickField.STRING_1),
				getFormulaWithBrickField(BrickField.STRING_2),
				getFormulaWithBrickField(BrickField.SPEED)
		));
	}
}
