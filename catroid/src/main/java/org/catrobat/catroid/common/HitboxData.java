package org.catrobat.catroid.common;

import java.io.Serializable;

/**
 * A single rectangular hitbox attached to a LookData.
 * Coordinates are relative to the look image center (0,0 = center of image).
 * X/Y define the center of the hitbox rectangle.
 */
public class HitboxData implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Center X relative to image center (pixels in image space) */
    public float x;
    /** Center Y relative to image center (pixels in image space) */
    public float y;
    /** Width of the hitbox rectangle (pixels) */
    public float width;
    /** Height of the hitbox rectangle (pixels) */
    public float height;
    /** Rotation in degrees (0 = axis-aligned, positive = clockwise) */
    public float rotation;

    public HitboxData() {
    }

    public HitboxData(float x, float y, float width, float height, float rotation) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    public HitboxData copy() {
        return new HitboxData(x, y, width, height, rotation);
    }

    @Override
    public String toString() {
        return "Hitbox[x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + ", rot=" + rotation + "]";
    }
}
