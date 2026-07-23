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
 * Marks (or unmarks) a tileset index as solid on the sprite's active tilemap costume.
 * The {@code solid} formula is interpreted as a boolean: 0 = not solid, non-zero = solid.
 */
public class SetTilemapSolidBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public SetTilemapSolidBrick() {
		addAllowedBrickField(BrickField.TILEMAP_TILE_INDEX, R.id.brick_set_tilemap_solid_index);
		addAllowedBrickField(BrickField.TILEMAP_SOLID, R.id.brick_set_tilemap_solid_value);
	}

	public SetTilemapSolidBrick(int tileIndex, boolean solid) {
		this(new Formula(tileIndex), new Formula(solid ? 1 : 0));
	}

	public SetTilemapSolidBrick(Formula tileIndex, Formula solid) {
		this();
		setFormulaWithBrickField(BrickField.TILEMAP_TILE_INDEX, tileIndex);
		setFormulaWithBrickField(BrickField.TILEMAP_SOLID, solid);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_tilemap_solid;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetTilemapSolidAction(
				sprite, sequence,
				getFormulaWithBrickField(BrickField.TILEMAP_TILE_INDEX),
				getFormulaWithBrickField(BrickField.TILEMAP_SOLID)));
	}
}
