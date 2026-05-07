package org.catrobat.catroid.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class EditorCameraController implements GestureListener {

    public Camera camera;
    public boolean enabled = true;
    public boolean isPcMode = false;

    public float rotateSpeed = 0.2f;

    public float baseMoveSpeed = 5f;
    public float maxMoveSpeed = 100f;
    public float acceleration = 30f;
    private float currentMoveSpeed = baseMoveSpeed;
    public boolean isAccelerating = false;

    public final Vector3 velocity = new Vector3();
    private final Vector3 tmp = new Vector3();

    public EditorCameraController(Camera camera) {
        this.camera = camera;
    }

    public void update(float delta) {
        if (!enabled) return;

        if (isPcMode) {
            handlePcInputs();
        }

        if (isAccelerating) {
            currentMoveSpeed = Math.min(currentMoveSpeed + acceleration * delta, maxMoveSpeed);
        } else {
            currentMoveSpeed = Math.max(currentMoveSpeed - acceleration * delta * 2, baseMoveSpeed);
        }

        if (velocity.isZero() && isPcMode) {
            camera.update();
            return;
        }

        float finalSpeed = currentMoveSpeed * delta;

        if (velocity.z != 0) {
            tmp.set(camera.direction).nor().scl(finalSpeed * velocity.z);
            camera.position.add(tmp);
        }
        if (velocity.x != 0) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(finalSpeed * velocity.x);
            camera.position.add(tmp);
        }
        if (velocity.y != 0) {
            tmp.set(camera.up).nor().scl(finalSpeed * velocity.y);
            camera.position.add(tmp);
        }
        camera.update();
    }

    private void handlePcInputs() {
        velocity.setZero();
        isAccelerating = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        if (Gdx.input.isKeyPressed(Input.Keys.W)) velocity.z = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) velocity.z = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x = -1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.E)) velocity.y = 1;
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) velocity.y = -1;

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {

            float deltaX = -Gdx.input.getDeltaX() * rotateSpeed;
            float deltaY = -Gdx.input.getDeltaY() * rotateSpeed;

            if (deltaX != 0) {
                camera.rotate(Vector3.Y, deltaX);
            }
            if (deltaY != 0) {
                tmp.set(camera.direction).crs(camera.up).nor();
                camera.rotate(tmp, deltaY);
            }
        } else {
            Gdx.input.getDeltaX();
            Gdx.input.getDeltaY();
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        if (!enabled) return false;

        if (isPcMode) return false;

        camera.rotate(Vector3.Y, -deltaX * rotateSpeed);
        tmp.set(camera.direction).crs(camera.up).nor();
        camera.rotate(tmp, -deltaY * rotateSpeed);
        camera.update();
        return true;
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return enabled;
    }

    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean pinch(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4) { return false; }
    @Override public void pinchStop() {}
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float vX, float vY, int b) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
}