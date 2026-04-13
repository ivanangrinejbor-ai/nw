package org.catrobat.catroid.fast2d.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.physics.box2d.Body;

public class PhysicsComponent extends PooledComponent {
    public Body body = null;

    public boolean isDynamic = true;
    public String shapeType = "BOX";
    public float density = 1f;
    public float friction = 0.5f;
    public float bounce = 0f;

    @Override
    protected void reset() {
        body = null;
        isDynamic = true;
        shapeType = "BOX";
        density = 1f;
        friction = 0.5f;
        bounce = 0f;
    }
}