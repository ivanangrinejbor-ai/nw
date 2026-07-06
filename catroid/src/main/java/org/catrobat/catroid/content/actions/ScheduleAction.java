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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class ScheduleAction extends Action {
	private Scope scope;
	private Formula delay;
	private Action scheduledAction;
	private boolean initialized;
	private boolean scheduled;

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setDelay(Formula delay) {
		this.delay = delay;
	}

	public void setScheduledAction(Action scheduledAction) {
		this.scheduledAction = scheduledAction;
	}

	@Override
	public boolean act(float delta) {
		if (!initialized) {
			initialized = true;
			if (scope == null || delay == null || scheduledAction == null) {
				return true;
			}
			try {
				float delaySeconds = delay.interpretFloat(scope);
				long delayMillis = (long) (delaySeconds * 1000);
				Handler handler = new Handler(Looper.getMainLooper());
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						scheduled = true;
					}
				}, delayMillis);
			} catch (InterpretationException e) {
				Log.d(getClass().getSimpleName(), "Formula interpretation failed", e);
				return true;
			}
		}
		if (scheduled) {
			if (scheduledAction != null) {
				return scheduledAction.act(delta);
			}
			return true;
		}
		return false;
	}

	@Override
	public void restart() {
		super.restart();
		initialized = false;
		scheduled = false;
		if (scheduledAction != null) {
			scheduledAction.restart();
		}
	}
}
