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

		// Step 1: Collect boundary pixels (first and last non-transparent per row)
		List<Vector2> boundaryPoints = collectBoundaryPoints(pixmap, width, height);

		if (boundaryPoints.isEmpty()) {
			return null;
		}

		// Step 2: Compute the convex hull using Andrew's monotone chain
		List<Vector2> hull = computeMonotoneChainConvexHull(boundaryPoints);

		// Step 3: Handle degenerate hull (0, 1, or 2 vertices — cannot form a valid polygon)
		if (hull.size() < 3) {
			hull = createMinimalTriangle(hull, width, height);
		}

		// Step 4 (already handled inside monotone chain): collinear points removed via epsilon

		// Step 5: Convert to Box2D coordinates and divide into sub-polygons (max 8 vertices each)
		return divideShape(hull.toArray(new Vector2[hull.size()]), width, height);
	}

	/**
	 * Collects boundary-representative points by scanning each row for
	 * the first and last non-transparent pixel. This set is sufficient
	 * to compute the exact convex hull of the full pixmap.
	 */
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

	/**
	 * Returns true if the pixel at (x, y) has enough alpha to be considered opaque.
	 * For formats without an alpha channel (RGB888, RGB565) all pixels are opaque.
	 * For RGBA8888 the alpha byte is in bits 24-31 of the pixel int.
	 * For RGBA4444 the alpha is in bits 12-15 (4-bit), scaled up for comparison.
	 */
	private static boolean isOpaque(Pixmap pixmap, int x, int y, Pixmap.Format format, boolean hasAlpha) {
		if (!hasAlpha) {
			return true; // No alpha channel — every pixel is fully opaque
		}
		int pixel = pixmap.getPixel(x, y);
		int alpha;
		if (format == Pixmap.Format.RGBA8888) {
			alpha = (pixel >>> 24) & 0xFF;
		} else { // RGBA4444
			alpha = ((pixel >>> 12) & 0x0F) * 17; // Scale 4-bit (0-15) to 8-bit (0-255)
		}
		return alpha >= MINIMUM_PIXEL_ALPHA_VALUE;
	}

	/**
	 * Andrew's monotone chain convex hull algorithm (O(n log n)).
	 * Removes collinear points using a small epsilon.
	 */
	private static List<Vector2> computeMonotoneChainConvexHull(List<Vector2> points) {
		if (points.size() < 2) {
			return new ArrayList<>(points);
		}

		// Sort by x, then by y
		Collections.sort(points, (a, b) -> {
			int cmp = Float.compare(a.x, b.x);
			if (cmp != 0) {
				return cmp;
			}
			return Float.compare(a.y, b.y);
		});

		// Remove duplicate points (consecutive after sorting)
		List<Vector2> unique = new ArrayList<>(points.size());
		for (Vector2 p : points) {
			if (unique.isEmpty() || !unique.get(unique.size() - 1).equals(p)) {
				unique.add(p);
			}
		}

		if (unique.size() < 3) {
			return unique; // Degenerate hull — caller will create a minimal triangle
		}

		List<Vector2> hull = new ArrayList<>();

		// Build lower hull
		for (Vector2 p : unique) {
			while (hull.size() >= 2) {
				Vector2 a = hull.get(hull.size() - 2);
				Vector2 b = hull.get(hull.size() - 1);
				float cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
				if (cross > COLLINEAR_EPSILON) {
					break; // Strictly counter-clockwise — keep b
				}
				hull.remove(hull.size() - 1); // Clockwise or collinear — pop b
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
