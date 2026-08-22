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
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class TintShaderBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public TintShaderBrick() {
		addAllowedBrickField(BrickField.RED, R.id.brick_tint_shader_red);
		addAllowedBrickField(BrickField.GREEN, R.id.brick_tint_shader_green);
		addAllowedBrickField(BrickField.BLUE, R.id.brick_tint_shader_blue);
		addAllowedBrickField(BrickField.INTENSITY, R.id.brick_tint_shader_amount);
	}

	public TintShaderBrick(double red, double green, double blue, double amount) {
		this(new Formula(red), new Formula(green), new Formula(blue), new Formula(amount));
	}

	public TintShaderBrick(Formula red, Formula green, Formula blue, Formula amount) {
		this();
		setFormulaWithBrickField(BrickField.RED, red);
		setFormulaWithBrickField(BrickField.GREEN, green);
		setFormulaWithBrickField(BrickField.BLUE, blue);
		setFormulaWithBrickField(BrickField.INTENSITY, amount);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_tint_shader;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createTintShaderAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.RED),
				getFormulaWithBrickField(BrickField.GREEN),
				getFormulaWithBrickField(BrickField.BLUE),
				getFormulaWithBrickField(BrickField.INTENSITY)));
	}
}
