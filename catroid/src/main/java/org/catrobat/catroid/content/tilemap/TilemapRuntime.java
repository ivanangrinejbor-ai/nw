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
package org.catrobat.catroid.content.tilemap;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;

import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.physics.TilemapPhysicsBuilder;

public class TilemapRuntime {

	private final TilemapLookData data;

	private Texture tilesetTexture;
	private TextureRegion[] tileRegions;
	private static Texture dummyTexture;
	private static volatile TextureRegion dummyRegion;
	private volatile boolean regionsDirty = true;

	private Body body;
	private volatile boolean physicsDirty = true;

	public TilemapRuntime(TilemapLookData data) {
		this.data = data;
	}

	public TilemapLookData getData() {
		return data;
	}

	private static TextureRegion getDummyRegion() {
		TextureRegion cached = dummyRegion;
		if (cached != null) return cached;
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(0, 0, 0, 0);
		p.fill();
		dummyTexture = new Texture(p);
		p.dispose();
		dummyRegion = new TextureRegion(dummyTexture);
		return dummyRegion;
	}

	public TextureRegion[] getRegions() {
		if (regionsDirty || tileRegions == null) {
			regionsDirty = false;
			sliceRegions();
		}
		return tileRegions;
	}

	public TextureRegion getRegion(int tileIndex) {
		if (tileIndex < 0) {
			return getDummyRegion();
		}
		TextureRegion[] regions = getRegions();
		if (regions == null || tileIndex >= regions.length || regions[tileIndex] == null) {
			return getDummyRegion();
		}
		return regions[tileIndex];
	}

	private void sliceRegions() {
		disposeTexture();
		Pixmap srcPixmap = data.getPixmap();
		if (srcPixmap == null) {
			tileRegions = new TextureRegion[0];
			return;
		}
		Pixmap pixmap = new Pixmap(srcPixmap.getWidth(), srcPixmap.getHeight(), srcPixmap.getFormat());
		pixmap.drawPixmap(srcPixmap, 0, 0);
		tilesetTexture = new Texture(pixmap);
		pixmap.dispose();

		int columns = data.getTilesetColumns(tilesetTexture.getWidth());
		int rows = data.getTilesetRows(tilesetTexture.getHeight());
		int tileW = data.getTileWidth();
		int tileH = data.getTileHeight();
		int margin = data.getMargin();
		int spacing = data.getSpacing();

		tileRegions = new TextureRegion[Math.max(0, columns * rows)];
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < columns; col++) {
				int x = margin + col * (tileW + spacing);
				int y = margin + row * (tileH + spacing);
				tileRegions[row * columns + col] = new TextureRegion(tilesetTexture, x, y, tileW, tileH);
			}
		}
	}

	public void invalidateRegions() {
		regionsDirty = true;
	}

	public void invalidatePhysics() {
		physicsDirty = true;
	}

	public boolean isPhysicsDirty() {
		return physicsDirty;
	}

	public void rebuildIfDirty(PhysicsWorld physicsWorld, Sprite sprite) {
		if (!physicsDirty || physicsWorld == null || sprite == null) {
			return;
		}
		TilemapPhysicsBuilder.destroy(physicsWorld, body);
		body = null;
		Look look = sprite.look;
		if (look != null) {
			body = TilemapPhysicsBuilder.build(physicsWorld, look.getX(), look.getY(), data, sprite);
		}
		physicsDirty = false;
	}

	private void disposeTexture() {
		if (tilesetTexture != null) {
			tilesetTexture.dispose();
			tilesetTexture = null;
		}
		tileRegions = null;
	}

	public void dispose(PhysicsWorld physicsWorld) {
		if (physicsWorld != null) {
			TilemapPhysicsBuilder.destroy(physicsWorld, body);
		}
		body = null;
		disposeTexture();
	}
}
