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

public class FirebaseSignInEmailPasswordBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;
	private int waitForResponseSelection = 0;

	public FirebaseSignInEmailPasswordBrick() {
		addAllowedBrickField(BrickField.FIREBASE_EMAIL, R.id.brick_firebase_sign_in_email_password_edit_email);
		addAllowedBrickField(BrickField.FIREBASE_PASSWORD, R.id.brick_firebase_sign_in_email_password_edit_password);
	}

	public FirebaseSignInEmailPasswordBrick(String email, String password) {
		this(new Formula(email), new Formula(password));
	}

	public FirebaseSignInEmailPasswordBrick(Formula email, Formula password) {
		this();
		setFormulaWithBrickField(BrickField.FIREBASE_EMAIL, email);
		setFormulaWithBrickField(BrickField.FIREBASE_PASSWORD, password);
	}

	public FirebaseSignInEmailPasswordBrick(String email, String password, int waitForResponseSelection) {
		this(email, password);
		this.waitForResponseSelection = waitForResponseSelection;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		Spinner spinner = view.findViewById(R.id.brick_firebase_sign_in_email_password_wait_spinner);
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
		return R.layout.brick_firebase_sign_in_email_password;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createFirebaseSignInEmailPasswordAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.FIREBASE_EMAIL),
				getFormulaWithBrickField(BrickField.FIREBASE_PASSWORD),
				waitForResponseSelection == 0));
	}
}