/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
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

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class FlipLookBrick extends BrickBaseType implements
		BrickSpinner.OnItemSelectedListener<FlipLookBrick.FlipOption> {

	private static final long serialVersionUID = 1L;

	public static final int FLIP_HORIZONTAL = 0;
	public static final int FLIP_VERTICAL = 1;

	private int flipMode = FLIP_HORIZONTAL;

	public FlipLookBrick() {
	}

	public FlipLookBrick(int flipMode) {
		this.flipMode = flipMode;
	}

	public int getFlipMode() {
		return flipMode;
	}

	public void setFlipMode(int flipMode) {
		this.flipMode = flipMode;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_flip_look;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new FlipOption(context.getString(R.string.brick_flip_horizontal), FLIP_HORIZONTAL));
		items.add(new FlipOption(context.getString(R.string.brick_flip_vertical), FLIP_VERTICAL));

		BrickSpinner<FlipOption> spinner = new BrickSpinner<>(R.id.brick_flip_look_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(flipMode);
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createFlipLookAction(sprite, sequence, flipMode == FLIP_HORIZONTAL));
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable FlipOption item) {
		if (item != null) {
			flipMode = item.getFlipMode();
		}
	}

	public static class FlipOption implements Nameable {
		private String name;
		private final int mode;

		public FlipOption(String name, int mode) {
			this.name = name;
			this.mode = mode;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		public int getFlipMode() {
			return mode;
		}
	}
}
