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

public class ClampPositionBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public ClampPositionBrick() {
		addAllowedBrickField(BrickField.X1, R.id.brick_clamp_position_min_x_edit_text);
		addAllowedBrickField(BrickField.X2, R.id.brick_clamp_position_max_x_edit_text);
		addAllowedBrickField(BrickField.Y1, R.id.brick_clamp_position_min_y_edit_text);
		addAllowedBrickField(BrickField.Y2, R.id.brick_clamp_position_max_y_edit_text);
	}

	public ClampPositionBrick(double minX, double maxX, double minY, double maxY) {
		this(new Formula(minX), new Formula(maxX), new Formula(minY), new Formula(maxY));
	}

	public ClampPositionBrick(Formula minX, Formula maxX, Formula minY, Formula maxY) {
		this();
		setFormulaWithBrickField(BrickField.X1, minX);
		setFormulaWithBrickField(BrickField.X2, maxX);
		setFormulaWithBrickField(BrickField.Y1, minY);
		setFormulaWithBrickField(BrickField.Y2, maxY);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_clamp_position;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createClampPositionAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.X1),
				getFormulaWithBrickField(BrickField.X2),
				getFormulaWithBrickField(BrickField.Y1),
				getFormulaWithBrickField(BrickField.Y2)));
	}
}
