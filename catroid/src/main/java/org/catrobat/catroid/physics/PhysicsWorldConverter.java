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
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.danvexteam.lunoscript_annotations.LunoClass;

@LunoClass
public final class PhysicsWorldConverter {

	private PhysicsWorldConverter() {
	}

	public static float convertBox2dToNormalAngle(float box2dAngle) {
		return (float) Math.toDegrees(box2dAngle);
	}

	public static float convertNormalToBox2dAngle(float catroidAngle) {
		return (float) Math.toRadians(catroidAngle);
	}

	public static float convertBox2dToNormalCoordinate(float box2dCoordinate) {
		return box2dCoordinate * PhysicsWorld.RATIO;
	}

	public static float convertNormalToBox2dCoordinate(float catroidCoordinate) {
		return catroidCoordinate / PhysicsWorld.RATIO;
	}

	public static Vector2 convertBox2dToNormalVector(Vector2 box2DVector) {
		return new Vector2(convertBox2dToNormalCoordinate(box2DVector.x), convertBox2dToNormalCoordinate(box2DVector.y));
	}

	public static Vector2 convertCatroidToBox2dVector(Vector2 catroidVector) {
		return new Vector2(convertNormalToBox2dCoordinate(catroidVector.x),
				convertNormalToBox2dCoordinate(catroidVector.y));
	}

	public static Vector2 convertBox2dToNormal(Vector2 box2DVector) {
		return new Vector2(
				convertBox2dToNormalCoordinate(box2DVector.x),
				convertBox2dToNormalCoordinate(box2DVector.y));
	}

	public static Vector2 convertCatroidToBox2d(Vector2 catroidVector) {
		return new Vector2(
				convertNormalToBox2dCoordinate(catroidVector.x),
				convertNormalToBox2dCoordinate(catroidVector.y));
	}

	private static final ThreadLocal<Vector2> tmpVecLocal = ThreadLocal.withInitial(Vector2::new);

	public static float computeShapeArea(Shape shape) {
		switch (shape.getType()) {
			case Circle: {
				CircleShape circle = (CircleShape) shape;
				float r = circle.getRadius();
				return (float) (Math.PI * r * r);
			}
			case Polygon: {
				PolygonShape poly = (PolygonShape) shape;
				int count = poly.getVertexCount();
				if (count < 3) return 0f;
				Vector2 tmp = tmpVecLocal.get();
				Vector2 first = new Vector2();
				Vector2 prev = new Vector2();
				float area = 0f;
				poly.getVertex(0, first);
				prev.set(first);
				for (int i = 1; i < count; i++) {
					Vector2 curr = tmp;
					poly.getVertex(i, curr);
					area += prev.x * curr.y - curr.x * prev.y;
					prev.set(curr);
				}
				area += prev.x * first.y - first.x * prev.y;
				return Math.abs(area) / 2f;
			}
			default:
				return 0f;
		}
	}

	public static void convertBox2dToNormalVector(Vector2 box2DVector, Vector2 out) {
		out.set(convertBox2dToNormalCoordinate(box2DVector.x), convertBox2dToNormalCoordinate(box2DVector.y));
	}

	public static void convertCatroidToBox2dVector(Vector2 catroidVector, Vector2 out) {
		out.set(convertNormalToBox2dCoordinate(catroidVector.x), convertNormalToBox2dCoordinate(catroidVector.y));
	}
}
