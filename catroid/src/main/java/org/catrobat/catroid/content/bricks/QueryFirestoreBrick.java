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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class QueryFirestoreBrick extends UserVariableBrickWithFormula {

	private static final long serialVersionUID = 1L;
	private int waitForResponseSelection = 0;
	private String operatorSelection = "=";

	public QueryFirestoreBrick() {
		addAllowedBrickField(BrickField.FIRESTORE_BASE, R.id.brick_query_firestore_edit_base);
		addAllowedBrickField(BrickField.FIRESTORE_COLLECTION, R.id.brick_query_firestore_edit_collection);
		addAllowedBrickField(BrickField.FIRESTORE_FIELD, R.id.brick_query_firestore_edit_field);
		addAllowedBrickField(BrickField.FIRESTORE_VALUE, R.id.brick_query_firestore_edit_value);
		addAllowedBrickField(BrickField.FIRESTORE_LIMIT, R.id.brick_query_firestore_edit_limit);
	}

	public QueryFirestoreBrick(String collection, String field, String operator, String value, String limit) {
		this("", collection, field, operator, value, limit);
	}

	public QueryFirestoreBrick(String base, String collection, String field, String operator, String value, String limit) {
		this(new Formula(base), new Formula(collection), new Formula(field), new Formula(value), new Formula(limit));
		this.operatorSelection = operator;
	}

	public QueryFirestoreBrick(Formula collection, Formula field, String operator, Formula value, Formula limit) {
		this(new Formula(""), collection, field, value, limit);
		this.operatorSelection = operator;
	}

	public QueryFirestoreBrick(Formula base, Formula collection, Formula field, Formula value, Formula limit) {
		this();
		setFormulaWithBrickField(BrickField.FIRESTORE_BASE, base);
		setFormulaWithBrickField(BrickField.FIRESTORE_COLLECTION, collection);
		setFormulaWithBrickField(BrickField.FIRESTORE_FIELD, field);
		setFormulaWithBrickField(BrickField.FIRESTORE_VALUE, value);
		setFormulaWithBrickField(BrickField.FIRESTORE_LIMIT, limit);
	}

	public QueryFirestoreBrick(Formula collection, Formula field, String operator, Formula value, Formula limit,
							   UserVariable userVariable, int waitForResponseSelection) {
		this(collection, field, operator, value, limit);
		this.userVariable = userVariable;
		this.waitForResponseSelection = waitForResponseSelection;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		Spinner operatorSpinner = view.findViewById(R.id.brick_query_firestore_operator_spinner);
		ArrayAdapter<CharSequence> operatorAdapter = ArrayAdapter.createFromResource(context,
				R.array.firestore_operator_options, android.R.layout.simple_spinner_item);
		operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		operatorSpinner.setAdapter(operatorAdapter);
		operatorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> parent, View itemView, int position, long id) {
				operatorSelection = context.getResources().getStringArray(R.array.firestore_operator_options)[position];
			}
			@Override public void onNothingSelected(AdapterView<?> parent) { }
		});
		int operatorIndex = operatorAdapter.getPosition(operatorSelection);
		operatorSpinner.setSelection(operatorIndex >= 0 ? operatorIndex : 0);

		Spinner waitSpinner = view.findViewById(R.id.brick_query_firestore_wait_spinner);
		ArrayAdapter<CharSequence> waitAdapter = ArrayAdapter.createFromResource(context,
				R.array.firebase_wait_options, android.R.layout.simple_spinner_item);
		waitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		waitSpinner.setAdapter(waitAdapter);
		waitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> parent, View itemView, int position, long id) {
				waitForResponseSelection = position;
			}
			@Override public void onNothingSelected(AdapterView<?> parent) { }
		});
		waitSpinner.setSelection(waitForResponseSelection);
		return view;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_query_firestore;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.brick_query_firestore_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createQueryFirestoreAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.FIRESTORE_BASE),
				getFormulaWithBrickField(BrickField.FIRESTORE_COLLECTION),
				getFormulaWithBrickField(BrickField.FIRESTORE_FIELD),
				operatorSelection,
				getFormulaWithBrickField(BrickField.FIRESTORE_VALUE),
				getFormulaWithBrickField(BrickField.FIRESTORE_LIMIT),
				userVariable, waitForResponseSelection == 0));
	}
}