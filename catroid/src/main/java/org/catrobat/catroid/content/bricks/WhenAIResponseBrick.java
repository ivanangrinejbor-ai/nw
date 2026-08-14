/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
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

import org.catrobat.catroid.R;
import org.catrobat.catroid.ai.model.AiProvider;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenAIResponseScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;

import java.util.ArrayList;
import java.util.List;

public class WhenAIResponseBrick extends ScriptBrickBaseType
		implements BrickSpinner.OnItemSelectedListener<StringOption> {

	private static final long serialVersionUID = 1L;

	private static final String ANY_ESCAPE_CHAR = "\0";

	private WhenAIResponseScript script;

	private transient BrickSpinner<StringOption> spinner;

	public WhenAIResponseBrick() {
		this(new WhenAIResponseScript());
	}

	public WhenAIResponseBrick(WhenAIResponseScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	public WhenAIResponseBrick(String provider) {
		this(new WhenAIResponseScript(provider));
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenAIResponseBrick clone = (WhenAIResponseBrick) super.clone();
		clone.script = (WhenAIResponseScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_ai_response;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(ANY_ESCAPE_CHAR + context.getString(R.string.ai_provider_any)
				+ ANY_ESCAPE_CHAR));
		for (AiProvider provider : AiProvider.values()) {
			items.add(new StringOption(provider.getId()));
		}

		spinner = new BrickSpinner<>(R.id.brick_when_ai_response_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(script.getProvider());

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
		if (string == null || string.startsWith(ANY_ESCAPE_CHAR) || string.isEmpty()) {
			script.setProvider("");
		} else {
			script.setProvider(string);
		}
	}

	@Override
	public void onItemSelected(Integer spinnerId, StringOption item) {
		if (item == null || item.getName() == null || item.getName().startsWith(ANY_ESCAPE_CHAR)) {
			script.setProvider("");
		} else {
			script.setProvider(item.getName());
		}
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
