package org.catrobat.catroid.fast2d.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import org.catrobat.catroid.fast2d.components.MovementComponent;
import org.catrobat.catroid.fast2d.components.TransformComponent;

public class Fast2DMovementSystem extends IteratingSystem {

    protected ComponentMapper<TransformComponent> mTransform;
    protected ComponentMapper<MovementComponent> mMovement;

    public Fast2DMovementSystem() {
        super(Aspect.all(TransformComponent.class, MovementComponent.class));
    }

    @Override
    protected void process(int entityId) {
        TransformComponent t = mTransform.get(entityId);
        MovementComponent m = mMovement.get(entityId);

        float delta = world.getDelta();

        m.velocityY += m.gravityY * delta;
        t.x += m.velocityX * delta;
        t.y += m.velocityY * delta;
        t.rotation += m.angularVelocity * delta;
    }
}