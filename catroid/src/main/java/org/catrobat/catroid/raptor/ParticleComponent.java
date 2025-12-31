package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ParticleComponent implements Component {
    public enum SpawnShape {
        POINT,
        BOX,
        SPHERE,
        CYLINDER
    }

    public boolean looping = true;
    public float duration = 5f;
    public float startLifetime = 5f;
    public int maxParticles = 100;
    public float emissionRate = 10f;
    public boolean isAdditive = false;
    public String texturePath = null;

    public float coneAngle = 45f;
    public float coneRadius = 1f;
    public List<ParticleCurvePoint<Color>> colorGraph = new ArrayList<>();

    public float baseSize = 1f;
    public List<ParticleCurvePoint<Float>> sizeGraph = new ArrayList<>();

    public List<ParticleCurvePoint<Float>> rotationGraph = new ArrayList<>();

    public List<ParticleCurvePoint<Float>> speedGraph = new ArrayList<>();

    public List<ParticleCurvePoint<Float>> gravityGraph = new ArrayList<>();

    public List<ParticleCurvePoint<Float>> vortexGraph = new ArrayList<>();
    public List<ParticleCurvePoint<Float>> turbulenceGraph = new ArrayList<>();

    public Color startColor = new Color(1,1,1,1);
    public Color endColor = new Color(1,1,1,1);
    public float startSize = 1f;
    public float endSize = 1f;
    public float startSpeed = 5f;
    public float gravityModifier = 0f;
    public float startRotation = 0f;
    public float rotationOverLifetime = 0f;

    public SpawnShape spawnShape = SpawnShape.CYLINDER;

    public Vector3 spawnSize = new Vector3(1f, 1f, 1f);

    public boolean spawnOnSurface = false;

    public void migrateOldDataIfNeeded() {
        if (colorGraph.isEmpty()) {
            colorGraph.add(new ParticleCurvePoint<>(0f, new Color(startColor)));
            colorGraph.add(new ParticleCurvePoint<>(1f, new Color(endColor)));
        }
        if (sizeGraph.isEmpty()) {
            baseSize = startSize;
            sizeGraph.add(new ParticleCurvePoint<>(0f, 1f));
            float ratio = (startSize != 0) ? endSize / startSize : 1f;
            sizeGraph.add(new ParticleCurvePoint<>(1f, ratio));
        }
        if (speedGraph.isEmpty()) {
            speedGraph.add(new ParticleCurvePoint<>(0f, startSpeed));
            speedGraph.add(new ParticleCurvePoint<>(1f, startSpeed));
        }
        if (gravityGraph.isEmpty() && gravityModifier != 0) {
            float gForce = 9.81f * gravityModifier;
            gravityGraph.add(new ParticleCurvePoint<>(0f, gForce));
            gravityGraph.add(new ParticleCurvePoint<>(1f, gForce));
        }
        if (rotationGraph.isEmpty() && (startRotation != 0 || rotationOverLifetime != 0)) {
            rotationGraph.add(new ParticleCurvePoint<>(0f, rotationOverLifetime));
            rotationGraph.add(new ParticleCurvePoint<>(1f, rotationOverLifetime));
        }
    }

    public void sortGraphs() {
        Comparator<ParticleCurvePoint<?>> comp = (o1, o2) -> Float.compare(o1.time, o2.time);
        Collections.sort(colorGraph, comp);
        Collections.sort(sizeGraph, comp);
        Collections.sort(rotationGraph, comp);
        Collections.sort(speedGraph, comp);
        Collections.sort(gravityGraph, comp);
        Collections.sort(vortexGraph, comp);
        Collections.sort(turbulenceGraph, comp);
    }
}