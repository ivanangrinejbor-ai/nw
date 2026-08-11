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

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenTouchingSpriteScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenTouchingSpriteBrick extends ScriptBrickBaseType {

	private static final long serialVersionUID = 1L;

	private WhenTouchingSpriteScript script;

	public WhenTouchingSpriteBrick() {
		this(new WhenTouchingSpriteScript());
	}

	public WhenTouchingSpriteBrick(WhenTouchingSpriteScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	public WhenTouchingSpriteBrick(boolean reactToBackground) {
		this(new WhenTouchingSpriteScript(reactToBackground));
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenTouchingSpriteBrick clone = (WhenTouchingSpriteBrick) super.clone();
		clone.script = (WhenTouchingSpriteScript) script.clone();
		clone.script.setScriptBrick(clone);
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_touching_sprite;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		CheckBox backgroundCheckbox = view.findViewById(R.id.brick_when_touching_sprite_background_checkbox);
		backgroundCheckbox.setChecked(script.isReactToBackground());
		backgroundCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
				script.setReactToBackground(isChecked));

		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
