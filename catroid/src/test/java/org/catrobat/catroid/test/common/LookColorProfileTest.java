package org.catrobat.catroid.test.common;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import org.catrobat.catroid.common.LookColorProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LookColorProfileTest {

	@Test
	public void colorizePreservesLuminanceOrderingAndAlpha() {
		Pixmap profileSource = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
		Pixmap target = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
		try {
			profileSource.setColor(Color.RED);
			profileSource.fill();
			setPixel(target, 0, 0, new Color(0.2f, 0.2f, 0.2f, 0.25f));
			setPixel(target, 1, 0, new Color(0.8f, 0.8f, 0.8f, 0.75f));

			LookColorProfile profile = LookColorProfile.fromPixmap(profileSource);
			Pixmap result = profile.colorize(target);

			Color dark = readPixel(result, 0, 0);
			Color bright = readPixel(result, 1, 0);
			assertTrue(bright.r > dark.r);
			assertTrue(dark.r > dark.g);
			assertTrue(dark.r > dark.b);
			assertEquals(0.25f, dark.a, 0.01f);
			assertEquals(0.75f, bright.a, 0.01f);
			result.dispose();
		} finally {
			profileSource.dispose();
			target.dispose();
		}
	}

	@Test
	public void transparentProfileFallsBackWithoutChangingTargetAlpha() {
		Pixmap profileSource = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
		Pixmap target = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		try {
			profileSource.setColor(new Color(1f, 1f, 1f, 0f));
			profileSource.fill();
			setPixel(target, 0, 0, new Color(0.5f, 0.5f, 0.5f, 0.4f));

			Pixmap result = LookColorProfile.fromPixmap(profileSource).colorize(target);
			Color pixel = readPixel(result, 0, 0);
			assertEquals(0.4f, pixel.a, 0.01f);
			assertTrue(pixel.r > 0f);
			assertEquals(pixel.r, pixel.g, 0.01f);
			assertEquals(pixel.g, pixel.b, 0.01f);
			result.dispose();
		} finally {
			profileSource.dispose();
			target.dispose();
		}
	}

	private static void setPixel(Pixmap pixmap, int x, int y, Color color) {
		pixmap.drawPixel(x, y, Color.argb8888(color));
	}

	private static Color readPixel(Pixmap pixmap, int x, int y) {
		Color color = new Color();
		Color.argb8888ToColor(color, pixmap.getPixel(x, y));
		return color;
	}
}
