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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class PreloadSceneBrick extends BrickBaseType implements BrickSpinner.OnItemSelectedListener<Scene> {

	private static final long serialVersionUID = 1L;

	private String sceneToPreload;

	private transient BrickSpinner<Scene> spinner;

	public PreloadSceneBrick() {
		sceneToPreload = "";
	}

	public PreloadSceneBrick(String sceneToPreload) {
		this.sceneToPreload = sceneToPreload;
	}

	public String getSceneToPreload() {
		return sceneToPreload;
	}

	public void setSceneToPreload(String sceneToPreload) {
		this.sceneToPreload = sceneToPreload;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		PreloadSceneBrick clone = (PreloadSceneBrick) super.clone();
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_preload_scene;
	}

	@Override
	public View getView(final Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new NewOption(context.getString(R.string.new_option)));
		items.addAll(ProjectManager.getInstance().getCurrentProject().getSceneList());
		spinner = new BrickSpinner<>(R.id.preload_scene_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(sceneToPreload);

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
		sceneToPreload = item != null ? item.getName() : null;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createPreloadSceneAction(sprite, sequence, sceneToPreload));
	}
}
