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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Assigns scripts from a .neoscript file to an object in a specific Scene.
 *
 * Parameters:
 * <ul>
 *   <li>File path &ndash; .neoscript file path (Formula, evaluated at runtime)</li>
 *   <li>Object name &ndash; target object name (Formula, evaluated at runtime)</li>
 *   <li>Scene &ndash; target scene (spinner, stored as scene name; null/empty = Current scene)</li>
 *   <li>Replace existing scripts &ndash; Yes/No spinner (0 = keep existing + add imported; 1 = remove all existing + add imported)</li>
 * </ul>
 */
public class AssignScriptsBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Scene> {

	private static final long serialVersionUID = 1L;

	private String targetScene;
	private int replaceExistingSelection = 0; // 0 = keep existing + add imported; 1 = replace all existing scripts

	private transient BrickSpinner<Scene> sceneSpinner;

	public AssignScriptsBrick() {
		addAllowedBrickField(BrickField.ASSIGN_SCRIPTS_FILE, R.id.brick_assign_scripts_file_edit);
		addAllowedBrickField(BrickField.ASSIGN_SCRIPTS_OBJECT, R.id.brick_assign_scripts_object_edit);
	}

	public AssignScriptsBrick(String filePath, String objectName, String scene, boolean replaceExistingScripts) {
		this();
		setFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_FILE, new Formula(filePath));
		setFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_OBJECT, new Formula(objectName));
		this.targetScene = scene;
		this.replaceExistingSelection = replaceExistingScripts ? 1 : 0;
	}

	public AssignScriptsBrick(Formula filePath, Formula objectName, String scene, boolean replaceExistingScripts) {
		this();
		setFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_FILE, filePath);
		setFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_OBJECT, objectName);
		this.targetScene = scene;
		this.replaceExistingSelection = replaceExistingScripts ? 1 : 0;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		AssignScriptsBrick clone = (AssignScriptsBrick) super.clone();
		clone.sceneSpinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_assign_scripts;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		// Scene spinner
		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(context.getString(R.string.current_scene)));
		items.addAll(ProjectManager.getInstance().getCurrentProject().getSceneList());
		sceneSpinner = new BrickSpinner<>(R.id.brick_assign_scripts_scene_spinner, view, items);
		sceneSpinner.setOnItemSelectedListener(this);
		sceneSpinner.setSelection(targetScene != null ? targetScene : "");

		// Replace existing scripts spinner
		Spinner replaceSpinner = view.findViewById(R.id.brick_assign_scripts_replace_spinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
				R.layout.simple_spinner_item_white_text,
				new String[]{context.getString(R.string.no), context.getString(R.string.yes)});
		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
		replaceSpinner.setAdapter(adapter);
		replaceSpinner.setSelection(replaceExistingSelection);
		replaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				replaceExistingSelection = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		return view;
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
		if (string != null && string.equals(view.getContext().getString(R.string.current_scene))) {
			targetScene = null;
		}
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable Scene item) {
		targetScene = item != null ? item.getName() : null;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createAssignScriptsAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_FILE),
				getFormulaWithBrickField(BrickField.ASSIGN_SCRIPTS_OBJECT),
				targetScene,
				isReplaceExistingScripts()));
	}

	public boolean isReplaceExistingScripts() {
		return replaceExistingSelection == 1;
	}
}
