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

package org.catrobat.catroid.test.common;

import com.badlogic.gdx.math.Polygon;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

import org.catrobat.catroid.common.HitboxData;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.sensing.CollisionInformation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class HitboxModeTest {

	private static final float DELTA = 0.001f;
	private static final float IMG_W = 100f;
	private static final float IMG_H = 80f;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	// --- LookData model ---------------------------------------------------

	@Test
	public void testDefaultHitboxModeIsPhysics() {
		LookData lookData = new LookData();
		assertEquals(LookData.HITBOX_MODE_PHYSICS, lookData.getHitboxMode());
		assertEquals(0, lookData.getHitboxMode());
		assertTrue(!lookData.isFullHitboxMode());
	}

	@Test
	public void testSetFullHitboxMode() {
		LookData lookData = new LookData();
		lookData.setHitboxMode(LookData.HITBOX_MODE_FULL);
		assertEquals(LookData.HITBOX_MODE_FULL, lookData.getHitboxMode());
		assertTrue(lookData.isFullHitboxMode());
	}

	@Test
	public void testCloneCarriesHitboxMode() throws Exception {
		File file = temporaryFolder.newFile("look.png");
		LookData lookData = new LookData("full", file);
		lookData.setHitboxMode(LookData.HITBOX_MODE_FULL);
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(0f, 0f, 10f, 10f, 0f));
		lookData.setHitboxes(boxes);

		LookData copy = lookData.clone();

		assertEquals(LookData.HITBOX_MODE_FULL, copy.getHitboxMode());
		assertTrue(copy.isFullHitboxMode());
		assertEquals(1, copy.getHitboxes().size());
	}

	// --- XStream round-trip / backward compatibility ----------------------

	@Test
	public void testHitboxModeSurvivesXStreamRoundTrip() {
		LookData lookData = new LookData("full");
		lookData.setHitboxMode(LookData.HITBOX_MODE_FULL);

		XStream xstream = newXStream();
		String xml = xstream.toXML(lookData);
		LookData restored = (LookData) xstream.fromXML(xml);

		assertEquals(LookData.HITBOX_MODE_FULL, restored.getHitboxMode());
	}

	@Test
	public void testMissingHitboxModeDefaultsToPhysics() {
		String xml = "<org.catrobat.catroid.common.LookData>"
				+ "<name>legacy</name>"
				+ "<valid>true</valid>"
				+ "</org.catrobat.catroid.common.LookData>";

		XStream xstream = newXStream();
		LookData restored = (LookData) xstream.fromXML(xml);

		assertEquals(LookData.HITBOX_MODE_PHYSICS, restored.getHitboxMode());
		assertEquals(0, restored.getHitboxMode());
	}

	private XStream newXStream() {
		XStream xstream = new XStream();
		xstream.addPermission(AnyTypePermission.ANY);
		xstream.ignoreUnknownElements();
		return xstream;
	}

	// --- buildPolygonsFromHitboxes geometry --------------------------------

	@Test
	public void testFullImageBoxCoversWholeImage() {
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(0f, 0f, IMG_W, IMG_H, 0f));

		Polygon[] polygons = CollisionInformation.buildPolygonsFromHitboxes(boxes, IMG_W, IMG_H);

		assertEquals(1, polygons.length);
		// A full-image, unrotated box maps to the image rectangle [0..w] x [0..h].
		float[] expected = {0f, IMG_H, IMG_W, IMG_H, IMG_W, 0f, 0f, 0f};
		assertArrayEquals(expected, polygons[0].getVertices(), DELTA);
	}

	@Test
	public void testCenteredBoxKeepsCenterAndSize() {
		float w = IMG_W / 2f;
		float h = IMG_H / 2f;
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(0f, 0f, w, h, 0f));

		Polygon[] polygons = CollisionInformation.buildPolygonsFromHitboxes(boxes, IMG_W, IMG_H);

		assertEquals(1, polygons.length);
		float[] v = polygons[0].getVertices();
		float minX = min(v, 0);
		float maxX = max(v, 0);
		float minY = min(v, 1);
		float maxY = max(v, 1);
		assertEquals(w, maxX - minX, DELTA);
		assertEquals(h, maxY - minY, DELTA);
		assertEquals(IMG_W / 2f, (minX + maxX) / 2f, DELTA);
		assertEquals(IMG_H / 2f, (minY + maxY) / 2f, DELTA);
	}

	@Test
	public void testRotation90SwapsWidthAndHeight() {
		float w = 40f;
		float h = 20f;
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(0f, 0f, w, h, 90f));

		Polygon[] polygons = CollisionInformation.buildPolygonsFromHitboxes(boxes, IMG_W, IMG_H);

		assertEquals(1, polygons.length);
		float[] v = polygons[0].getVertices();
		// After a 90 degree rotation the bounding extents are swapped.
		assertEquals(h, max(v, 0) - min(v, 0), DELTA);
		assertEquals(w, max(v, 1) - min(v, 1), DELTA);
		assertEquals(IMG_W / 2f, (min(v, 0) + max(v, 0)) / 2f, DELTA);
		assertEquals(IMG_H / 2f, (min(v, 1) + max(v, 1)) / 2f, DELTA);
	}

	@Test
	public void testNullListReturnsNull() {
		assertNull(CollisionInformation.buildPolygonsFromHitboxes(null, IMG_W, IMG_H));
	}

	@Test
	public void testEmptyListReturnsNull() {
		assertNull(CollisionInformation.buildPolygonsFromHitboxes(
				Collections.<HitboxData>emptyList(), IMG_W, IMG_H));
	}

	@Test
	public void testZeroSizeBoxesAreSkipped() {
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(0f, 0f, 0f, 10f, 0f));
		boxes.add(new HitboxData(0f, 0f, 10f, 0f, 0f));

		assertNull(CollisionInformation.buildPolygonsFromHitboxes(boxes, IMG_W, IMG_H));
	}

	@Test
	public void testMultipleBoxesProduceMultiplePolygons() {
		List<HitboxData> boxes = new ArrayList<>();
		boxes.add(new HitboxData(-20f, 0f, 10f, 10f, 0f));
		boxes.add(new HitboxData(20f, 0f, 10f, 10f, 0f));

		Polygon[] polygons = CollisionInformation.buildPolygonsFromHitboxes(boxes, IMG_W, IMG_H);

		assertEquals(2, polygons.length);
	}

	private static float min(float[] vertices, int offset) {
		float result = Float.MAX_VALUE;
		for (int i = offset; i < vertices.length; i += 2) {
			result = Math.min(result, vertices[i]);
		}
		return result;
	}

	private static float max(float[] vertices, int offset) {
		float result = -Float.MAX_VALUE;
		for (int i = offset; i < vertices.length; i += 2) {
			result = Math.max(result, vertices[i]);
		}
		return result;
	}
}
