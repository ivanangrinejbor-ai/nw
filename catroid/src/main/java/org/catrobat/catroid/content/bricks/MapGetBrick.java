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
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class MapGetBrick extends UserVariableBrickWithFormula {

	private static final long serialVersionUID = 1L;

	private UserVariable destVariable;
	private transient BrickSpinner<UserVariable> destSpinner;

	public MapGetBrick() {
		addAllowedBrickField(BrickField.KEY, R.id.brick_map_get_key);
	}

	public MapGetBrick(Formula key, UserVariable userVariable, UserVariable destVariable) {
		this();
		setFormulaWithBrickField(BrickField.KEY, key);
		this.userVariable = userVariable;
		this.destVariable = destVariable;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_map_get;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.brick_map_get_spinner;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		List<Nameable> items = new ArrayList<>();
		if (sprite != null) {
			items.addAll(sprite.getUserVariables());
		}
		items.addAll(ProjectManager.getInstance().getCurrentProject().getUserVariables());
		items.addAll(ProjectManager.getInstance().getCurrentProject().getMultiplayerVariables());

		destSpinner = new BrickSpinner<>(R.id.brick_map_get_dest_spinner, view, items);
		destSpinner.setOnItemSelectedListener(this);
		destSpinner.setSelection(destVariable);
		return view;
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable UserVariable item) {
		if (spinnerId == R.id.brick_map_get_dest_spinner) {
			destVariable = item;
		} else {
			super.onItemSelected(spinnerId, item);
		}
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		MapGetBrick clone = (MapGetBrick) super.clone();
		clone.destSpinner = null;
		return clone;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createMapGetAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.KEY), userVariable, destVariable));
	}
}
