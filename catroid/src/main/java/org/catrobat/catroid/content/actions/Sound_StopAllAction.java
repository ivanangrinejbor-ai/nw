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

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.audio.MidiServiceHolder;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.content.bricks.Sound_StopAllBrick;

public class Sound_StopAllAction extends TemporalAction {

	private long replayBlockMillis = 3000L;

	public void setReplayMode(int replayMode) {
		if (replayMode == Sound_StopAllBrick.MODE_NEVER_REPLAY) {
			replayBlockMillis = Long.MAX_VALUE;
		} else if (replayMode == Sound_StopAllBrick.MODE_WAIT_3_SECONDS) {
			replayBlockMillis = 3000L;
		} else {
			replayBlockMillis = 0L;
		}
	}

	@Override
	protected void update(float percent) {
		SoundManager.getInstance().stopAllSounds(replayBlockMillis);
		MidiServiceHolder.midiService.stopAllSounds();
	}
}
