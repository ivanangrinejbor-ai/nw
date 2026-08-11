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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.VolumeButtonHoldScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VolumeButtonHoldBrick extends FormulaBrick implements ScriptBrick, BrickSpinner.OnItemSelectedListener<StringOption> {

	private static final long serialVersionUID = 1L;

	private VolumeButtonHoldScript script;

	public VolumeButtonHoldBrick() {
		this(new VolumeButtonHoldScript());
	}

	public VolumeButtonHoldBrick(@NonNull VolumeButtonHoldScript script) {
		addAllowedBrickField(BrickField.VOLUME_HOLD_DURATION, R.id.brick_volume_button_hold_edit_text);
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
		formulaMap = script.getFormulaMap();
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		VolumeButtonHoldBrick clone = (VolumeButtonHoldBrick) super.clone();
		clone.script = (VolumeButtonHoldScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.formulaMap = clone.script.getFormulaMap();
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_volume_button_hold;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(context.getString(R.string.brick_when_button_pressed_up)));
		items.add(new StringOption(context.getString(R.string.brick_when_button_pressed_down)));

		BrickSpinner<StringOption> spinner = new BrickSpinner<>(R.id.brick_volume_button_hold_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		if (script.getKeyCode() == VolumeButtonHoldScript.BUTTON_VOLUME_UP) {
			spinner.setSelection(context.getString(R.string.brick_when_button_pressed_up));
		} else {
			spinner.setSelection(context.getString(R.string.brick_when_button_pressed_down));
		}

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
		Context viewContext = view.getContext();
		if (string.equals(viewContext.getString(R.string.brick_when_button_pressed_down))) {
			script.setKeyCode(VolumeButtonHoldScript.BUTTON_VOLUME_DOWN);
		} else {
			script.setKeyCode(VolumeButtonHoldScript.BUTTON_VOLUME_UP);
		}
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable StringOption item) {
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public int getPositionInScript() {
		return -1;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		super.addToFlatList(bricks);
		for (Brick brick : getScript().getBrickList()) {
			brick.addToFlatList(bricks);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return getScript().getBrickList();
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return -1;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		getScript().setCommentedOut(commentedOut);
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
