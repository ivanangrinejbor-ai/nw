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

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.AdapterViewOnItemSelectedListenerImpl;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import kotlin.Unit;

public class SlideOutBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private int edgeSelection = 0;

	public SlideOutBrick() {
		addAllowedBrickField(BrickField.SPEED, R.id.brick_slide_out_edit_text);
	}

	public SlideOutBrick(double speed) {
		this(new Formula(speed));
	}

	public SlideOutBrick(Formula formula) {
		this();
		setFormulaWithBrickField(BrickField.SPEED, formula);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_slide_out;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		Spinner spinner = view.findViewById(R.id.brick_slide_out_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
				R.array.transition_edges, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(edgeSelection);
		spinner.setOnItemSelectedListener(new AdapterViewOnItemSelectedListenerImpl(position -> {
			edgeSelection = position;
			return Unit.INSTANCE;
		}));

		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSlideOutAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.SPEED), edgeSelection));
	}
}
