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
package org.catrobat.catroid.physics.shapebuilder;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

import org.catrobat.catroid.physics.PhysicsWorldConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PhysicsShapeBuilderStrategyFastHull implements PhysicsShapeBuilderStrategy {

	private static final int MINIMUM_PIXEL_ALPHA_VALUE = 1;
	private static final float COLLINEAR_EPSILON = 1e-6f;

	@Override
	public Shape[] build(Pixmap pixmap, float scale) {
		if (pixmap == null) {
			return null;
		}

		int width = pixmap.getWidth();
		int height = pixmap.getHeight();

		List<Vector2> boundaryPoints = collectBoundaryPoints(pixmap, width, height);

		if (boundaryPoints.isEmpty()) {
			return null;
		}

		List<Vector2> hull = computeMonotoneChainConvexHull(boundaryPoints);

		if (hull.size() < 3) {
			hull = createMinimalTriangle(hull, width, height);
		}

		return divideShape(hull.toArray(new Vector2[hull.size()]), width, height);
	}

	private static List<Vector2> collectBoundaryPoints(Pixmap pixmap, int width, int height) {
		List<Vector2> points = new ArrayList<>();
		Pixmap.Format format = pixmap.getFormat();
		boolean hasAlpha = (format == Pixmap.Format.RGBA8888 || format == Pixmap.Format.RGBA4444);

		for (int y = 0; y < height; y++) {
			int firstX = -1;
			int lastX = -1;
			for (int x = 0; x < width; x++) {
				if (isOpaque(pixmap, x, y, format, hasAlpha)) {
					if (firstX == -1) {
						firstX = x;
					}
					lastX = x;
				}
			}
			if (firstX != -1) {
				points.add(new Vector2(firstX, y));
				if (lastX > firstX) {
					points.add(new Vector2(lastX, y));
				}
			}
		}
		return points;
	}

	private static boolean isOpaque(Pixmap pixmap, int x, int y, Pixmap.Format format, boolean hasAlpha) {
		if (!hasAlpha) {
			return true;
		}
		int pixel = pixmap.getPixel(x, y);
		int alpha;
		if (format == Pixmap.Format.RGBA8888) {
			alpha = (pixel >>> 24) & 0xFF;
		} else {
			alpha = ((pixel >>> 12) & 0x0F) * 17;
		}
		return alpha >= MINIMUM_PIXEL_ALPHA_VALUE;
	}

	private static List<Vector2> computeMonotoneChainConvexHull(List<Vector2> points) {
		if (points.size() < 2) {
			return new ArrayList<>(points);
		}

		Collections.sort(points, (a, b) -> {
			int cmp = Float.compare(a.x, b.x);
			if (cmp != 0) {
				return cmp;
			}
			return Float.compare(a.y, b.y);
		});

		List<Vector2> unique = new ArrayList<>(points.size());
		for (Vector2 p : points) {
			if (unique.isEmpty() || !unique.get(unique.size() - 1).equals(p)) {
				unique.add(p);
			}
		}

		if (unique.size() < 3) {
			return unique;
		}

		List<Vector2> hull = new ArrayList<>();

		for (Vector2 p : unique) {
			while (hull.size() >= 2) {
				Vector2 a = hull.get(hull.size() - 2);
				Vector2 b = hull.get(hull.size() - 1);
				float cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
				if (cross > COLLINEAR_EPSILON) {
					break;
				}
				hull.remove(hull.size() - 1);
			}
			hull.add(p);
		}

		// Build upper hull
		int lowerSize = hull.size();
		for (int i = unique.size() - 2; i >= 0; i--) {
			Vector2 p = unique.get(i);
			while (hull.size() > lowerSize) {
				Vector2 a = hull.get(hull.size() - 2);
				Vector2 b = hull.get(hull.size() - 1);
				float cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
				if (cross > COLLINEAR_EPSILON) {
					break;
				}
				hull.remove(hull.size() - 1);
			}
			hull.add(p);
		}

		// Remove the last point (duplicate of the first point, closing the chain)
		if (hull.size() > 1) {
			hull.remove(hull.size() - 1);
		}

		return hull;
	}

	/**
	 * Creates a minimal valid triangle (1x1 pixel box centered at the available point(s)).
	 * Called when the convex hull has fewer than 3 unique vertices.
	 */
	private static List<Vector2> createMinimalTriangle(List<Vector2> hullPoints, int width, int height) {
		float cx;
		float cy;

		if (hullPoints.isEmpty()) {
			// No opaque pixels at all — center of pixmap
			cx = width / 2.0f;
			cy = height / 2.0f;
		} else if (hullPoints.size() == 1) {
			// Single opaque pixel
			Vector2 p = hullPoints.get(0);
			cx = p.x;
			cy = p.y;
		} else {
			// Two or more collinear points — use their average
			float sumX = 0;
			float sumY = 0;
			for (Vector2 p : hullPoints) {
				sumX += p.x;
				sumY += p.y;
			}
			cx = sumX / hullPoints.size();
			cy = sumY / hullPoints.size();
		}

		List<Vector2> triangle = new ArrayList<>(3);
		triangle.add(new Vector2(cx - 0.5f, cy - 0.5f));
		triangle.add(new Vector2(cx + 0.5f, cy - 0.5f));
		triangle.add(new Vector2(cx, cy + 0.5f));
		return triangle;
	}

	/**
	 * Converts convex hull vertices from pixel coordinates to Box2D coordinates
	 * and splits the hull into sub-polygons of at most 8 vertices
	 * (Box2D PolygonShape vertex limit).
	 */
	private Shape[] divideShape(Vector2[] convexPoints, int width, int height) {
		// Convert pixel coordinates to Box2D coordinates (center-origin, Y-up)
		for (int i = 0; i < convexPoints.length; i++) {
			Vector2 point = convexPoints[i];
			float x = point.x - width / 2.0f;
			float y = height / 2.0f - point.y;
			convexPoints[i] = PhysicsWorldConverter.convertCatroidToBox2dVector(new Vector2(x, y));
		}

		if (convexPoints.length < 3) {
			// Should not happen (caller ensures 3+ hull points), but safeguard
			return null;
		}

		if (convexPoints.length < 9) {
			PolygonShape polygon = new PolygonShape();
			polygon.set(convexPoints);
			return new Shape[] {polygon};
		}

		List<Shape> shapes = new ArrayList<>(convexPoints.length / 6 + 1);
		List<Vector2> pointsPerShape = new ArrayList<>(8);

		Vector2 rome = convexPoints[0];
		int index = 1;
		while (index < convexPoints.length - 1) {
			int k = index + 7;

			int remainingPointsCount = convexPoints.length - index;
			if (remainingPointsCount > 7 && remainingPointsCount < 9) {
				k -= 3;
			}

			pointsPerShape.add(rome);
			for (; index < k && index < convexPoints.length; index++) {
				pointsPerShape.add(convexPoints[index]);
			}

			PolygonShape polygon = new PolygonShape();
			polygon.set(pointsPerShape.toArray(new Vector2[pointsPerShape.size()]));
			shapes.add(polygon);

			pointsPerShape.clear();
			index--;
		}

		return shapes.toArray(new Shape[shapes.size()]);
	}
}
