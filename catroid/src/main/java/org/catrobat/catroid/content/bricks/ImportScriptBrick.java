/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;

import java.lang.ref.WeakReference;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Imports a reusable script module (.neoscript file) into a target object at
 * runtime.
 *
 * Parameters:
 * <ul>
 *   <li>Object name   – name of the object that should receive the scripts</li>
 *   <li>File          – path / content uri of the .neoscript file</li>
 *   <li>Overwrite     – Yes/No: replace existing duplicates or skip them</li>
 * </ul>
 */
public class ImportScriptBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private int overwriteSelection = 0; // 0 = No, 1 = Yes

	// BUG-NS-01 fix: use WeakReference so the brick is not GC-rooted while the
	// file picker Activity is open. If the hosting Activity is destroyed during
	// the picker, the reference becomes null and onFilePicked() degrades
	// gracefully instead of leaking the entire view hierarchy.
	private static volatile WeakReference<ImportScriptBrick> pendingPickerBrickRef;

	public ImportScriptBrick() {
		addAllowedBrickField(BrickField.IMPORT_SCRIPT_OBJECT, R.id.brick_import_script_object_edit);
		addAllowedBrickField(BrickField.IMPORT_SCRIPT_FILE, R.id.brick_import_script_file_edit);
	}

	public ImportScriptBrick(String objectName, String filePath, boolean overwrite) {
		this();
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT, new Formula(objectName));
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, new Formula(filePath));
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	public ImportScriptBrick(Formula objectName, Formula filePath, boolean overwrite) {
		this();
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT, objectName);
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, filePath);
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	public boolean isOverwrite() {
		return overwriteSelection == 1;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_import_script;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		Spinner overwriteSpinner = view.findViewById(R.id.brick_import_script_overwrite_spinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
				R.layout.simple_spinner_item_white_text, new String[]{"No", "Yes"});
		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
		overwriteSpinner.setAdapter(adapter);
		overwriteSpinner.setSelection(overwriteSelection);
		overwriteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				overwriteSelection = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		android.widget.TextView fileEdit = view.findViewById(R.id.brick_import_script_file_edit);
		fileEdit.setOnClickListener(v -> openFilePicker(context));

		return view;
	}

	private void openFilePicker(Context context) {
		AppCompatActivity activity = UiUtils.getActivityFromView(view);
		if (activity == null) {
			return;
		}
		// BUG-NS-01: store as WeakReference so the brick does not prevent GC of the Activity
		pendingPickerBrickRef = new WeakReference<>(this);
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "text/xml"});
		activity.startActivityForResult(intent, org.catrobat.catroid.ui.SpriteActivity.REQUEST_NEO_SCRIPT_FILE);
	}

	/**
	 * Called by {@code SpriteActivity} once the user picked a .neoscript file.
	 */
	public static void onFilePicked(android.net.Uri uri) {
		WeakReference<ImportScriptBrick> ref = pendingPickerBrickRef;
		if (ref == null || uri == null) {
			return;
		}
		ImportScriptBrick brick = ref.get();
		pendingPickerBrickRef = null; // clear eagerly to allow GC
		if (brick == null) {
			return; // Activity was destroyed while picker was open
		}
		String path = uri.toString();
		brick.setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, new Formula(path));
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createImportScriptAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT),
				getFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE),
				isOverwrite()));
	}
}
