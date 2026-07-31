package org.catrobat.catroid.stage;

import java.util.Random;

public class ScreenShakeController {
	private final Random random = new Random();

	private float intensity = 0f;
	private float duration = 0f;
	private float elapsed = 0f;
	private float offsetX = 0f;
	private float offsetY = 0f;

	public boolean isActive() {
		return elapsed < duration && intensity > 0f;
	}

	public void start(float intensity, float duration) {
		if (intensity <= 0f || duration <= 0f) {
			return;
		}
		if (isActive() && this.intensity > intensity) {
			return;
		}
		this.intensity = intensity;
		this.duration = duration;
		this.elapsed = 0f;
	}

	public void stop() {
		intensity = 0f;
		duration = 0f;
		elapsed = 0f;
		offsetX = 0f;
		offsetY = 0f;
	}

	public boolean update(float delta) {
		if (!isActive()) {
			if (offsetX != 0f || offsetY != 0f) {
				offsetX = 0f;
				offsetY = 0f;
			}
			return false;
		}
		elapsed += delta;
		if (elapsed >= duration) {
			stop();
			return false;
		}
		float remaining = 1f - (elapsed / duration);
		float magnitude = intensity * remaining;
		offsetX = (random.nextFloat() * 2f - 1f) * magnitude;
		offsetY = (random.nextFloat() * 2f - 1f) * magnitude;
		return true;
	}

	public float getOffsetX() {
		return offsetX;
	}

	public float getOffsetY() {
		return offsetY;
	}
}
