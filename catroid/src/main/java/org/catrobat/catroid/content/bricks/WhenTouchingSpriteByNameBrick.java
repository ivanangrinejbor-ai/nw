/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2025 The Catrobat Team
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
import android.widget.CheckBox;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenTouchingSpriteByNameScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class WhenTouchingSpriteByNameBrick extends ScriptBrickBaseType
		implements BrickSpinner.OnItemSelectedListener<Sprite> {

	private static final long serialVersionUID = 1L;

	private static final String ANYTHING_ESCAPE_CHAR = "\0";

	private WhenTouchingSpriteByNameScript script;

	private transient BrickSpinner<Sprite> spinner;

	public WhenTouchingSpriteByNameBrick() {
		this(new WhenTouchingSpriteByNameScript());
	}

	public WhenTouchingSpriteByNameBrick(WhenTouchingSpriteByNameScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	public WhenTouchingSpriteByNameBrick(String spriteToTouchName) {
		this(new WhenTouchingSpriteByNameScript(spriteToTouchName));
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenTouchingSpriteByNameBrick clone = (WhenTouchingSpriteByNameBrick) super.clone();
		clone.script = (WhenTouchingSpriteByNameScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_touching_sprite_by_name;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(ANYTHING_ESCAPE_CHAR + context.getString(R.string.touching_sprite_anything)
				+ ANYTHING_ESCAPE_CHAR));
		items.addAll(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());

		spinner = new BrickSpinner<>(R.id.brick_when_touching_sprite_by_name_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(script.getSpriteToTouchName());

		CheckBox backgroundCheckbox = view.findViewById(R.id.brick_when_touching_sprite_by_name_background_checkbox);
		backgroundCheckbox.setChecked(script.isReactToBackground());
		backgroundCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
				script.setReactToBackground(isChecked));

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
		script.setSpriteToTouchName("");
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable Sprite item) {
		script.setSpriteToTouchName(item != null ? item.getName() : "");
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
