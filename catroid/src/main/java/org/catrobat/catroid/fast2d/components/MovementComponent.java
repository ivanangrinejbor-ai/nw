package org.catrobat.catroid.fast2d.components;

import com.artemis.PooledComponent;

public class MovementComponent extends PooledComponent {
    public float velocityX = 0f, velocityY = 0f;
    public float angularVelocity = 0f;
    public float gravityY = 0f;

    @Override
    protected void reset() {
        velocityX = 0f; velocityY = 0f;
        angularVelocity = 0f; gravityY = 0f;
    }
}