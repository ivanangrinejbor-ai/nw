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

package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ConcurrentFormulaHashMap;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenFirebaseChangedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.WhenFirebaseChangedEventId;
import org.catrobat.catroid.formulaeditor.Formula;

public class WhenFirebaseChangedScript extends Script {

	private static final long serialVersionUID = 1L;

	private ConcurrentFormulaHashMap formulaMap = new ConcurrentFormulaHashMap();

	public WhenFirebaseChangedScript() {
		formulaMap.putIfAbsent(Brick.BrickField.FIREBASE_TRIGGER_BUCKET, new Formula(""));
		formulaMap.putIfAbsent(Brick.BrickField.FIREBASE_TRIGGER_PATH, new Formula(""));
	}

	public ConcurrentFormulaHashMap getFormulaMap() {
		return formulaMap;
	}

	@Override
	public Script clone() throws CloneNotSupportedException {
		WhenFirebaseChangedScript clone = (WhenFirebaseChangedScript) super.clone();
		clone.formulaMap = formulaMap.clone();
		return clone;
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenFirebaseChangedBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public void addRequiredResources(final Brick.ResourcesSet requiredResourcesSet) {
		for (Formula formula : formulaMap.values()) {
			formula.addRequiredResources(requiredResourcesSet);
		}
		for (Brick brick : brickList) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		WhenFirebaseChangedBrick brick = (WhenFirebaseChangedBrick) getScriptBrick();
		return new WhenFirebaseChangedEventId(
				brick.getFormulaWithBrickField(Brick.BrickField.FIREBASE_TRIGGER_BUCKET),
				brick.getFormulaWithBrickField(Brick.BrickField.FIREBASE_TRIGGER_PATH));
	}
}
