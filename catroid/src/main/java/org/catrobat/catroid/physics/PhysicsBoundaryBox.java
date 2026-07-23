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
import com.danvexteam.lunoscript_annotations.LunoClass;

@LunoClass
public class PhysicsBoundaryBox {

	public static final int FRAME_SIZE = 5;

	private final World world;
	private Body boundaryBody;
	private boolean created = false;

	public enum BoundaryBoxIdentifier {BBI_HORIZONTAL, BBI_VERTICAL}

	public PhysicsBoundaryBox(World world) {
		this.world = world;
	}

	/**
	 * Creates a single static body with four polygon fixtures (top, bottom, left, right).
	 * Each fixture carries its BoundaryBoxIdentifier in fixture user data.
	 *
	 * @param height
	 * @param width
	 */
	public void create(int width, int height) {
		destroy(); // prevent double-create body leak
		float boxWidth = PhysicsWorldConverter.convertNormalToBox2dCoordinate(width);
		float boxHeight = PhysicsWorldConverter.convertNormalToBox2dCoordinate(height);
		float boxElementSize = PhysicsWorldConverter.convertNormalToBox2dCoordinate(PhysicsBoundaryBox.FRAME_SIZE);
		float halfBoxElementSize = boxElementSize / 2.0f;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.allowSleep = false;
		boundaryBody = world.createBody(bodyDef);

		addSideFixture(new Vector2(0.0f, (boxHeight / 2.0f) + halfBoxElementSize),
				boxWidth, boxElementSize, BoundaryBoxIdentifier.BBI_HORIZONTAL);
		addSideFixture(new Vector2(0.0f, -(boxHeight / 2.0f) - halfBoxElementSize),
				boxWidth, boxElementSize, BoundaryBoxIdentifier.BBI_HORIZONTAL);
		addSideFixture(new Vector2(-(boxWidth / 2.0f) - halfBoxElementSize, 0.0f),
				boxElementSize, boxHeight, BoundaryBoxIdentifier.BBI_VERTICAL);
		addSideFixture(new Vector2((boxWidth / 2.0f) + halfBoxElementSize, 0.0f),
				boxElementSize, boxHeight, BoundaryBoxIdentifier.BBI_VERTICAL);

		created = true;
	}

	private void addSideFixture(Vector2 center, float width, float height, BoundaryBoxIdentifier identifier) {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(width / 2.0f, height / 2f, center, 0.0f);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.filter.maskBits = PhysicsWorld.MASK_BOUNDARYBOX;
		fixtureDef.filter.categoryBits = PhysicsWorld.CATEGORY_BOUNDARYBOX;

		boundaryBody.createFixture(fixtureDef).setUserData(identifier);
		shape.dispose(); // Box2D copies shape data into fixture — native peer must be freed
	}

	public void destroy() {
		if (boundaryBody != null && boundaryBody.getWorld() != null) {
			world.destroyBody(boundaryBody);
			boundaryBody = null;
		}
		created = false;
	}
}
