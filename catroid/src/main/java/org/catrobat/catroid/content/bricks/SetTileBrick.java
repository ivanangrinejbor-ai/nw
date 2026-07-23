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

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

/**
 * Sets the tile at (column, row) on the sprite's active tilemap costume to the given tile index.
 */
public class SetTileBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public SetTileBrick() {
		addAllowedBrickField(BrickField.TILEMAP_COLUMN, R.id.brick_set_tile_column);
		addAllowedBrickField(BrickField.TILEMAP_ROW, R.id.brick_set_tile_row);
		addAllowedBrickField(BrickField.TILEMAP_TILE_INDEX, R.id.brick_set_tile_index);
	}

	public SetTileBrick(int column, int row, int tileIndex) {
		this(new Formula(column), new Formula(row), new Formula(tileIndex));
	}

	public SetTileBrick(Formula column, Formula row, Formula tileIndex) {
		this();
		setFormulaWithBrickField(BrickField.TILEMAP_COLUMN, column);
		setFormulaWithBrickField(BrickField.TILEMAP_ROW, row);
		setFormulaWithBrickField(BrickField.TILEMAP_TILE_INDEX, tileIndex);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_tile;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetTileAction(
				sprite, sequence,
				getFormulaWithBrickField(BrickField.TILEMAP_COLUMN),
				getFormulaWithBrickField(BrickField.TILEMAP_ROW),
				getFormulaWithBrickField(BrickField.TILEMAP_TILE_INDEX)));
	}
}
