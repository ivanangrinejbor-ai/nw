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

public class PlayToneBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public PlayToneBrick() {
		addAllowedBrickField(BrickField.CUSTOM_PARAM_1, R.id.brick_play_tone_frequency);
		addAllowedBrickField(BrickField.DURATION, R.id.brick_play_tone_duration);
	}

	public PlayToneBrick(Float frequency, Float duration) {
		this(new Formula(frequency), new Formula(duration));
	}

	public PlayToneBrick(Formula frequency, Formula duration) {
		this();
		setFormulaWithBrickField(BrickField.CUSTOM_PARAM_1, frequency);
		setFormulaWithBrickField(BrickField.DURATION, duration);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_play_tone;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory()
				.createPlayToneAction(sprite, sequence,
						getFormulaWithBrickField(BrickField.CUSTOM_PARAM_1),
						getFormulaWithBrickField(BrickField.DURATION)));
	}
}
