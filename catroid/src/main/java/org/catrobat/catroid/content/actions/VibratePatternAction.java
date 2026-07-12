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

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.stage.StageActivity;

public class VibratePatternAction extends TemporalAction {
	private Scope scope;
	private Formula pattern;

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setPattern(Formula pattern) {
		this.pattern = pattern;
	}

	@Override
	protected void begin() {
		if (scope == null) return;
		try {
			String patternStr = pattern == null ? "" : pattern.interpretString(scope);
			if (patternStr.isEmpty()) {
				return;
			}
			String[] parts = patternStr.split(",");
			long[] patternArray = new long[parts.length];
			long totalDuration = 0;
			for (int i = 0; i < parts.length; i++) {
				try {
					long val = Long.parseLong(parts[i].trim());
					if (val < 0) val = 0; // negative values would crash VibrationEffect
					patternArray[i] = val;
					totalDuration += val;
				} catch (NumberFormatException nfe) {
					Log.d(getClass().getSimpleName(), "Invalid vibration pattern value: '" + parts[i].trim() + "'", nfe);
					return;
				}
			}
			if (totalDuration <= 0) return;
			super.setDuration(totalDuration / 1000f);
			Context context = StageActivity.activeStageActivity.get();
			if (context == null) {
				return;
			}
			Vibrator vibrator;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
				vibrator = vibratorManager.getDefaultVibrator();
			} else {
				vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			}
			if (vibrator != null && vibrator.hasVibrator()) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					vibrator.vibrate(VibrationEffect.createWaveform(patternArray, -1));
				} else {
					vibrator.vibrate(patternArray, -1);
				}
			}
		} catch (InterpretationException e) {
			Log.d(getClass().getSimpleName(), "Formula interpretation for this specific Brick failed.", e);
		}
	}

	@Override
	protected void update(float percent) {
		if (scope == null) return;
	}
}
