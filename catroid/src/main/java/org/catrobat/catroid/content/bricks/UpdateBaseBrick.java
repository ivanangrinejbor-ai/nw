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

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class UpdateBaseBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;
	private int waitForResponseSelection = 0;

	public UpdateBaseBrick() {
		addAllowedBrickField(BrickField.FIREBASE_ID, R.id.brick_update_base_edit_base);
		addAllowedBrickField(BrickField.FIREBASE_KEY, R.id.brick_update_base_edit_key);
		addAllowedBrickField(BrickField.FIREBASE_VALUE, R.id.brick_update_base_edit_value);
	}

	public UpdateBaseBrick(String base, String key, String value) {
		this(new Formula(base), new Formula(key), new Formula(value));
	}

	public UpdateBaseBrick(Formula base, Formula key, Formula value) {
		this();
		setFormulaWithBrickField(BrickField.FIREBASE_ID, base);
		setFormulaWithBrickField(BrickField.FIREBASE_KEY, key);
		setFormulaWithBrickField(BrickField.FIREBASE_VALUE, value);
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		Spinner spinner = view.findViewById(R.id.brick_update_base_wait_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
				R.array.firebase_wait_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> parent, View itemView, int position, long id) {
				waitForResponseSelection = position;
			}
			@Override public void onNothingSelected(AdapterView<?> parent) { }
		});
		spinner.setSelection(waitForResponseSelection);
		return view;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_update_base;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createUpdateBaseAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.FIREBASE_ID), getFormulaWithBrickField(BrickField.FIREBASE_KEY),
				getFormulaWithBrickField(BrickField.FIREBASE_VALUE), waitForResponseSelection == 0));
	}
}