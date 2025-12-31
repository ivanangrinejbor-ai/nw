package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.Color;

public class FogComponent implements Component {

    public enum FogType {
        NONE,
        LINEAR,
        EXPONENTIAL,
        EXPONENTIAL_SQUARED
    }

    public boolean isEnabled = true;
    public FogType type = FogType.EXPONENTIAL_SQUARED;
    public Color color = new Color(0.5f, 0.6f, 0.7f, 1.0f);

    public float density = 0.02f;

    public float startDistance = 20f;
    public float endDistance = 100f;

    public float heightFalloff = 0.1f;

    public FogComponent() {}
}