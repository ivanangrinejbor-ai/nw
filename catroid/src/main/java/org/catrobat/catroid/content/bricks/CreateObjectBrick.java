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
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import androidx.annotation.Nullable;

/**
 * Creates a new empty Sprite with the given name in the specified Scene.
 *
 * Parameters:
 * <ul>
 *   <li>Object name &ndash; name for the new sprite (Formula, evaluated at runtime)</li>
 *   <li>Scene &ndash; target scene (spinner, stored as scene name string; null/empty = Current scene)</li>
 * </ul>
 */
public class CreateObjectBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Scene> {

	private static final long serialVersionUID = 1L;

	private String targetScene;      // null or empty = Current scene

	private transient BrickSpinner<Scene> spinner;
	private int persistentSelection = 0; // 0 = runtime only, 1 = persist to project

	public CreateObjectBrick() {
		addAllowedBrickField(BrickField.CREATE_OBJECT_NAME, R.id.brick_create_object_name_edit);
	}

	public CreateObjectBrick(String objectName, String scene) {
		this();
		setFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME, new Formula(objectName));
		this.targetScene = scene;
	}

	public CreateObjectBrick(Formula objectName, String scene) {
		this();
		setFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME, objectName);
		this.targetScene = scene;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		CreateObjectBrick clone = (CreateObjectBrick) super.clone();
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_object;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(context.getString(R.string.current_scene)));
		items.addAll(ProjectManager.getInstance().getCurrentProject().getSceneList());

		spinner = new BrickSpinner<>(R.id.brick_create_object_scene_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(targetScene != null ? targetScene : "");

		// Persist to project spinner (No = runtime only, Yes = persist)
		Spinner persistentSpinner = view.findViewById(R.id.brick_create_object_persistent_spinner);
		ArrayAdapter<String> persistentAdapter = new ArrayAdapter<>(context,
				R.layout.simple_spinner_item_white_text,
				new String[]{context.getString(R.string.no), context.getString(R.string.yes)});
		persistentAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
		persistentSpinner.setAdapter(persistentAdapter);
		persistentSpinner.setSelection(persistentSelection);
		persistentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				persistentSelection = position;
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
		sequence.addAction(sprite.getActionFactory().createCreateObjectAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME),
				targetScene,
				isPersistent()));
	}

	public boolean isPersistent() {
		return persistentSelection == 1;
	}
}
