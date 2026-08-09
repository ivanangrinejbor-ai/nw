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

package org.catrobat.catroid.content.actions;

import android.util.Log;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class ForVariableFromToAction extends LoopAction {

	private static final int MAX_ITERATIONS = 10_000_000;
	
	private UserVariable controlVariable;
	private Formula from;
	private Formula to;
	private Scope scope;
	private boolean isCurrentLoopInitialized = false;
	private boolean isRepeatActionInitialized = false;
	private int fromValue;
	private int toValue;
	private int step = 1;
	private int iterationCount = 0;

	@Override
	protected boolean loopDelegate(float delta) {

		if (!isRepeatActionInitialized && !interpretParameters()) {
			return true;
		}

		if (!isCurrentLoopInitialized) {
			setCurrentTime(0.f);
			isCurrentLoopInitialized = true;
		}

		setCurrentTime(getCurrentTime() + delta);
		
		if (iterationCount >= MAX_ITERATIONS) {
			Log.w(getClass().getSimpleName(), "For loop exceeded maximum iterations (" + MAX_ITERATIONS + "), stopping");
			return true;
		}

		if (action != null && action.act(delta) && !isLoopDelayNeeded()) {
			iterationCount++;
			if (!(controlVariable.getValue() instanceof Double)
					|| (step > 0 && (double) controlVariable.getValue() >= toValue)
					|| (step < 0 && (double) controlVariable.getValue() <= toValue)) {
				return true;
			}
			changeControlVariable(step);
			isCurrentLoopInitialized = false;
			action.restart();
		}
		return false;
	}

	@Override
	public void restart() {
		isCurrentLoopInitialized = false;
		isRepeatActionInitialized = false;
		iterationCount = 0;
		super.restart();
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setRange(Formula from, Formula to) {
		this.from = from;
		this.to = to;
	}

	public void setControlVariable(UserVariable variable) {
		controlVariable = variable;
	}

	private boolean interpretParameters() {
		isRepeatActionInitialized = true;
		try {
			Double fromInterpretation = from == null ? Double.valueOf(0d)
					: from.interpretDouble(scope);
			Double toInterpretation = to == null ? Double.valueOf(0d) : to.interpretDouble(scope);
			
			if (fromInterpretation == null || fromInterpretation.isNaN() || fromInterpretation.isInfinite()) {
				fromInterpretation = 0d;
			}
			if (toInterpretation == null || toInterpretation.isNaN() || toInterpretation.isInfinite()) {
				toInterpretation = 0d;
			}
			
			fromValue = fromInterpretation.intValue();
			toValue = toInterpretation.intValue();
			setStepValue();
			setControlVariable(fromValue);
			return true;
		} catch (InterpretationException interpretationException) {
			Log.d(getClass().getSimpleName(), "Formula interpretation for this specific Brick failed.", interpretationException);
			return false;
		}
	}

	private void setStepValue() {
		step = (fromValue <= toValue) ? 1 : -1;
	}

	private void setControlVariable(int value) {
		controlVariable.setValue((double) value);
	}

	private void changeControlVariable(int value) {
		controlVariable.setValue((double) controlVariable.getValue() + (double) value);
	}
}
