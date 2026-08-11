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
package org.catrobat.catroid.content;

import android.view.KeyEvent;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ConcurrentFormulaHashMap;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.VolumeButtonHoldBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.VolumeButtonHoldEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.ProjectManager;

public class VolumeButtonHoldScript extends Script {

	private static final long serialVersionUID = 1L;

	public static final int BUTTON_VOLUME_UP = KeyEvent.KEYCODE_VOLUME_UP;
	public static final int BUTTON_VOLUME_DOWN = KeyEvent.KEYCODE_VOLUME_DOWN;

	private int keyCode = BUTTON_VOLUME_UP;
	private ConcurrentFormulaHashMap formulaMap = new ConcurrentFormulaHashMap();

	public VolumeButtonHoldScript() {
		formulaMap.putIfAbsent(Brick.BrickField.VOLUME_HOLD_DURATION, new Formula(1.0f));
	}

	public VolumeButtonHoldScript(int keyCode, Formula durationFormula) {
		this();
		this.keyCode = keyCode;
		if (durationFormula != null) {
			formulaMap.replace(Brick.BrickField.VOLUME_HOLD_DURATION, durationFormula);
		}
	}

	public int getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode;
	}

	public ConcurrentFormulaHashMap getFormulaMap() {
		return formulaMap;
	}

	public Formula getDurationFormula() {
		return formulaMap.get(Brick.BrickField.VOLUME_HOLD_DURATION);
	}

	public void setDurationFormula(Formula durationFormula) {
		formulaMap.replace(Brick.BrickField.VOLUME_HOLD_DURATION, durationFormula);
	}

	public float getDuration(Sprite sprite) {
		Formula formula = getDurationFormula();
		if (formula == null) {
			return 1.0f;
		}
		try {
			if (sprite != null) {
				Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, null);
				return formula.interpretFloat(scope);
			}
			return formula.interpretFloat(null);
		} catch (Exception e) {
			try {
				String formulaString = formula.getTrimmedFormulaString(null);
				return Float.parseFloat(formulaString);
			} catch (Exception ex) {
				return 1.0f;
			}
		}
	}

	@Override
	public Script clone() throws CloneNotSupportedException {
		VolumeButtonHoldScript clone = (VolumeButtonHoldScript) super.clone();
		clone.formulaMap = formulaMap.clone();
		clone.keyCode = keyCode;
		return clone;
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new VolumeButtonHoldBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public void addRequiredResources(final Brick.ResourcesSet resourcesSet) {
		for (Formula formula : formulaMap.values()) {
			formula.addRequiredResources(resourcesSet);
		}
		for (Brick brick : brickList) {
			brick.addRequiredResources(resourcesSet);
		}
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new VolumeButtonHoldEventId(keyCode, getDurationFormula());
	}
}
