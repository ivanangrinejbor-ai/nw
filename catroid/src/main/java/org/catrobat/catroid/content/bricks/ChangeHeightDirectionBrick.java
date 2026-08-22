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
import org.catrobat.catroid.content.actions.ChangeHeightDirectionAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class ChangeHeightDirectionBrick extends FormulaBrick implements
		BrickSpinner.OnItemSelectedListener<ChangeHeightDirectionBrick.DirectionOption> {

	private static final long serialVersionUID = 1L;

	private int direction = ChangeHeightDirectionAction.DIRECTION_UP;

	public ChangeHeightDirectionBrick() {
		addAllowedBrickField(BrickField.HEIGHT, R.id.brick_change_height_direction_edit_text);
	}

	public ChangeHeightDirectionBrick(double size) {
		this(new Formula(size), ChangeHeightDirectionAction.DIRECTION_UP);
	}

	public ChangeHeightDirectionBrick(double size, int direction) {
		this(new Formula(size), direction);
	}

	public ChangeHeightDirectionBrick(Formula formula) {
		this(formula, ChangeHeightDirectionAction.DIRECTION_UP);
	}

	public ChangeHeightDirectionBrick(Formula formula, int direction) {
		this();
		setFormulaWithBrickField(BrickField.HEIGHT, formula);
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
		return R.layout.brick_change_height_direction;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.add(new DirectionOption(context.getString(R.string.brick_direction_up), ChangeHeightDirectionAction.DIRECTION_UP));
		items.add(new DirectionOption(context.getString(R.string.brick_direction_down), ChangeHeightDirectionAction.DIRECTION_DOWN));
		items.add(new DirectionOption(context.getString(R.string.brick_direction_center), ChangeHeightDirectionAction.DIRECTION_CENTER));

		BrickSpinner<DirectionOption> spinner = new BrickSpinner<>(R.id.brick_change_height_direction_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(direction);
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createChangeHeightDirectionAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.HEIGHT, BrickField.SIZE), direction));
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
