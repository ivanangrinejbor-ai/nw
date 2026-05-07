package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import org.catrobat.catroid.editor.EditorCameraController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ParticleSystem3DComponent implements Component {



    public static class SubEmitterEntry {
        public SubEmitterTrigger trigger = SubEmitterTrigger.DEATH;

        public String subEmitterObjectId = "";
        public float probability = 1f;

        public int emitCount = 0;
    }

    public enum SimulationSpace {
        LOCAL,
        WORLD
    }

    public enum RenderMode {
        BILLBOARD,
        STRETCHED_BILLBOARD,
        HORIZONTAL_BILLBOARD,
        VERTICAL_BILLBOARD,
        MESH
    }

    public enum ShapeType {
        SPHERE,
        HEMISPHERE,
        CONE,
        BOX,
        CIRCLE,
        EDGE,
        MESH_SHAPE
    }

    public enum EmitFrom {
        VOLUME,
        SURFACE,
        EDGE
    }

    public enum CollisionMode {
        NONE,
        WORLD_PHYSICS,
        PLANES
    }

    public enum SubEmitterTrigger {
        BIRTH,
        DEATH,
        COLLISION
    }

    public enum ScalingMode {
        HIERARCHY,
        LOCAL,
        SHAPE
    }



    public boolean isPlaying = true;
    public float duration = 5f;
    public boolean looping = true;
    public boolean prewarm = false;
    public TextureSheetModule textureSheetAnimation = new TextureSheetModule();



    public MinMaxCurve startDelay = new MinMaxCurve(0f);


    public MinMaxCurve startLifetime = new MinMaxCurve(5f);


    public MinMaxCurve startSpeed = new MinMaxCurve(5f);


    public boolean threeDStartSize = false;
    public MinMaxCurve startSize = new MinMaxCurve(1f);
    public MinMaxCurve startSizeX = new MinMaxCurve(1f);
    public MinMaxCurve startSizeY = new MinMaxCurve(1f);
    public MinMaxCurve startSizeZ = new MinMaxCurve(1f);


    public boolean threeDStartRotation = false;
    public MinMaxCurve startRotation = new MinMaxCurve(0f);
    public MinMaxCurve startRotationX = new MinMaxCurve(0f);
    public MinMaxCurve startRotationY = new MinMaxCurve(0f);
    public MinMaxCurve startRotationZ = new MinMaxCurve(0f);


    public float flipRotation = 0f;


    public MinMaxGradient startColor = new MinMaxGradient(new Color(1, 1, 1, 1));


    public MinMaxCurve gravityModifier = new MinMaxCurve(0f);

    public SimulationSpace simulationSpace = SimulationSpace.LOCAL;
    public ScalingMode scalingMode = ScalingMode.LOCAL;

    public int maxParticles = 1000;


    public long randomSeed = 0;



    public EmissionModule emission = new EmissionModule();

    public static class EmissionModule {
        public boolean enabled = true;

        public MinMaxCurve rateOverTime = new MinMaxCurve(10f);

        public MinMaxCurve rateOverDistance = new MinMaxCurve(0f);

        public List<Burst> bursts = new ArrayList<>();
    }

    public static class Burst {
        public float time = 0f;
        public MinMaxCurve count = new MinMaxCurve(30f);
        public int cycles = 1;
        public float interval = 0.01f;
        public float probability = 1f;

        public Burst() {}
        public Burst(float time, float count) {
            this.time = time;
            this.count = new MinMaxCurve(count);
        }
    }



    public ShapeModule shape = new ShapeModule();

    public static class ShapeModule {
        public boolean enabled = true;
        public ShapeType type = ShapeType.CONE;


        public float coneAngle = 25f;
        public float coneRadius = 1f;
        public float coneLength = 5f;
        public EmitFrom emitFrom = EmitFrom.VOLUME;


        public Vector3 boxSize = new Vector3(1, 1, 1);


        public float sphereRadius = 1f;


        public float circleRadius = 1f;
        public float circleArc = 360f;


        public float edgeLength = 1f;


        public boolean randomizeDirection = false;
        public float spherizeDirection = 0f;
        public float randomizePosition = 0f;


        public String meshModelPath = null;
    }



    public VelocityOverLifetimeModule velocityOverLifetime = new VelocityOverLifetimeModule();

    public static class VelocityOverLifetimeModule {
        public boolean enabled = false;
        public MinMaxCurve x = new MinMaxCurve(0f);
        public MinMaxCurve y = new MinMaxCurve(0f);
        public MinMaxCurve z = new MinMaxCurve(0f);
        public SimulationSpace space = SimulationSpace.LOCAL;


        public MinMaxCurve orbitalX = new MinMaxCurve(0f);
        public MinMaxCurve orbitalY = new MinMaxCurve(0f);
        public MinMaxCurve orbitalZ = new MinMaxCurve(0f);


        public MinMaxCurve radial = new MinMaxCurve(0f);


        public MinMaxCurve speedModifier = new MinMaxCurve(1f);
    }



    public ForceOverLifetimeModule forceOverLifetime = new ForceOverLifetimeModule();

    public static class ForceOverLifetimeModule {
        public boolean enabled = false;
        public MinMaxCurve x = new MinMaxCurve(0f);
        public MinMaxCurve y = new MinMaxCurve(0f);
        public MinMaxCurve z = new MinMaxCurve(0f);
        public SimulationSpace space = SimulationSpace.WORLD;
    }



    public ColorOverLifetimeModule colorOverLifetime = new ColorOverLifetimeModule();

    public static class ColorOverLifetimeModule {
        public boolean enabled = true;
        public MinMaxGradient color = new MinMaxGradient(new Color(1, 1, 1, 1));
    }



    public SizeOverLifetimeModule sizeOverLifetime = new SizeOverLifetimeModule();

    public static class SizeOverLifetimeModule {
        public boolean enabled = false;
        public boolean separateAxes = false;
        public MinMaxCurve size = new MinMaxCurve(1f);
        public MinMaxCurve sizeX = new MinMaxCurve(1f);
        public MinMaxCurve sizeY = new MinMaxCurve(1f);
        public MinMaxCurve sizeZ = new MinMaxCurve(1f);
    }



    public RotationOverLifetimeModule rotationOverLifetime = new RotationOverLifetimeModule();

    public static class RotationOverLifetimeModule {
        public boolean enabled = false;
        public boolean separateAxes = false;
        public MinMaxCurve angularVelocity = new MinMaxCurve(45f);
        public MinMaxCurve angularVelocityX = new MinMaxCurve(0f);
        public MinMaxCurve angularVelocityY = new MinMaxCurve(0f);
        public MinMaxCurve angularVelocityZ = new MinMaxCurve(45f);
    }



    public NoiseModule noise = new NoiseModule();

    public static class NoiseModule {
        public boolean enabled = false;
        public float strength = 1f;
        public float frequency = 0.5f;
        public int octaves = 1;
        public float scrollSpeed = 0f;
        public boolean damping = true;


        public boolean separateAxes = false;
        public float strengthX = 1f;
        public float strengthY = 1f;
        public float strengthZ = 1f;


        public boolean remapEnabled = false;
        public MinMaxCurve remapCurve = new MinMaxCurve(1f);
    }



    public CollisionModule collision = new CollisionModule();

    public static class CollisionModule {
        public boolean enabled = false;
        public CollisionMode mode = CollisionMode.WORLD_PHYSICS;


        public float bounce = 0.5f;


        public float lifetimeLoss = 0f;


        public float minKillSpeed = 0f;


        public int maxCollisionShapes = 256;


        public float radiusScale = 1f;


        public float dampen = 0f;


        public CollisionQuality quality = CollisionQuality.MEDIUM;


        public List<CollisionPlane> planes = new ArrayList<>();
    }

    public enum CollisionQuality { LOW, MEDIUM, HIGH }

    public static class CollisionPlane {
        public Vector3 point = new Vector3(0, 0, 0);
        public Vector3 normal = new Vector3(0, 1, 0);
    }



    public SubEmittersModule subEmitters = new SubEmittersModule();

    public static class SubEmittersModule {
        public boolean enabled = false;
        public List<SubEmitterEntry> entries = new ArrayList<>();
    }



    public TrailsModule trails = new TrailsModule();

    public static class TrailsModule {
        public boolean enabled = false;
        public float ratio = 1f;
        public float lifetime = 1f;
        public float minimumVertexDistance = 0.2f;
        public boolean worldSpace = false;
        public boolean dieWithParticles = true;
        public MinMaxGradient colorOverLifetime = new MinMaxGradient(new Color(1, 1, 1, 1));
        public MinMaxCurve widthOverTrail = new MinMaxCurve(1f);
        public boolean inheritParticleColor = true;
        public int textureMode = 0;
    }



    public static class TextureSheetModule {
        public boolean enabled = false;
        public int tilesX = 1;
        public int tilesY = 1;
        public AnimationType animationType = AnimationType.WHOLE_SHEET;
        public MinMaxCurve frameOverTime = new MinMaxCurve(0f, 1f);
        public MinMaxCurve startFrame = new MinMaxCurve(0f);
        public int cycles = 1;
    }

    public enum AnimationType {
        WHOLE_SHEET,
        SINGLE_ROW
    }



    public RendererModule renderer = new RendererModule();

    public static class RendererModule {
        public RenderMode renderMode = RenderMode.BILLBOARD;


        public float lengthScale = 2f;
        public float speedScale = 0f;


        public MeshType meshType = MeshType.CUBE;
        public String meshPath = null;
        public boolean alignToVelocity = false;


        public boolean isAdditive = false;
        public String texturePath = null;
        public int sortMode = 1;
        public float minParticleSize = 0f;
        public float maxParticleSize = 0.5f;
        public boolean castShadows = false;
        public boolean receiveShadows = false;
    }

    public enum MeshType {
        CUBE,
        SPHERE_LOW,
        CYLINDER_LOW,
        CUSTOM
    }




    public static class MinMaxCurve {
        public CurveMode mode = CurveMode.CONSTANT;
        public float constantMin = 0f;
        public float constantMax = 1f;


        public List<ParticleCurvePoint<Float>> curve = new ArrayList<>();
        public List<ParticleCurvePoint<Float>> curveMin = new ArrayList<>();


        public float multiplier = 1f;

        public MinMaxCurve() {}

        public MinMaxCurve(float constant) {
            this.mode = CurveMode.CONSTANT;
            this.constantMax = constant;
            this.constantMin = constant;
        }

        public MinMaxCurve(float min, float max) {
            this.mode = CurveMode.RANDOM_BETWEEN_TWO_CONSTANTS;
            this.constantMin = min;
            this.constantMax = max;
        }


        public float evaluate(float t) {
            return evaluate(t, (float) Math.random());
        }

        public float evaluate(float t, float randomFactor) {
            switch (mode) {
                case CONSTANT:
                    return constantMax * multiplier;

                case RANDOM_BETWEEN_TWO_CONSTANTS:
                    float val = constantMin + (constantMax - constantMin) * randomFactor;
                    return val * multiplier;

                case CURVE:
                    return evaluateCurve(curve, t) * multiplier;

                case RANDOM_BETWEEN_TWO_CURVES:
                    float v1 = evaluateCurve(curveMin, t);
                    float v2 = evaluateCurve(curve, t);
                    return (v1 + (v2 - v1) * randomFactor) * multiplier;

                default:
                    return constantMax * multiplier;
            }
        }

        private float evaluateCurve(List<ParticleCurvePoint<Float>> pts, float t) {
            if (pts == null || pts.isEmpty()) return constantMax;
            if (pts.size() == 1) return pts.get(0).value;

            t = Math.max(0, Math.min(1, t));


            ParticleCurvePoint<Float> prev = pts.get(0);
            ParticleCurvePoint<Float> next = pts.get(pts.size() - 1);

            for (int i = 0; i < pts.size() - 1; i++) {
                if (pts.get(i).time <= t && pts.get(i + 1).time >= t) {
                    prev = pts.get(i);
                    next = pts.get(i + 1);
                    break;
                }
            }

            if (prev == next || prev.time == next.time) return prev.value;

            float alpha = (t - prev.time) / (next.time - prev.time);
            return prev.value + (next.value - prev.value) * alpha;
        }
    }

    public enum CurveMode {
        CONSTANT,
        RANDOM_BETWEEN_TWO_CONSTANTS,
        CURVE,
        RANDOM_BETWEEN_TWO_CURVES
    }


    public static class MinMaxGradient {
        public GradientMode mode = GradientMode.COLOR;
        public Color colorMin = new Color(1, 1, 1, 1);
        public Color colorMax = new Color(1, 1, 1, 1);


        public List<ParticleCurvePoint<Color>> gradient = new ArrayList<>();
        public List<ParticleCurvePoint<Color>> gradientMin = new ArrayList<>();

        public MinMaxGradient() {}

        public MinMaxGradient(Color color) {
            this.mode = GradientMode.COLOR;
            this.colorMax.set(color);
            this.colorMin.set(color);
        }

        public Color evaluate(float t) {
            return evaluate(t, (float) Math.random());
        }

        public Color evaluate(float t, float randomFactor) {
            switch (mode) {
                case COLOR:
                    return new Color(colorMax);

                case RANDOM_BETWEEN_TWO_COLORS:
                    return new Color(colorMin).lerp(colorMax, randomFactor);

                case GRADIENT:
                    return evaluateGradient(gradient, t);

                case RANDOM_BETWEEN_TWO_GRADIENTS:
                    Color c1 = evaluateGradient(gradientMin, t);
                    Color c2 = evaluateGradient(gradient, t);
                    return c1.lerp(c2, randomFactor);

                default:
                    return new Color(colorMax);
            }
        }

        private Color evaluateGradient(List<ParticleCurvePoint<Color>> pts, float t) {
            if (pts == null || pts.isEmpty()) return new Color(colorMax);
            if (pts.size() == 1) return new Color(pts.get(0).value);

            t = Math.max(0, Math.min(1, t));

            ParticleCurvePoint<Color> prev = pts.get(0);
            ParticleCurvePoint<Color> next = pts.get(pts.size() - 1);

            for (int i = 0; i < pts.size() - 1; i++) {
                if (pts.get(i).time <= t && pts.get(i + 1).time >= t) {
                    prev = pts.get(i);
                    next = pts.get(i + 1);
                    break;
                }
            }

            if (prev == next || prev.time == next.time) return new Color(prev.value);

            float alpha = (t - prev.time) / (next.time - prev.time);
            return new Color(prev.value).lerp(next.value, alpha);
        }
    }

    public enum GradientMode {
        COLOR,
        RANDOM_BETWEEN_TWO_COLORS,
        GRADIENT,
        RANDOM_BETWEEN_TWO_GRADIENTS
    }




    public static ParticleSystem3DComponent migrateFromLegacy(ParticleComponent old) {
        if (old == null) return new ParticleSystem3DComponent();

        old.migrateOldDataIfNeeded();

        ParticleSystem3DComponent ps = new ParticleSystem3DComponent();


        ps.looping = old.looping;
        ps.duration = old.duration;
        ps.startLifetime = new MinMaxCurve(old.startLifetime);
        ps.maxParticles = old.maxParticles;


        ps.emission.rateOverTime = new MinMaxCurve(old.emissionRate);


        if (!old.speedGraph.isEmpty()) {
            ps.startSpeed = new MinMaxCurve(old.speedGraph.get(0).value);
            if (old.speedGraph.size() > 1) {
                ps.velocityOverLifetime.enabled = true;
                ps.velocityOverLifetime.speedModifier = new MinMaxCurve();
                ps.velocityOverLifetime.speedModifier.mode = CurveMode.CURVE;
                ps.velocityOverLifetime.speedModifier.curve.addAll(old.speedGraph);
                ps.velocityOverLifetime.speedModifier.multiplier = 1f;
            }
        } else {
            ps.startSpeed = new MinMaxCurve(old.startSpeed);
        }


        ps.shape.enabled = true;
        switch (old.spawnShape) {
            case POINT:
                ps.shape.type = ShapeType.SPHERE;
                ps.shape.sphereRadius = 0.001f;
                break;
            case BOX:
                ps.shape.type = ShapeType.BOX;
                ps.shape.boxSize.set(old.spawnSize.x * 2, old.spawnSize.y * 2, old.spawnSize.z * 2);
                break;
            case SPHERE:
                ps.shape.type = ShapeType.SPHERE;
                ps.shape.sphereRadius = old.spawnSize.x;
                break;
            case CYLINDER:
                ps.shape.type = ShapeType.CONE;
                ps.shape.coneRadius = old.spawnSize.x;
                ps.shape.coneLength = old.spawnSize.y;
                ps.shape.coneAngle = old.coneAngle;
                break;
        }
        if (old.spawnOnSurface) {
            ps.shape.emitFrom = EmitFrom.SURFACE;
        }


        if (!old.sizeGraph.isEmpty()) {
            ps.startSize = new MinMaxCurve(old.baseSize);
            ps.sizeOverLifetime.enabled = true;
            ps.sizeOverLifetime.size = new MinMaxCurve();
            ps.sizeOverLifetime.size.mode = CurveMode.CURVE;
            ps.sizeOverLifetime.size.curve.addAll(old.sizeGraph);
        } else {
            ps.startSize = new MinMaxCurve(old.startSize);
        }


        if (!old.colorGraph.isEmpty()) {
            ps.colorOverLifetime.enabled = true;
            ps.colorOverLifetime.color = new MinMaxGradient();
            ps.colorOverLifetime.color.mode = GradientMode.GRADIENT;
            ps.colorOverLifetime.color.gradient.addAll(old.colorGraph);
        } else {
            ps.startColor = new MinMaxGradient(old.startColor);
        }


        if (!old.gravityGraph.isEmpty()) {
            float avgGravity = 0;
            for (ParticleCurvePoint<Float> p : old.gravityGraph) avgGravity += p.value;
            avgGravity /= old.gravityGraph.size();
            ps.gravityModifier = new MinMaxCurve(avgGravity / 9.81f);
        } else if (old.gravityModifier != 0) {
            ps.gravityModifier = new MinMaxCurve(old.gravityModifier);
        }


        if (!old.turbulenceGraph.isEmpty()) {
            ps.noise.enabled = true;
            float avgTurb = 0;
            for (ParticleCurvePoint<Float> p : old.turbulenceGraph) avgTurb += p.value;
            avgTurb /= old.turbulenceGraph.size();
            ps.noise.strength = avgTurb;
            ps.noise.frequency = 0.5f;
        }


        if (!old.vortexGraph.isEmpty()) {
            ps.velocityOverLifetime.enabled = true;
            float avgVortex = 0;
            for (ParticleCurvePoint<Float> p : old.vortexGraph) avgVortex += p.value;
            avgVortex /= old.vortexGraph.size();
            ps.velocityOverLifetime.orbitalY = new MinMaxCurve(avgVortex);
        }


        if (!old.rotationGraph.isEmpty()) {
            ps.rotationOverLifetime.enabled = true;
            ps.rotationOverLifetime.angularVelocity = new MinMaxCurve();
            ps.rotationOverLifetime.angularVelocity.mode = CurveMode.CURVE;
            ps.rotationOverLifetime.angularVelocity.curve.addAll(old.rotationGraph);
        }


        ps.renderer.isAdditive = old.isAdditive;
        ps.renderer.texturePath = old.texturePath;

        return ps;
    }
}