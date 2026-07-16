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

package org.catrobat.catroid.test.visualplacement;

import org.catrobat.catroid.visualplacement.VisualPlacementActivity;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class VisualPlacementCoordinateConversionTest {

	private static final int PROJECT_WIDTH = 480;
	private static final int PROJECT_HEIGHT = 800;
	private static final float LAYOUT_WIDTH_RATIO = 0.5f;  // layout 240px <-> project 480
	private static final float LAYOUT_HEIGHT_RATIO = 0.5f; // layout 400px <-> project 800

	@Test
	public void testCenterTapGivesOrigin() {
		// Центр frameLayout в пикселях: (layoutWidth/2, layoutHeight/2)
		float centerX = (PROJECT_WIDTH * LAYOUT_WIDTH_RATIO) / 2f;  // 120
		float centerY = -(PROJECT_HEIGHT * LAYOUT_HEIGHT_RATIO) / 2f; // -200 (ось Y инвертирована)

		int stageX = VisualPlacementActivity.pixelToStageX(centerX, LAYOUT_WIDTH_RATIO, PROJECT_WIDTH);
		int stageY = VisualPlacementActivity.pixelToStageY(centerY, LAYOUT_HEIGHT_RATIO, PROJECT_HEIGHT);

		assertEquals("центр экрана должен давать X=0", 0, stageX);
		assertEquals("центр экрана должен давать Y=0", 0, stageY);
	}

	@Test
	public void testRightEdgeGivesPlusHalfWidth() {
		float rightX = PROJECT_WIDTH * LAYOUT_WIDTH_RATIO; // правый край frameLayout
		int stageX = VisualPlacementActivity.pixelToStageX(rightX, LAYOUT_WIDTH_RATIO, PROJECT_WIDTH);
		assertEquals("правый край -> +W/2", PROJECT_WIDTH / 2, stageX);
	}

	@Test
	public void testLeftEdgeGivesMinusHalfWidth() {
		float leftX = 0f; // левый край frameLayout
		int stageX = VisualPlacementActivity.pixelToStageX(leftX, LAYOUT_WIDTH_RATIO, PROJECT_WIDTH);
		assertEquals("левый край -> -W/2", -PROJECT_WIDTH / 2, stageX);
	}

	@Test
	public void testTopEdgeGivesPlusHalfHeight() {
		float topY = 0f; // верх frameLayout -> yCoord = 0
		int stageY = VisualPlacementActivity.pixelToStageY(topY, LAYOUT_HEIGHT_RATIO, PROJECT_HEIGHT);
		assertEquals("верх -> +H/2", PROJECT_HEIGHT / 2, stageY);
	}

	@Test
	public void testBottomEdgeGivesMinusHalfHeight() {
		float bottomY = -(PROJECT_HEIGHT * LAYOUT_HEIGHT_RATIO); // низ frameLayout
		int stageY = VisualPlacementActivity.pixelToStageY(bottomY, LAYOUT_HEIGHT_RATIO, PROJECT_HEIGHT);
		assertEquals("низ -> -H/2", -PROJECT_HEIGHT / 2, stageY);
	}

	@Test
	public void testRoundTripIsIdentity() {
		// То, что показываем по translateX/Y, должно возвращаться обратно.
		int translateX = 37;
		int translateY = -53;

		float sceneCenterX = (PROJECT_WIDTH * LAYOUT_WIDTH_RATIO) / 2f + translateX * LAYOUT_WIDTH_RATIO;
		float sceneCenterY = (PROJECT_HEIGHT * LAYOUT_HEIGHT_RATIO) / 2f - translateY * LAYOUT_HEIGHT_RATIO;

		// listener сохраняет центр объекта как (sceneCenterX, -sceneCenterY)
		float xCoord = sceneCenterX;
		float yCoord = -sceneCenterY;

		int backX = VisualPlacementActivity.pixelToStageX(xCoord, LAYOUT_WIDTH_RATIO, PROJECT_WIDTH);
		int backY = VisualPlacementActivity.pixelToStageY(yCoord, LAYOUT_HEIGHT_RATIO, PROJECT_HEIGHT);

		assertEquals("round-trip X должен совпадать", translateX, backX);
		assertEquals("round-trip Y должен совпадать", translateY, backY);
	}
}
