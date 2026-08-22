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
import org.catrobat.catroid.content.actions.ChangeWidthDirectionAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class ChangeWidthDirectionBrick extends FormulaBrick implements
		BrickSpinner.OnItemSelectedListener<ChangeWidthDirectionBrick.DirectionOption> {

	private static final long serialVersionUID = 1L;

	private int direction = ChangeWidthDirectionAction.DIRECTION_RIGHT;

	public ChangeWidthDirectionBrick() {
		addAllowedBrickField(BrickField.WIDTH, R.id.brick_change_width_direction_edit_text);
	}

	public ChangeWidthDirectionBrick(double size) {
		this(new Formula(size), ChangeWidthDirectionAction.DIRECTION_RIGHT);
	}

	public ChangeWidthDirectionBrick(double size, int direction) {
		this(new Formula(size), direction);
	}

	public ChangeWidthDirectionBrick(Formula formula) {
		this(formula, ChangeWidthDirectionAction.DIRECTION_RIGHT);
	}

	public ChangeWidthDirectionBrick(Formula formula, int direction) {
		this();
		setFormulaWithBrickField(BrickField.WIDTH, formula);
		this.direction = direction;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_change_width_direction;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new DirectionOption(context.getString(R.string.brick_direction_right), ChangeWidthDirectionAction.DIRECTION_RIGHT));
		items.add(new DirectionOption(context.getString(R.string.brick_direction_left), ChangeWidthDirectionAction.DIRECTION_LEFT));
		items.add(new DirectionOption(context.getString(R.string.brick_direction_center), ChangeWidthDirectionAction.DIRECTION_CENTER));

		BrickSpinner<DirectionOption> spinner = new BrickSpinner<>(R.id.brick_change_width_direction_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(direction);
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createChangeWidthDirectionAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.WIDTH, BrickField.SIZE), direction));
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
	public void onItemSelected(Integer spinnerId, @Nullable DirectionOption item) {
		if (item != null) {
			direction = item.getDirection();
		}
	}

	public static class DirectionOption implements Nameable {
		private String name;
		private int direction;

		public DirectionOption(String name, int direction) {
			this.name = name;
			this.direction = direction;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		public int getDirection() {
			return direction;
		}
	}
}
