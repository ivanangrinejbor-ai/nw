/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits)
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

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenFirebaseChildChangedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

public class WhenFirebaseChildChangedBrick extends FormulaBrick implements ScriptBrick {

	private static final long serialVersionUID = 1L;

	private WhenFirebaseChildChangedScript script;

	public WhenFirebaseChildChangedBrick() {
		this(new WhenFirebaseChildChangedScript());
	}

	public WhenFirebaseChildChangedBrick(WhenFirebaseChildChangedScript script) {
		addAllowedBrickField(BrickField.FIREBASE_TRIGGER_BUCKET, R.id.brick_when_firebase_child_changed_bucket);
		addAllowedBrickField(BrickField.FIREBASE_TRIGGER_PATH, R.id.brick_when_firebase_child_changed_path);
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;

		formulaMap = script.getFormulaMap();
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		Spinner spinner = view.findViewById(R.id.brick_when_firebase_child_changed_event_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
				R.array.firebase_child_event_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> parent, View itemView, int position, long id) {
				script.setEventTypeSelection(position);
			}
			@Override public void onNothingSelected(AdapterView<?> parent) { }
		});
		spinner.setSelection(script.getEventTypeSelection());
		return view;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenFirebaseChildChangedBrick clone = (WhenFirebaseChildChangedBrick) super.clone();
		clone.script = (WhenFirebaseChildChangedScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.formulaMap = clone.script.getFormulaMap();
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_firebase_child_changed;
	}

	public Formula getBucketFormula() {
		return getFormulaWithBrickField(BrickField.FIREBASE_TRIGGER_BUCKET);
	}

	public Formula getPathFormula() {
		return getFormulaWithBrickField(BrickField.FIREBASE_TRIGGER_PATH);
	}

	public int getEventTypeSelection() {
		return script.getEventTypeSelection();
	}

	public void setEventTypeSelection(int eventTypeSelection) {
		script.setEventTypeSelection(eventTypeSelection);
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
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}