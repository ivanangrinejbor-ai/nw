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

import android.graphics.Point;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

import org.catrobat.catroid.physics.PhysicsWorldConverter;

public class PhysicsShapeBuilderStrategyRectangle implements PhysicsShapeBuilderStrategy {

	private static final int MINIMUM_PIXEL_ALPHA_VALUE = 1;

	@Override
	public Shape[] build(Pixmap pixmap, float scale) {
		if (pixmap == null) {
			return null;
		}

		Pixmap.Format format = pixmap.getFormat();
		boolean hasAlpha = (format == Pixmap.Format.RGBA8888 || format == Pixmap.Format.RGBA4444);

		Point start = null;
		Point end = null;

		for (int y = 0; y < pixmap.getHeight(); y++) {
			for (int x = 0; x < pixmap.getWidth(); x++) {
				if (!isOpaque(pixmap, x, y, format, hasAlpha)) {
					continue;
				}

				if (start == null) {
					start = new Point(x, y);
					end = new Point(x, y);
					continue;
				}

				if (x < start.x) {
					start.x = x;
				} else if (x > end.x) {
					end.x = x;
				}

				if (y < start.y) {
					start.y = y;
				} else if (y > end.y) {
					end.y = y;
				}
			}
		}

		if (start == null) {
			return null;
		}

		int width = end.x - start.x;
		int height = end.y - start.y;

		if (width == 0) {
			width = 1;
		}

		if (height == 0) {
			height = 1;
		}

		float box2dWidth = PhysicsWorldConverter.convertNormalToBox2dCoordinate(width) / 2.0f;
		float box2dHeight = PhysicsWorldConverter.convertNormalToBox2dCoordinate(height) / 2.0f;

		// Correct center: compute the bounding box center in pixel space,
		// offset from the pixmap origin, flip Y for Box2D, then convert units.
		float pixelCenterX = start.x + width / 2.0f;
		float pixelCenterY = start.y + height / 2.0f;
		Vector2 center = new Vector2(
				PhysicsWorldConverter.convertNormalToBox2dCoordinate(pixelCenterX - pixmap.getWidth() / 2.0f),
				PhysicsWorldConverter.convertNormalToBox2dCoordinate(pixmap.getHeight() / 2.0f - pixelCenterY));

		PolygonShape polygonShape = new PolygonShape();
		polygonShape.setAsBox(box2dWidth, box2dHeight, center, 0.0f);

		return new Shape[] {polygonShape};
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
}
