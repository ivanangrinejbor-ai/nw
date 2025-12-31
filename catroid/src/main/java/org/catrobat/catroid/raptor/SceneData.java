package org.catrobat.catroid.raptor;

import com.badlogic.gdx.utils.Array;

public class SceneData {
    public Array<GameObject> gameObjects = new Array<>();

    public float skyR = 0.1f;
    public float skyG = 0.2f;
    public float skyB = 0.3f;

    public String skyboxPath = null;

    public boolean fogEnabled = false;
    public float fogDensity = 0.01f;
    public float fogR = 0.5f, fogG = 0.5f, fogB = 0.5f;

    public float ambientIntensity = 1;

    public ThreeDManager.SceneSettings renderSettings = new ThreeDManager.SceneSettings();
}