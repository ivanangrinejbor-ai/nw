package org.catrobat.catroid.fast2d.components;

import com.artemis.PooledComponent;

public class TransformComponent extends PooledComponent {
    public float x = 0f, y = 0f, z = 0f;
    public float scaleX = 1f, scaleY = 1f;
    public float rotation = 0f;

    @Override
    protected void reset() {
        x = 0f; y = 0f; z = 0f;
        scaleX = 1f; scaleY = 1f;
        rotation = 0f;
    }
}