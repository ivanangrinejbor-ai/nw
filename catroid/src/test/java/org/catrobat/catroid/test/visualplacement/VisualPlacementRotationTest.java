package org.catrobat.catroid.test.visualplacement;

import org.catrobat.catroid.content.bricks.ShowTextBrick;
import org.catrobat.catroid.visualplacement.VisualPlacementActivity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VisualPlacementRotationTest {

	@Test
	public void visualPlacementConvertsScreenRotationToCatroidDirection() {
		// 0 screen rotation (upright image) -> 90 Catroid direction (standard upright)
		assertEquals(90f,
				VisualPlacementActivity.getSpriteDirectionFromVisualPlacement(0f), 0.001f);
		// +90 screen rotation (clockwise 90) -> 180 Catroid direction (down)
		assertEquals(180f,
				VisualPlacementActivity.getSpriteDirectionFromVisualPlacement(90f), 0.001f);
		// -90 screen rotation (counter-clockwise 90) -> 0 Catroid direction (up)
		assertEquals(0f,
				VisualPlacementActivity.getSpriteDirectionFromVisualPlacement(-90f), 0.001f);
		// 180 screen rotation -> -90 Catroid direction (left)
		assertEquals(-90f,
				VisualPlacementActivity.getSpriteDirectionFromVisualPlacement(180f), 0.001f);
		// -180 screen rotation -> -90 Catroid direction (left)
		assertEquals(-90f,
				VisualPlacementActivity.getSpriteDirectionFromVisualPlacement(-180f), 0.001f);
	}

	@Test
	public void textPlacementDoesNotInsertSpriteLevelHelpers() {
		assertFalse(new ShowTextBrick().shouldInsertHelperBricks());
	}
}
