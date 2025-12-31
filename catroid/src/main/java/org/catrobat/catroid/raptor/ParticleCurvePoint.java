package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.Color;

public class ParticleCurvePoint<T> {
    public float time;
    public T value;

    public ParticleCurvePoint() {}

    public ParticleCurvePoint(float time, T value) {
        this.time = time;
        this.value = value;
    }
}