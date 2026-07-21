package org.catrobat.catroid.stage;

import java.util.Random;

/**
 * Drives a decaying 2D screen-shake effect.
 *
 * The shake produces a random per-frame offset (in world/camera units) whose
 * magnitude decays linearly from {@code intensity} to 0 over {@code duration}
 * seconds. The owning {@link StageListener} applies the offset to the main
 * camera around the 2D draw pass and restores it afterwards, so the shake is
 * purely visual and never accumulates into the camera's real position.
 */
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

	/**
	 * Start (or restart) a shake. A weaker shake does not interrupt a stronger
	 * one that is still running.
	 */
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

	/**
	 * Advance the shake by {@code delta} seconds and recompute the offset.
	 *
	 * @return true if a non-zero shake offset is active this frame.
	 */
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
