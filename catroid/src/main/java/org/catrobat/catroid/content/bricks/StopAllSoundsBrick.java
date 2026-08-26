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
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;

import java.util.ArrayList;
import java.util.List;

public class StopAllSoundsBrick extends BrickBaseType implements BrickSpinner.OnItemSelectedListener<StringOption> {

	private static final long serialVersionUID = 1L;

	public static final int MODE_WAIT_3_SECONDS = 0;
	public static final int MODE_NEVER_REPLAY = 1;

	private int replayMode = MODE_WAIT_3_SECONDS;

	private transient BrickSpinner<StringOption> modeSpinner;

	public StopAllSoundsBrick() {
	}

	public StopAllSoundsBrick(int replayMode) {
		this.replayMode = replayMode;
	}

	public int getReplayMode() {
		return replayMode;
	}

	public void setReplayMode(int replayMode) {
		this.replayMode = replayMode;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		StopAllSoundsBrick clone = (StopAllSoundsBrick) super.clone();
		clone.modeSpinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_stop_all_sounds;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new StringOption(context.getString(R.string.brick_stop_all_mode_replay_wait)));
		items.add(new StringOption(context.getString(R.string.brick_stop_all_mode_never_replay)));
		modeSpinner = new BrickSpinner<>(R.id.brick_stop_all_sounds_mode_spinner, view, items);
		modeSpinner.setOnItemSelectedListener(this);
		int position = replayMode == MODE_NEVER_REPLAY ? 1 : 0;
		modeSpinner.setSelection(position);

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
		if (string == null || view == null) {
			return;
		}
		Context context = view.getContext();
		if (context.getString(R.string.brick_stop_all_mode_never_replay).equals(string)) {
			replayMode = MODE_NEVER_REPLAY;
		} else if (context.getString(R.string.brick_stop_all_mode_replay_wait).equals(string)) {
			replayMode = MODE_WAIT_3_SECONDS;
		}
	}

	@Override
	public void onItemSelected(Integer spinnerId, StringOption item) {
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createStopAllSoundsAction(replayMode));
	}
}
