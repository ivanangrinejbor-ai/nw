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

package org.catrobat.catroid.content.bricks.brickspinner;

import android.app.Dialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;

import org.catrobat.catroid.R;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog;
import org.catrobat.catroid.ui.recyclerview.dialog.textwatcher.DuplicateInputTextWatcher;
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment;
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider;
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment;
import org.catrobat.catroid.utils.ToastUtil;

import androidx.appcompat.app.AppCompatActivity;

public class UserVariableBrickTextInputDialogBuilder extends TextInputDialog.Builder {

	public UserVariableBrickTextInputDialogBuilder(Project project, Sprite sprite, UserVariable currentUserVariable,
			AppCompatActivity activity, BrickSpinner<UserVariable> spinner) {
		super(activity);
		View dialogView = View.inflate(activity, R.layout.dialog_new_user_data, null);
		RadioButton multiplayerRadioButton = dialogView.findViewById(R.id.multiplayer);
		if (SettingsFragment.isMultiplayerVariablesPreferenceEnabled(activity.getApplicationContext())) {
			multiplayerRadioButton.setVisibility(View.VISIBLE);
		}

		RadioButton sceneRadioButton = dialogView.findViewById(R.id.scene);
		Scene editedScene = ProjectManager.getInstance().getCurrentlyEditedScene();
		if (editedScene != null && !editedScene.isGlobalScene()) {
			sceneRadioButton.setVisibility(View.VISIBLE);
		}

		CheckBox protectedCheckBox = dialogView.findViewById(R.id.protected_var);
		View passwordInput = dialogView.findViewById(R.id.password_input);
		protectedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			passwordInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
		});

		setView(dialogView);

		setHint(activity.getString(R.string.data_label))
				.setTextWatcher(new DuplicateInputTextWatcher<>(spinner.getItems()))
				.setPositiveButton(activity.getString(R.string.ok), (TextInputDialog.OnClickListener) (dialog, textInput) -> {
					UserVariable userVariable = new UserVariable(textInput);

					RadioButton addToProjectVariablesRadioButton = ((Dialog) dialog).findViewById(R.id.global);
					boolean addToProjectVariables = addToProjectVariablesRadioButton.isChecked();
					boolean addToMultiplayerVariables = multiplayerRadioButton.isChecked();
					boolean addToSceneVariables = sceneRadioButton.isChecked();

					if (addToProjectVariables) {
						project.addUserVariable(userVariable);
					} else if (addToMultiplayerVariables) {
						project.addMultiplayerVariable(userVariable);
					} else if (addToSceneVariables && editedScene != null) {
						editedScene.addSceneVariable(userVariable);
					} else {
						sprite.addUserVariable(userVariable);
					}

					if (protectedCheckBox.isChecked()) {
						EditText passwordEditText = ((Dialog) dialog).findViewById(R.id.password_edit_text);
						String password = passwordEditText.getText().toString();
						if (password.isEmpty()) {
							ToastUtil.showError(activity, R.string.brick_password_empty);
						} else {
							userVariable.setLock(password);
							ToastUtil.showSuccess(activity, R.string.variable_locked);
							ToastUtil.showError(activity, R.string.variable_lock_warning);
						}
					}

					spinner.add(userVariable);
					spinner.setSelection(userVariable);

					ScriptFragment parentFragment = (ScriptFragment) activity
							.getSupportFragmentManager().findFragmentByTag(ScriptFragment.TAG);
					if (parentFragment != null) {
						parentFragment.notifyDataSetChanged();
					}
				});

		setTitle(R.string.formula_editor_variable_dialog_title);
		UniqueNameProvider uniqueNameProvider = createUniqueNameProvider(R.string.default_variable_name);
		setText(uniqueNameProvider.getUniqueName(activity.getString(R.string.default_variable_name), null));

		setNegativeButton(R.string.cancel, (dialog, which) -> spinner.setSelection(currentUserVariable));
		setOnCancelListener(dialog -> spinner.setSelection(currentUserVariable));
	}
}
