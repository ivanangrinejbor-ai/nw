package org.catrobat.catroid.raptor;

import com.badlogic.gdx.math.Vector3;
import java.io.Serializable;

public class AIComponent implements Component {
    public enum Mode { OFF, FOLLOW, MOVE_TO, FLEE }

    public Mode mode = Mode.OFF;
    public String targetId = "";
    public Vector3 targetPos = new Vector3();
    public float speed = 5f;
    public float turnSpeed = 5f;
    public float stopDistance = 1.5f;
    public boolean avoidObstacles = true;
    public float avoidanceRange = 3.0f;
    public boolean groundOnly = true;
    public float detectionRange = 4.0f;
    public float stepHeight = 0.7f;
    public float climbSpeed = 4.0f;

    public final Vector3 currentVelocity = new Vector3();
    public final Vector3 lastPosition = new Vector3();
    public float stuckTimer = 0;
    public float randomWanderTimer = 0;

    public AIComponent() {}
}