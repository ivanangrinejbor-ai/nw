package org.catrobat.catroid.common;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

public final class LookColorProfile {
	private final float red;
	private final float green;
	private final float blue;
	private final float luminance;

	private LookColorProfile(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		luminance = Math.max(0.001f, red * 0.299f + green * 0.587f + blue * 0.114f);
	}

	public static LookColorProfile fromPixmap(Pixmap pixmap) {
		Color color = new Color();
		float red = 0f;
		float green = 0f;
		float blue = 0f;
		float weight = 0f;
		for (int y = 0; y < pixmap.getHeight(); y += 2) {
			for (int x = 0; x < pixmap.getWidth(); x += 2) {
				Color.argb8888ToColor(color, pixmap.getPixel(x, y));
				float pixelWeight = color.a;
				red += color.r * pixelWeight;
				green += color.g * pixelWeight;
				blue += color.b * pixelWeight;
				weight += pixelWeight;
			}
		}
		if (weight == 0f) {
			return new LookColorProfile(1f, 1f, 1f);
		}
		return new LookColorProfile(red / weight, green / weight, blue / weight);
	}

	public Pixmap colorize(Pixmap source) {
		Pixmap result = new Pixmap(source.getWidth(), source.getHeight(), source.getFormat());
		Color color = new Color();
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				Color.argb8888ToColor(color, source.getPixel(x, y));
				float sourceLuminance = color.r * 0.299f + color.g * 0.587f + color.b * 0.114f;
				float factor = sourceLuminance / luminance;
				color.r = Math.min(1f, red * factor);
				color.g = Math.min(1f, green * factor);
				color.b = Math.min(1f, blue * factor);
				result.drawPixel(x, y, Color.argb8888(color));
			}
		}
		return result;
	}
}
