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

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.tilemap.TilemapRuntime;
import org.catrobat.catroid.content.tilemap.TilemapRuntimeManager;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class SetTilemapSolidAction extends TemporalAction {

	private Scope scope;
	private Formula tileIndex;
	private Formula solid;

	@Override
	protected void update(float delta) {
		if (scope == null) {
			return;
		}
		LookData look = scope.getSprite().look.getLookData();
		if (!(look instanceof TilemapLookData)) {
			return;
		}
		TilemapLookData tilemap = (TilemapLookData) look;
		try {
			int idx = tileIndex == null ? 0 : tileIndex.interpretFloat(scope).intValue();
			float solidValue = solid == null ? 0f : solid.interpretFloat(scope);
			boolean isSolid = solidValue != 0f;
			if (tilemap.isSolidTile(idx) != isSolid) {
				tilemap.setTileSolid(idx, isSolid);
				TilemapRuntime runtime = TilemapRuntimeManager.peek(tilemap);
				if (runtime != null) {
					runtime.invalidatePhysics();
				}
			}
		} catch (InterpretationException e) {
			Log.d(getClass().getSimpleName(), "Formula interpretation failed.", e);
		}
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setTileIndex(Formula tileIndex) {
		this.tileIndex = tileIndex;
	}

	public void setSolid(Formula solid) {
		this.solid = solid;
	}
}
