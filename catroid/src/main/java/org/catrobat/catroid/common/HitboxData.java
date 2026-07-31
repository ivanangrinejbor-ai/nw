package org.catrobat.catroid.common;

import java.io.Serializable;

public class HitboxData implements Serializable {
    private static final long serialVersionUID = 1L;

    public float x;
    public float y;
    public float width;
    public float height;
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
