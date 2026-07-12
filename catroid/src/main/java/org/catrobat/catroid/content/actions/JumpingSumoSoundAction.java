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

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class JumpingSumoSoundAction extends TemporalAction {

	private static final String TAG = JumpingSumoSoundAction.class.getSimpleName();

	private String soundName;
	private Formula volume;
	private Scope scope;

	@Override
	protected void update(float percent) {
		if (scope == null) return;
		try {
			float volumeValue = volume == null ? 0f : volume.interpretFloat(scope);
			Log.i(TAG, "Jumping Sumo play sound: " + soundName
					+ ", volume=" + volumeValue + "% (hardware not available)");
		} catch (InterpretationException exception) {
			Log.e(TAG, "Formula interpretation failed", exception);
		}
	}

	public void setSoundName(String soundName) {
		this.soundName = soundName;
	}

	public void setVolume(Formula volume) {
		this.volume = volume;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}
}
