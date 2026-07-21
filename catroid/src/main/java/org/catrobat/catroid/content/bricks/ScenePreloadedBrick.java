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

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class ScenePreloadedBrick extends BrickBaseType implements BrickSpinner.OnItemSelectedListener<Scene> {

	private static final long serialVersionUID = 1L;

	private String sceneToCheck;
	private UserVariable userVariable;

	private transient BrickSpinner<Scene> sceneSpinner;
	private transient BrickSpinner<UserVariable> variableSpinner;

	public ScenePreloadedBrick() {
		sceneToCheck = "";
	}

	public ScenePreloadedBrick(String sceneToCheck, UserVariable userVariable) {
		this.sceneToCheck = sceneToCheck;
		this.userVariable = userVariable;
	}

	public String getSceneToCheck() {
		return sceneToCheck;
	}

	public void setSceneToCheck(String sceneToCheck) {
		this.sceneToCheck = sceneToCheck;
	}

	public UserVariable getUserVariable() {
		return userVariable;
	}

	public void setUserVariable(UserVariable userVariable) {
		this.userVariable = userVariable;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		ScenePreloadedBrick clone = (ScenePreloadedBrick) super.clone();
		clone.sceneSpinner = null;
		clone.variableSpinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_scene_preloaded;
	}

	@Override
	public View getView(final Context context) {
		super.getView(context);

		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();

		List<Nameable> sceneItems = new ArrayList<>();
		sceneItems.add(new NewOption(context.getString(R.string.new_option)));
		sceneItems.addAll(ProjectManager.getInstance().getCurrentProject().getSceneList());
		sceneSpinner = new BrickSpinner<>(R.id.scene_preloaded_scene_spinner, view, sceneItems);
		sceneSpinner.setOnItemSelectedListener(this);
		sceneSpinner.setSelection(sceneToCheck);

		List<Nameable> variableItems = new ArrayList<>();
		variableItems.add(new NewOption(context.getString(R.string.new_option)));
		if (sprite != null) {
			variableItems.addAll(sprite.getUserVariables());
		}
		variableItems.addAll(ProjectManager.getInstance().getCurrentProject().getUserVariables());
		variableItems.addAll(ProjectManager.getInstance().getCurrentProject().getMultiplayerVariables());
		variableSpinner = new BrickSpinner<>(R.id.scene_preloaded_variable_spinner, view, variableItems);
		variableSpinner.setOnItemSelectedListener(new VariableSpinnerListener());
		variableSpinner.setSelection(userVariable);

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
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable Scene item) {
		sceneToCheck = item != null ? item.getName() : null;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createScenePreloadedAction(sprite, sequence, sceneToCheck, userVariable));
	}

	private class VariableSpinnerListener implements BrickSpinner.OnItemSelectedListener<UserVariable> {
		@Override
		public void onNewOptionSelected(Integer spinnerId) {
		}

		@Override
		public void onEditOptionSelected(Integer spinnerId) {
		}

		@Override
		public void onStringOptionSelected(Integer spinnerId, String string) {
		}

		@Override
		public void onItemSelected(Integer spinnerId, @Nullable UserVariable item) {
			userVariable = item;
		}
	}
}
