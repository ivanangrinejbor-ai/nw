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

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class WaitWhileAction extends Action {

	private boolean completed = false;
	private Formula condition;
	private Scope scope;
	private static final float LOOP_DELAY = 0.02f;
	private static final float TIMEOUT = 10.0f; // 10 seconds timeout
	private float currentTime = 0f;
	private float totalTime = 0f;
	private boolean hasError = false;

	public WaitWhileAction() {
	}

	public void setCondition(Formula condition) {
		this.condition = condition;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	@Override
	public boolean act(float delta) {
		if (scope == null) {
			return true; // Fail fast if no scope
		}
		if (completed || hasError) {
			return true;
		}

		currentTime += delta;
		totalTime += delta;

		// Timeout protection: stop waiting after 10 seconds
		if (totalTime >= TIMEOUT) {
			Log.w(getClass().getSimpleName(), "WaitWhile timeout exceeded (" + TIMEOUT + "s), stopping");
			return true;
		}

		if (currentTime < LOOP_DELAY) {
			return false;
		} else {
			currentTime = 0.0f;
		}

		try {
			// Wait WHILE the condition is true; finish as soon as it becomes false.
			completed = !condition.interpretBoolean(scope);
			hasError = false; // Clear error on success
		} catch (InterpretationException e) {
			// On error, complete the wait (don't hang forever)
			hasError = true;
			Log.w(getClass().getSimpleName(), "Formula interpretation failed, completing wait", e);
		}

		return completed || hasError;
	}

	@Override
	public void restart() {
		completed = false;
		hasError = false;
		currentTime = 0f;
		totalTime = 0f;
	}
}
