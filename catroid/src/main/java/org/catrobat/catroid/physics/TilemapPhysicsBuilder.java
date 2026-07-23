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
package org.catrobat.catroid.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import org.catrobat.catroid.common.TilemapLookData;

import org.catrobat.catroid.content.Sprite;

/**
 * Stateless helper that turns the solid tiles of a {@link TilemapLookData} into Box2D static
 * bodies. It holds NO state — the caller ({@code TilemapRuntime}) owns the returned body and is
 * responsible for destroying it. Solidity is the union across all layers.
 *
 * <p>Adjacent solid tiles are merged into larger rectangles (greedy meshing) to keep the fixture
 * count low. All fixtures are attached to a single static body at the world origin; each fixture's
 * box is offset to the tile's world position (same approach as {@link PhysicsBoundaryBox}).</p>
 */
public final class TilemapPhysicsBuilder {

	private TilemapPhysicsBuilder() {
	}

	/** Destroys a body previously returned by {@link #build}. Safe to call with {@code null}. */
	public static void destroy(PhysicsWorld physicsWorld, Body body) {
		if (body == null) {
			return;
		}
		World world = physicsWorld.getWorld();
		if (body.getWorld() != null) {
			world.destroyBody(body);
		}
	}

	public static Body build(PhysicsWorld physicsWorld, float bottomLeftX, float bottomLeftY,
			TilemapLookData data) {
		return build(physicsWorld, bottomLeftX, bottomLeftY, data, null);
	}

	/**
	 * Builds a static body for the map's solid tiles.
	 *
	 * @param physicsWorld    target world
	 * @param bottomLeftX     catroid X of the map's bottom-left corner (sprite center - mapWidth/2)
	 * @param bottomLeftY     catroid Y of the map's bottom-left corner (sprite center - mapHeight/2)
	 * @param data            the tilemap
	 * @param sprite          the owner sprite (attached to body userData for collision callbacks)
	 * @return the created static body, or {@code null} if there are no solid tiles
	 */
	public static Body build(PhysicsWorld physicsWorld, float bottomLeftX, float bottomLeftY,
			TilemapLookData data, Sprite sprite) {
		int columns = data.getMapColumns();
		int rows = data.getMapRows();
		if (columns <= 0 || rows <= 0) {
			return null;
		}

		boolean[] solid = collectSolidCells(data, columns, rows);
		boolean any = false;
		for (boolean b : solid) {
			if (b) {
				any = true;
				break;
			}
		}
		if (!any) {
			return null;
		}

		World world = physicsWorld.getWorld();
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.allowSleep = true;
		Body body = world.createBody(bodyDef);

		// Attach sprite so contact listeners don't throw NPE on body.getUserData()
		if (sprite != null) {
			body.setUserData(sprite);
		}

		float scaleX = sprite != null && sprite.look != null ? sprite.look.getScaleX() : 1f;
		float scaleY = sprite != null && sprite.look != null ? sprite.look.getScaleY() : 1f;
		float rotation = sprite != null && sprite.look != null ? sprite.look.getRotation() : 0f;

		float tileW = data.getTileWidth() * scaleX;
		float tileH = data.getTileHeight() * scaleY;
		boolean[] used = new boolean[columns * rows];

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < columns; col++) {
				int index = row * columns + col;
				if (!solid[index] || used[index]) {
					continue;
				}
				// Greedy: extend width, then height while the full width stays solid.
				int width = 1;
				while (col + width < columns && solid[index + width] && !used[index + width]) {
					width++;
				}
				int height = 1;
				boolean canGrow = true;
				while (canGrow && row + height < rows) {
					int base = (row + height) * columns + col;
					for (int w = 0; w < width; w++) {
						if (!solid[base + w] || used[base + w]) {
							canGrow = false;
							break;
						}
					}
					if (canGrow) {
						height++;
					}
				}
				for (int r = 0; r < height; r++) {
					for (int w = 0; w < width; w++) {
						used[(row + r) * columns + col + w] = true;
					}
				}
				addRectFixture(body, bottomLeftX + col * tileW, bottomLeftY + row * tileH,
						width * tileW, height * tileH, sprite, rotation);
			}
		}
		return body;
	}

	private static boolean[] collectSolidCells(TilemapLookData data, int columns, int rows) {
		boolean[] solid = new boolean[columns * rows];
		for (short[] layer : data.getLayers()) {
			if (layer == null) {
				continue;
			}
			for (int i = 0; i < layer.length && i < solid.length; i++) {
				short tile = layer[i];
				if (tile != TilemapLookData.EMPTY && data.isSolidTile(tile)) {
					solid[i] = true;
				}
			}
		}
		return solid;
	}

	private static void addRectFixture(Body body, float x, float y, float width, float height,
			Sprite sprite, float rotation) {
		float centerX = x + width / 2f;
		float centerY = y + height / 2f;

		if (sprite != null && sprite.look != null && rotation != 0f) {
			float pivotX = sprite.look.getX();
			float pivotY = sprite.look.getY();
			float rad = (float) Math.toRadians(rotation);
			float cos = (float) Math.cos(rad);
			float sin = (float) Math.sin(rad);
			float relX = centerX - pivotX;
			float relY = centerY - pivotY;
			centerX = pivotX + (relX * cos - relY * sin);
			centerY = pivotY + (relX * sin + relY * cos);
		}

		float halfWidthBox2d = PhysicsWorldConverter.convertNormalToBox2dCoordinate(width / 2f);
		float halfHeightBox2d = PhysicsWorldConverter.convertNormalToBox2dCoordinate(height / 2f);
		Vector2 centerBox2d = new Vector2(
				PhysicsWorldConverter.convertNormalToBox2dCoordinate(centerX),
				PhysicsWorldConverter.convertNormalToBox2dCoordinate(centerY));

		PolygonShape shape = new PolygonShape();
		float angleRad = (float) Math.toRadians(rotation);
		shape.setAsBox(halfWidthBox2d, halfHeightBox2d, centerBox2d, angleRad);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.filter.categoryBits = PhysicsWorld.CATEGORY_PHYSICSOBJECT;
		fixtureDef.filter.maskBits = PhysicsWorld.MASK_PHYSICSOBJECT;

		body.createFixture(fixtureDef);
		shape.dispose();
	}
}
