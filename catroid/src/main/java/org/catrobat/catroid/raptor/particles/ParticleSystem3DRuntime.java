package org.catrobat.catroid.raptor.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import org.catrobat.catroid.raptor.ParticleCurvePoint;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.*;
import org.catrobat.catroid.raptor.ThreeDManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.EmissionModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.ShapeModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.VelocityOverLifetimeModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.ForceOverLifetimeModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.NoiseModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.CollisionModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.CollisionPlane;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.SubEmitterEntry;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.SubEmitterTrigger;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.RendererModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.TextureSheetModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.TrailsModule;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.Burst;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.MinMaxCurve;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.MinMaxGradient;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.SimulationSpace;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.RenderMode;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.CollisionMode;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.CollisionQuality;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.EmitFrom;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent.ShapeType;


public class ParticleSystem3DRuntime implements Disposable {

    private boolean glInitialized = false;
    private float[] rotationX, rotationY, rotationZ;
    private boolean meshBatcherDirty = false;

    

    private static final int FLOATS_PER_VERTEX = 9; 
    private static final int VERTICES_PER_PARTICLE = 4;
    private static final int INDICES_PER_PARTICLE = 6;

    private MeshParticleBatcher meshBatcher;
    private boolean meshBatcherReady = false;

    
    private int maxParticles;
    private int aliveCount = 0;

    
    private float[] posX, posY, posZ;          
    private float[] velX, velY, velZ;          
    private float[] life;                       
    private float[] maxLife;                    
    private float[] randomSeed;                

    
    private float[] size;                       
    private float[] startSizeVal;              
    private float[] rotation;                   
    private float[] rotVelocity;               
    private float[] colorR, colorG, colorB, colorA;

    
    private float[] prevPosX, prevPosY, prevPosZ;

    
    private List<List<TrailPoint>> trailData;
    private boolean[] hasTrail;

    
    private boolean[] collidedThisFrame;
    private float[] collisionNormalX, collisionNormalY, collisionNormalZ;

    

    private ParticleSystem3DComponent config;
    private Matrix4 emitterTransform = new Matrix4();
    private Matrix4 prevEmitterTransform = new Matrix4();
    private Vector3 emitterWorldPos = new Vector3();
    private Vector3 prevEmitterWorldPos = new Vector3();

    private float emissionAccumulator = 0f;
    private float distanceAccumulator = 0f;
    private float elapsedTime = 0f;
    private boolean isPlaying = false;
    private boolean hasPrewarmed = false;

    private Random rng = new Random();

    
    private Mesh billboardMesh;
    private float[] vertices;
    private short[] indices;
    private Texture texture;
    private ShaderProgram shader;
    private boolean isAdditive;

    
    private Camera camera;
    private ThreeDManager engineRef;   
    private String ownerObjectId;

    
    public interface SubEmitterCallback {
        void onSubEmit(String subEmitterObjectId, Vector3 position, Vector3 velocity);
    }
    private SubEmitterCallback subEmitterCallback;

    
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();
    private final Vector3 tmpV3 = new Vector3();
    private final Vector3 tmpV4 = new Vector3();
    private final Quaternion tmpQ = new Quaternion();
    private final Matrix4 tmpMat = new Matrix4();
    private final Color tmpColor = new Color();

    

    private static class TrailPoint {
        float x, y, z;
        float age;
        float r, g, b, a;
        float width;
    }

    

    public ParticleSystem3DRuntime(ParticleSystem3DComponent config, String ownerObjectId,
                                   Camera camera, ThreeDManager engine) {
        this.config = config;
        this.ownerObjectId = ownerObjectId;
        this.camera = camera;
        this.engineRef = engine;
        this.maxParticles = config.maxParticles;

        allocateArrays(maxParticles);

        this.isPlaying = config.isPlaying;
    }

    private void ensureGLInitialized() {
        if (glInitialized) return;

        buildMesh(maxParticles);
        buildShader();

        if (config.renderer.renderMode == ParticleSystem3DComponent.RenderMode.MESH) {
            initMeshBatcher();
        }

        glInitialized = true;
    }

    private void initMeshBatcher() {
        if (meshBatcher != null) meshBatcher.dispose();
        meshBatcher = new MeshParticleBatcher(maxParticles);

        if (config.renderer.meshType == ParticleSystem3DComponent.MeshType.CUSTOM
                && config.renderer.meshPath != null && !config.renderer.meshPath.isEmpty()) {
            
            try {
                loadCustomMeshForBatcher(config.renderer.meshPath);
            } catch (Exception e) {
                Gdx.app.error("ParticleRuntime", "Failed to load custom mesh: " + config.renderer.meshPath, e);
                meshBatcher.loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
            }
        } else {
            meshBatcher.loadPrimitive(config.renderer.meshType);
        }
    }

    private void loadCustomMeshForBatcher(String meshPath) {
        java.io.File modelFile = org.catrobat.catroid.ProjectManager.getInstance()
                .getCurrentProject().getFile(meshPath);

        if (modelFile == null || !modelFile.exists()) {
            Gdx.app.error("ParticleRuntime", "Mesh file not found: " + meshPath);
            meshBatcher.loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
            return;
        }

        com.badlogic.gdx.files.FileHandle fh = Gdx.files.absolute(modelFile.getAbsolutePath());
        String lowerName = meshPath.toLowerCase();
        Model model = null;

        try {
            if (lowerName.endsWith(".glb")) {
                net.mgsx.gltf.scene3d.scene.SceneAsset asset =
                        new net.mgsx.gltf.loaders.glb.GLBLoader().load(fh, true);
                if (asset != null && asset.scene != null) {
                    model = asset.scene.model;
                }
            } else if (lowerName.endsWith(".gltf")) {
                net.mgsx.gltf.scene3d.scene.SceneAsset asset =
                        new net.mgsx.gltf.loaders.gltf.GLTFLoader().load(fh, true);
                if (asset != null && asset.scene != null) {
                    model = asset.scene.model;
                }
            } else if (lowerName.endsWith(".obj")) {
                com.badlogic.gdx.graphics.g3d.loader.ObjLoader loader =
                        new com.badlogic.gdx.graphics.g3d.loader.ObjLoader();
                model = loader.loadModel(fh, true);
            }
        } catch (Exception e) {
            Gdx.app.error("ParticleRuntime", "Error loading model: " + meshPath, e);
        }

        if (model != null) {
            meshBatcher.loadFromModel(model);
            
            
            
            Gdx.app.log("ParticleRuntime", "Custom particle mesh loaded: " + meshPath);
        } else {
            Gdx.app.error("ParticleRuntime", "Model was null, fallback to cube");
            meshBatcher.loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
        }
    }

    
    public void setMeshModel(com.badlogic.gdx.graphics.g3d.Model model) {
        if (meshBatcher == null) {
            meshBatcher = new MeshParticleBatcher(maxParticles);
        }
        meshBatcher.loadFromModel(model);
    }


    private void allocateArrays(int max) {
        posX = new float[max]; posY = new float[max]; posZ = new float[max];
        velX = new float[max]; velY = new float[max]; velZ = new float[max];
        life = new float[max]; maxLife = new float[max]; randomSeed = new float[max];
        size = new float[max]; startSizeVal = new float[max];
        rotation = new float[max]; rotVelocity = new float[max];
        colorR = new float[max]; colorG = new float[max];
        colorB = new float[max]; colorA = new float[max];
        prevPosX = new float[max]; prevPosY = new float[max]; prevPosZ = new float[max];
        collidedThisFrame = new boolean[max];
        collisionNormalX = new float[max];
        collisionNormalY = new float[max];
        collisionNormalZ = new float[max];
        hasTrail = new boolean[max];
        rotationX = new float[max];
        rotationY = new float[max];
        rotationZ = new float[max];

        trailData = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            trailData.add(new ArrayList<>());
        }
    }

    

    public void setTransform(Matrix4 transform) {
        prevEmitterTransform.set(emitterTransform);
        emitterTransform.set(transform);
        prevEmitterWorldPos.set(emitterWorldPos);
        emitterTransform.getTranslation(emitterWorldPos);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public void setSubEmitterCallback(SubEmitterCallback cb) {
        this.subEmitterCallback = cb;
    }

    public void play() {
        isPlaying = true;
        elapsedTime = 0;
        emissionAccumulator = 0;
        hasPrewarmed = false;
    }

    public void stop() {
        isPlaying = false;
    }

    public void clear() {
        aliveCount = 0;
    }

    public int getAliveCount() {
        return aliveCount;
    }

    

    public void update(float delta) {
        if (!isPlaying && aliveCount == 0) return;

        
        if (config.prewarm && config.looping && !hasPrewarmed) {
            hasPrewarmed = true;
            float prewarmTime = config.duration;
            float step = 1f / 60f;
            for (float t = 0; t < prewarmTime; t += step) {
                updateInternal(step);
            }
            return;
        }

        updateInternal(delta);
    }

    private void updateInternal(float delta) {
        elapsedTime += delta;

        
        boolean canEmit = isPlaying;
        if (!config.looping && elapsedTime > config.duration + config.startDelay.evaluate(0)) {
            canEmit = false;
        }
        if (config.looping && elapsedTime > config.duration) {
            elapsedTime -= config.duration;
        }

        
        if (canEmit && config.emission.enabled) {
            doEmission(delta);
        }

        
        for (int i = aliveCount - 1; i >= 0; i--) {
            life[i] -= delta;

            if (life[i] <= 0) {
                
                onParticleDeath(i);
                killParticle(i);
                continue;
            }

            float normalizedLife = 1f - (life[i] / maxLife[i]); 
            float rand = randomSeed[i];

            
            prevPosX[i] = posX[i];
            prevPosY[i] = posY[i];
            prevPosZ[i] = posZ[i];

            
            if (config.velocityOverLifetime.enabled) {
                VelocityOverLifetimeModule vol = config.velocityOverLifetime;
                velX[i] += vol.x.evaluate(normalizedLife, rand) * delta;
                velY[i] += vol.y.evaluate(normalizedLife, rand) * delta;
                velZ[i] += vol.z.evaluate(normalizedLife, rand) * delta;

                
                float orbY = vol.orbitalY.evaluate(normalizedLife, rand);
                if (orbY != 0) {
                    applyOrbitalVelocity(i, 0, orbY, 0, delta);
                }
                float orbX = vol.orbitalX.evaluate(normalizedLife, rand);
                if (orbX != 0) {
                    applyOrbitalVelocity(i, orbX, 0, 0, delta);
                }

                
                float radial = vol.radial.evaluate(normalizedLife, rand);
                if (radial != 0) {
                    tmpV1.set(posX[i], posY[i], posZ[i]).sub(emitterWorldPos);
                    float len = tmpV1.len();
                    if (len > 0.001f) {
                        tmpV1.nor().scl(radial * delta);
                        velX[i] += tmpV1.x;
                        velY[i] += tmpV1.y;
                        velZ[i] += tmpV1.z;
                    }
                }

                
                float speedMod = vol.speedModifier.evaluate(normalizedLife, rand);
                if (speedMod != 1f) {
                    velX[i] *= speedMod;
                    velY[i] *= speedMod;
                    velZ[i] *= speedMod;
                }
            }

            
            if (config.forceOverLifetime.enabled) {
                ForceOverLifetimeModule fol = config.forceOverLifetime;
                velX[i] += fol.x.evaluate(normalizedLife, rand) * delta;
                velY[i] += fol.y.evaluate(normalizedLife, rand) * delta;
                velZ[i] += fol.z.evaluate(normalizedLife, rand) * delta;
            }

            
            float grav = config.gravityModifier.evaluate(normalizedLife, rand);
            if (grav != 0) {
                velY[i] -= 9.81f * grav * delta;
            }

            
            if (config.noise.enabled) {
                applyNoise(i, normalizedLife, delta);
            }

            
            posX[i] += velX[i] * delta;
            posY[i] += velY[i] * delta;
            posZ[i] += velZ[i] * delta;

            
            collidedThisFrame[i] = false;
            if (config.collision.enabled) {
                handleCollision(i, delta);
            }

            
            if (config.sizeOverLifetime.enabled) {
                size[i] = startSizeVal[i] * config.sizeOverLifetime.size.evaluate(normalizedLife, rand);
            }

            
            if (config.colorOverLifetime.enabled) {
                Color c = config.colorOverLifetime.color.evaluate(normalizedLife, rand);
                colorR[i] = c.r; colorG[i] = c.g; colorB[i] = c.b; colorA[i] = c.a;
            }

            
            if (config.rotationOverLifetime.enabled) {
                float angVel = config.rotationOverLifetime.angularVelocity.evaluate(normalizedLife, rand);
                rotation[i] += angVel * delta;
                if (config.rotationOverLifetime.separateAxes) {
                    rotationX[i] += config.rotationOverLifetime.angularVelocityX.evaluate(normalizedLife, rand) * delta;
                    rotationY[i] += config.rotationOverLifetime.angularVelocityY.evaluate(normalizedLife, rand) * delta;
                    rotationZ[i] += config.rotationOverLifetime.angularVelocityZ.evaluate(normalizedLife, rand) * delta;
                } else {
                    rotationX[i] += 0;
                    rotationY[i] += 0;
                    rotationZ[i] += angVel * delta;
                }
            }

            
            if (config.trails.enabled && hasTrail[i]) {
                updateTrail(i, normalizedLife, delta);
            }
        }

        
        
    }

    

    private void doEmission(float delta) {
        EmissionModule em = config.emission;

        
        float rateTime = em.rateOverTime.evaluate(elapsedTime / config.duration);
        emissionAccumulator += rateTime * delta;

        
        float distMoved = emitterWorldPos.dst(prevEmitterWorldPos);
        float rateDist = em.rateOverDistance.evaluate(elapsedTime / config.duration);
        emissionAccumulator += rateDist * distMoved;

        int toEmit = (int) emissionAccumulator;
        emissionAccumulator -= toEmit;

        
        for (Burst burst : em.bursts) {
            
            float burstTime = burst.time;
            if (elapsedTime >= burstTime && elapsedTime - delta < burstTime) {
                if (rng.nextFloat() <= burst.probability) {
                    toEmit += (int) burst.count.evaluate(0, rng.nextFloat());
                }
            }
        }

        for (int i = 0; i < toEmit && aliveCount < maxParticles; i++) {
            emitParticle();
        }
    }

    public void emitBurstAt(Vector3 worldPosition, int count) {
        Vector3 savedPos = new Vector3(emitterWorldPos);
        Matrix4 savedTransform = new Matrix4(emitterTransform);

        emitterWorldPos.set(worldPosition);
        emitterTransform.idt().setToTranslation(worldPosition);

        for (int i = 0; i < count && aliveCount < maxParticles; i++) {
            emitParticle();
        }

        emitterWorldPos.set(savedPos);
        emitterTransform.set(savedTransform);
    }

    public ParticleSystem3DComponent getConfig() {
        return config;
    }

    private void emitParticle() {
        int idx = aliveCount;
        aliveCount++;

        float rand = rng.nextFloat();
        randomSeed[idx] = rand;

        maxLife[idx] = config.startLifetime.evaluate(0, rand);
        life[idx] = maxLife[idx];

        startSizeVal[idx] = config.startSize.evaluate(0, rand);
        size[idx] = startSizeVal[idx];

        Color c = config.startColor.evaluate(0, rand);
        colorR[idx] = c.r; colorG[idx] = c.g; colorB[idx] = c.b; colorA[idx] = c.a;

        rotation[idx] = config.startRotation.evaluate(0, rand);
        if (config.threeDStartRotation) {
            rotationX[idx] = config.startRotationX.evaluate(0, rand);
            rotationY[idx] = config.startRotationY.evaluate(0, rand);
            rotationZ[idx] = config.startRotationZ.evaluate(0, rand);
        } else {
            rotationX[idx] = 0;
            rotationY[idx] = 0;
            rotationZ[idx] = rotation[idx];
        }
        if (config.flipRotation > 0 && rng.nextFloat() < config.flipRotation) {
            rotation[idx] += 180f;
        }
        rotVelocity[idx] = 0;

        
        Vector3 spawnPos = tmpV1;
        Vector3 spawnDir = tmpV2;
        generateFromShape(spawnPos, spawnDir);

        
        
        spawnPos.mul(emitterTransform);
        spawnDir.rot(emitterTransform).nor();
        

        posX[idx] = spawnPos.x;
        posY[idx] = spawnPos.y;
        posZ[idx] = spawnPos.z;
        prevPosX[idx] = spawnPos.x;
        prevPosY[idx] = spawnPos.y;
        prevPosZ[idx] = spawnPos.z;

        
        float speed = config.startSpeed.evaluate(0, rand);
        velX[idx] = spawnDir.x * speed;
        velY[idx] = spawnDir.y * speed;
        velZ[idx] = spawnDir.z * speed;

        
        if (config.trails.enabled && trailData != null) {
            hasTrail[idx] = rng.nextFloat() <= config.trails.ratio;
            if (hasTrail[idx] && idx < trailData.size()) {
                trailData.get(idx).clear();
                TrailPoint tp = new TrailPoint();
                tp.x = posX[idx]; tp.y = posY[idx]; tp.z = posZ[idx];
                tp.age = 0; tp.r = colorR[idx]; tp.g = colorG[idx];
                tp.b = colorB[idx]; tp.a = colorA[idx]; tp.width = size[idx];
                trailData.get(idx).add(tp);
            }
        } else {
            hasTrail[idx] = false;
        }

        collidedThisFrame[idx] = false;

        
        if (config.subEmitters.enabled && subEmitterCallback != null) {
            for (SubEmitterEntry entry : config.subEmitters.entries) {
                if (entry.trigger == SubEmitterTrigger.BIRTH && rng.nextFloat() <= entry.probability) {
                    subEmitterCallback.onSubEmit(entry.subEmitterObjectId,
                            new Vector3(posX[idx], posY[idx], posZ[idx]),
                            new Vector3(velX[idx], velY[idx], velZ[idx]));
                }
            }
        }
    }

    

    private void generateFromShape(Vector3 outPos, Vector3 outDir) {
        ShapeModule s = config.shape;
        if (!s.enabled) {
            outPos.set(0, 0, 0);
            outDir.set(0, 1, 0);
            return;
        }

        switch (s.type) {
            case CONE:
                generateCone(outPos, outDir, s);
                break;
            case SPHERE:
                generateSphere(outPos, outDir, s.sphereRadius, false);
                break;
            case HEMISPHERE:
                generateSphere(outPos, outDir, s.sphereRadius, true);
                break;
            case BOX:
                generateBox(outPos, outDir, s);
                break;
            case CIRCLE:
                generateCircle(outPos, outDir, s);
                break;
            case EDGE:
                generateEdge(outPos, outDir, s);
                break;
            default:
                outPos.set(0, 0, 0);
                outDir.set(0, 1, 0);
                break;
        }

        
        if (s.randomizeDirection) {
            Vector3 randomDir = tmpV3.set(
                    rng.nextFloat() * 2 - 1,
                    rng.nextFloat() * 2 - 1,
                    rng.nextFloat() * 2 - 1).nor();
            outDir.lerp(randomDir, 0.5f).nor();
        }

        
        if (s.spherizeDirection > 0) {
            Vector3 fromCenter = tmpV3.set(outPos).nor();
            outDir.lerp(fromCenter, s.spherizeDirection).nor();
        }

        
        if (s.randomizePosition > 0) {
            outPos.x += (rng.nextFloat() * 2 - 1) * s.randomizePosition;
            outPos.y += (rng.nextFloat() * 2 - 1) * s.randomizePosition;
            outPos.z += (rng.nextFloat() * 2 - 1) * s.randomizePosition;
        }
    }

    private void generateCone(Vector3 outPos, Vector3 outDir, ShapeModule s) {
        float angle = rng.nextFloat() * MathUtils.PI2;
        float radiusFraction;

        if (s.emitFrom == EmitFrom.SURFACE) {
            radiusFraction = 1f;
        } else if (s.emitFrom == EmitFrom.EDGE) {
            radiusFraction = 1f;
        } else {
            radiusFraction = (float) Math.sqrt(rng.nextFloat()); 
        }

        float spawnRadius = s.coneRadius * radiusFraction;
        outPos.set(
                MathUtils.cos(angle) * spawnRadius,
                0,
                MathUtils.sin(angle) * spawnRadius
        );

        
        float halfAngleRad = s.coneAngle * MathUtils.degreesToRadians * 0.5f;
        float dirRadius = MathUtils.sin(halfAngleRad) * radiusFraction;
        float dirY = MathUtils.cos(halfAngleRad);

        outDir.set(
                MathUtils.cos(angle) * dirRadius,
                dirY,
                MathUtils.sin(angle) * dirRadius
        ).nor();

        
        if (s.emitFrom == EmitFrom.VOLUME && s.coneLength > 0) {
            float t = rng.nextFloat();
            outPos.y += t * s.coneLength;
            
            float widening = t * MathUtils.tan(halfAngleRad) * s.coneLength;
            outPos.x += MathUtils.cos(angle) * widening;
            outPos.z += MathUtils.sin(angle) * widening;
        }
    }

    private void generateSphere(Vector3 outPos, Vector3 outDir, float radius, boolean hemisphere) {
        
        float theta = rng.nextFloat() * MathUtils.PI2;
        float phi = (float) Math.acos(hemisphere ? rng.nextFloat() : (2.0f * rng.nextFloat() - 1.0f));

        float sinPhi = MathUtils.sin(phi);
        outDir.set(
                sinPhi * MathUtils.cos(theta),
                MathUtils.cos(phi),
                sinPhi * MathUtils.sin(theta)
        ).nor();

        if (config.shape.emitFrom == EmitFrom.SURFACE) {
            outPos.set(outDir).scl(radius);
        } else {
            float r = radius * (float) Math.cbrt(rng.nextFloat()); 
            outPos.set(outDir).scl(r);
        }
    }

    private void generateBox(Vector3 outPos, Vector3 outDir, ShapeModule s) {
        Vector3 half = tmpV3.set(s.boxSize).scl(0.5f);

        if (s.emitFrom == EmitFrom.SURFACE) {
            
            int face = rng.nextInt(6);
            float u = rng.nextFloat() * 2 - 1;
            float v = rng.nextFloat() * 2 - 1;
            switch (face) {
                case 0: outPos.set(half.x, u * half.y, v * half.z); outDir.set(1, 0, 0); break;
                case 1: outPos.set(-half.x, u * half.y, v * half.z); outDir.set(-1, 0, 0); break;
                case 2: outPos.set(u * half.x, half.y, v * half.z); outDir.set(0, 1, 0); break;
                case 3: outPos.set(u * half.x, -half.y, v * half.z); outDir.set(0, -1, 0); break;
                case 4: outPos.set(u * half.x, v * half.y, half.z); outDir.set(0, 0, 1); break;
                case 5: outPos.set(u * half.x, v * half.y, -half.z); outDir.set(0, 0, -1); break;
            }
        } else {
            outPos.set(
                    (rng.nextFloat() * 2 - 1) * half.x,
                    (rng.nextFloat() * 2 - 1) * half.y,
                    (rng.nextFloat() * 2 - 1) * half.z
            );
            outDir.set(0, 1, 0); 
        }
    }

    private void generateCircle(Vector3 outPos, Vector3 outDir, ShapeModule s) {
        float arcRad = s.circleArc * MathUtils.degreesToRadians;
        float angle = rng.nextFloat() * arcRad;
        float r = (s.emitFrom == EmitFrom.SURFACE) ? s.circleRadius :
                s.circleRadius * (float) Math.sqrt(rng.nextFloat());
        outPos.set(MathUtils.cos(angle) * r, 0, MathUtils.sin(angle) * r);
        outDir.set(0, 1, 0);
    }

    private void generateEdge(Vector3 outPos, Vector3 outDir, ShapeModule s) {
        float t = rng.nextFloat() - 0.5f;
        outPos.set(t * s.edgeLength, 0, 0);
        outDir.set(0, 1, 0);
    }

    

    private void applyOrbitalVelocity(int idx, float orbX, float orbY, float orbZ, float delta) {
        
        tmpV1.set(posX[idx] - emitterWorldPos.x,
                posY[idx] - emitterWorldPos.y,
                posZ[idx] - emitterWorldPos.z);

        if (orbY != 0) {
            float angle = orbY * delta * MathUtils.degreesToRadians;
            float cosA = MathUtils.cos(angle);
            float sinA = MathUtils.sin(angle);
            float newX = tmpV1.x * cosA - tmpV1.z * sinA;
            float newZ = tmpV1.x * sinA + tmpV1.z * cosA;
            posX[idx] = emitterWorldPos.x + newX;
            posZ[idx] = emitterWorldPos.z + newZ;
        }
        if (orbX != 0) {
            float angle = orbX * delta * MathUtils.degreesToRadians;
            float cosA = MathUtils.cos(angle);
            float sinA = MathUtils.sin(angle);
            float newY = tmpV1.y * cosA - tmpV1.z * sinA;
            float newZ = tmpV1.y * sinA + tmpV1.z * cosA;
            posY[idx] = emitterWorldPos.y + newY;
            posZ[idx] = emitterWorldPos.z + newZ;
        }
    }

    

    private void applyNoise(int idx, float normalizedLife, float delta) {
        NoiseModule n = config.noise;
        float strength = n.strength;
        if (n.damping) {
            strength *= (1f - normalizedLife); 
        }

        
        float freq = n.frequency;
        float time = elapsedTime * n.scrollSpeed;
        float seed = randomSeed[idx] * 1000f;

        float noiseX = (float) Math.sin((posX[idx] + seed) * freq + time) *
                (float) Math.cos((posY[idx] + seed * 0.7f) * freq * 1.3f + time * 0.8f);
        float noiseY = (float) Math.sin((posY[idx] + seed * 1.1f) * freq + time * 1.1f) *
                (float) Math.cos((posZ[idx] + seed * 0.5f) * freq * 0.9f + time * 1.2f);
        float noiseZ = (float) Math.sin((posZ[idx] + seed * 0.9f) * freq + time * 0.7f) *
                (float) Math.cos((posX[idx] + seed * 1.3f) * freq * 1.1f + time * 0.9f);

        
        float amplitude = 1f;
        float totalNX = noiseX, totalNY = noiseY, totalNZ = noiseZ;
        for (int oct = 1; oct < n.octaves; oct++) {
            amplitude *= 0.5f;
            float f = freq * (1 << oct);
            totalNX += amplitude * (float) Math.sin((posX[idx] + seed) * f + time);
            totalNY += amplitude * (float) Math.sin((posY[idx] + seed) * f + time);
            totalNZ += amplitude * (float) Math.sin((posZ[idx] + seed) * f + time);
        }

        float sx = n.separateAxes ? n.strengthX : strength;
        float sy = n.separateAxes ? n.strengthY : strength;
        float sz = n.separateAxes ? n.strengthZ : strength;

        velX[idx] += totalNX * sx * delta;
        velY[idx] += totalNY * sy * delta;
        velZ[idx] += totalNZ * sz * delta;
    }

    

    private void handleCollision(int idx, float delta) {
        CollisionModule col = config.collision;

        if (col.mode == CollisionMode.PLANES) {
            handlePlaneCollision(idx, col);
        } else if (col.mode == CollisionMode.WORLD_PHYSICS) {
            handleWorldCollision(idx, col, delta);
        }
    }

    private void handlePlaneCollision(int idx, CollisionModule col) {
        for (CollisionPlane plane : col.planes) {
            
            tmpV1.set(posX[idx] - plane.point.x,
                    posY[idx] - plane.point.y,
                    posZ[idx] - plane.point.z);
            float dist = tmpV1.dot(plane.normal);
            float particleRadius = size[idx] * 0.5f * col.radiusScale;

            if (dist < particleRadius) {
                
                posX[idx] += plane.normal.x * (particleRadius - dist);
                posY[idx] += plane.normal.y * (particleRadius - dist);
                posZ[idx] += plane.normal.z * (particleRadius - dist);

                
                reflectVelocity(idx, plane.normal.x, plane.normal.y, plane.normal.z, col);
                collidedThisFrame[idx] = true;
                onParticleCollision(idx);
            }
        }
    }

    private void handleWorldCollision(int idx, CollisionModule col, float delta) {
        if (engineRef == null) return;

        
        int skipFrames = (col.quality == CollisionQuality.LOW) ? 4 :
                (col.quality == CollisionQuality.MEDIUM) ? 2 : 1;
        int frameHash = (int)(elapsedTime * 60) + idx;
        if (frameHash % skipFrames != 0) return;

        
        tmpV1.set(prevPosX[idx], prevPosY[idx], prevPosZ[idx]);
        tmpV2.set(posX[idx] - prevPosX[idx], posY[idx] - prevPosY[idx], posZ[idx] - prevPosZ[idx]);
        float moveLen = tmpV2.len();

        if (moveLen < 0.001f) return;

        String rayName = "particle_col_" + ownerObjectId + "_" + idx;
        engineRef.castRay(rayName, tmpV1, tmpV2.nor().scl(moveLen + size[idx] * col.radiusScale));

        if (engineRef.getRayDidHit(rayName)) {
            float hitDist = engineRef.getRaycastDistance(rayName);
            if (hitDist <= moveLen + size[idx] * col.radiusScale * 0.5f) {
                
                posX[idx] = engineRef.getRayHitPointX(rayName);
                posY[idx] = engineRef.getRayHitPointY(rayName);
                posZ[idx] = engineRef.getRayHitPointZ(rayName);

                float nx = engineRef.getRayHitNormalX(rayName);
                float ny = engineRef.getRayHitNormalY(rayName);
                float nz = engineRef.getRayHitNormalZ(rayName);

                
                posX[idx] += nx * size[idx] * col.radiusScale * 0.5f;
                posY[idx] += ny * size[idx] * col.radiusScale * 0.5f;
                posZ[idx] += nz * size[idx] * col.radiusScale * 0.5f;

                reflectVelocity(idx, nx, ny, nz, col);
                collidedThisFrame[idx] = true;
                collisionNormalX[idx] = nx;
                collisionNormalY[idx] = ny;
                collisionNormalZ[idx] = nz;

                
                life[idx] -= maxLife[idx] * col.lifetimeLoss;

                
                float speed = (float) Math.sqrt(velX[idx]*velX[idx] + velY[idx]*velY[idx] + velZ[idx]*velZ[idx]);
                if (speed < col.minKillSpeed) {
                    life[idx] = 0;
                }

                onParticleCollision(idx);
            }
        }
    }

    private void reflectVelocity(int idx, float nx, float ny, float nz, CollisionModule col) {
        
        float dot = velX[idx] * nx + velY[idx] * ny + velZ[idx] * nz;
        velX[idx] = (velX[idx] - 2 * dot * nx) * col.bounce;
        velY[idx] = (velY[idx] - 2 * dot * ny) * col.bounce;
        velZ[idx] = (velZ[idx] - 2 * dot * nz) * col.bounce;

        
        if (col.dampen > 0) {
            velX[idx] *= (1f - col.dampen);
            velY[idx] *= (1f - col.dampen);
            velZ[idx] *= (1f - col.dampen);
        }
    }

    

    private void onParticleDeath(int idx) {
        if (config.subEmitters.enabled && subEmitterCallback != null) {
            for (SubEmitterEntry entry : config.subEmitters.entries) {
                if (entry.trigger == SubEmitterTrigger.DEATH && rng.nextFloat() <= entry.probability) {
                    subEmitterCallback.onSubEmit(entry.subEmitterObjectId,
                            new Vector3(posX[idx], posY[idx], posZ[idx]),
                            new Vector3(velX[idx], velY[idx], velZ[idx]));
                }
            }
        }
    }

    private void onParticleCollision(int idx) {
        if (config.subEmitters.enabled && subEmitterCallback != null) {
            for (SubEmitterEntry entry : config.subEmitters.entries) {
                if (entry.trigger == SubEmitterTrigger.COLLISION && rng.nextFloat() <= entry.probability) {
                    subEmitterCallback.onSubEmit(entry.subEmitterObjectId,
                            new Vector3(posX[idx], posY[idx], posZ[idx]),
                            new Vector3(collisionNormalX[idx], collisionNormalY[idx], collisionNormalZ[idx]));
                }
            }
        }
    }

    private void killParticle(int idx) {
        
        int last = aliveCount - 1;
        if (idx != last) {
            posX[idx] = posX[last]; posY[idx] = posY[last]; posZ[idx] = posZ[last];
            velX[idx] = velX[last]; velY[idx] = velY[last]; velZ[idx] = velZ[last];
            prevPosX[idx] = prevPosX[last]; prevPosY[idx] = prevPosY[last]; prevPosZ[idx] = prevPosZ[last];
            life[idx] = life[last]; maxLife[idx] = maxLife[last]; randomSeed[idx] = randomSeed[last];
            size[idx] = size[last]; startSizeVal[idx] = startSizeVal[last];
            rotation[idx] = rotation[last]; rotVelocity[idx] = rotVelocity[last];
            rotationX[idx] = rotationX[last];
            rotationY[idx] = rotationY[last];
            rotationZ[idx] = rotationZ[last];
            colorR[idx] = colorR[last]; colorG[idx] = colorG[last];
            colorB[idx] = colorB[last]; colorA[idx] = colorA[last];
            collidedThisFrame[idx] = collidedThisFrame[last];
            hasTrail[idx] = hasTrail[last];
            if (trailData != null && idx < trailData.size() && last < trailData.size()) {
                List<TrailPoint> tmp = trailData.get(idx);
                trailData.set(idx, trailData.get(last));
                trailData.set(last, tmp);
            }
        }
        aliveCount--;
    }

    

    private void updateTrail(int idx, float normalizedLife, float delta) {
        if (trailData == null || idx >= trailData.size()) return;

        List<TrailPoint> trail = trailData.get(idx);
        if (trail == null) {
            trailData.set(idx, new ArrayList<>());
            trail = trailData.get(idx);
        }

        
        float trailMaxLife = maxLife[idx] * config.trails.lifetime;
        for (int i = trail.size() - 1; i >= 0; i--) {
            trail.get(i).age += delta;
            if (trail.get(i).age > trailMaxLife) {
                trail.remove(i);
            }
        }

        
        TrailPoint lastPoint = trail.isEmpty() ? null : trail.get(trail.size() - 1);
        float dist = (lastPoint == null) ? Float.MAX_VALUE :
                (float) Math.sqrt(
                        (posX[idx] - lastPoint.x) * (posX[idx] - lastPoint.x) +
                                (posY[idx] - lastPoint.y) * (posY[idx] - lastPoint.y) +
                                (posZ[idx] - lastPoint.z) * (posZ[idx] - lastPoint.z));

        if (dist >= config.trails.minimumVertexDistance) {
            TrailPoint tp = new TrailPoint();
            tp.x = posX[idx]; tp.y = posY[idx]; tp.z = posZ[idx];
            tp.age = 0;

            if (config.trails.inheritParticleColor) {
                tp.r = colorR[idx]; tp.g = colorG[idx];
                tp.b = colorB[idx]; tp.a = colorA[idx];
            } else {
                Color tc = config.trails.colorOverLifetime.evaluate(normalizedLife);
                tp.r = tc.r; tp.g = tc.g; tp.b = tc.b; tp.a = tc.a;
            }

            tp.width = size[idx] * config.trails.widthOverTrail.evaluate(
                    trail.isEmpty() ? 0 : 1f);
            trail.add(tp);
        }
    }

    

    private void buildMesh(int maxParticles) {
        int maxVertices = maxParticles * VERTICES_PER_PARTICLE;
        int maxIndices = maxParticles * INDICES_PER_PARTICLE;

        vertices = new float[maxVertices * FLOATS_PER_VERTEX];
        indices = new short[maxIndices];

        
        for (int i = 0; i < maxParticles; i++) {
            int vi = i * 4;
            int ii = i * 6;
            indices[ii]     = (short) vi;
            indices[ii + 1] = (short) (vi + 1);
            indices[ii + 2] = (short) (vi + 2);
            indices[ii + 3] = (short) (vi + 2);
            indices[ii + 4] = (short) (vi + 3);
            indices[ii + 5] = (short) vi;
        }

        billboardMesh = new Mesh(false, maxVertices, maxIndices,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"));

        billboardMesh.setIndices(indices);
    }

    private void buildShader() {
        String vertexShader =
                "attribute vec3 a_position;\n" +
                        "attribute vec2 a_texCoord0;\n" +
                        "attribute vec4 a_color;\n" +
                        "uniform mat4 u_projViewTrans;\n" +
                        "varying vec2 v_texCoord;\n" +
                        "varying vec4 v_color;\n" +
                        "void main() {\n" +
                        "    v_texCoord = a_texCoord0;\n" +
                        "    v_color = a_color;\n" +
                        "    gl_Position = u_projViewTrans * vec4(a_position, 1.0);\n" +
                        "}\n";

        String fragmentShader =
                "#ifdef GL_ES\n" +
                        "precision mediump float;\n" +
                        "#endif\n" +
                        "varying vec2 v_texCoord;\n" +
                        "varying vec4 v_color;\n" +
                        "uniform sampler2D u_texture;\n" +
                        "void main() {\n" +
                        "    vec4 texColor = texture2D(u_texture, v_texCoord);\n" +
                        "    gl_FragColor = texColor * v_color;\n" +
                        "    if (gl_FragColor.a < 0.01) discard;\n" +
                        "}\n";

        shader = new ShaderProgram(vertexShader, fragmentShader);

        if (!shader.isCompiled()) {
            Gdx.app.error("ParticleShader", "COMPILATION FAILED: " + shader.getLog());
            shader.dispose();
            shader = null;  
        } else {
            Gdx.app.log("ParticleShader", "Particle shader compiled successfully.");
        }
    }

    
    private int[] sortedIndices;
    private float[] sortDistances;

    private void sortByDistance() {
        if (sortedIndices == null || sortedIndices.length < maxParticles) {
            sortedIndices = new int[maxParticles];
            sortDistances = new float[maxParticles];
        }

        Vector3 camPos = camera.position;
        for (int i = 0; i < aliveCount; i++) {
            sortedIndices[i] = i;
            float dx = posX[i] - camPos.x;
            float dy = posY[i] - camPos.y;
            float dz = posZ[i] - camPos.z;
            sortDistances[i] = -(dx * dx + dy * dy + dz * dz); 
        }

        
        for (int i = 1; i < aliveCount; i++) {
            int key = sortedIndices[i];
            float keyDist = sortDistances[i];
            int j = i - 1;
            while (j >= 0 && sortDistances[j] > keyDist) {
                sortedIndices[j + 1] = sortedIndices[j];
                sortDistances[j + 1] = sortDistances[j];
                j--;
            }
            sortedIndices[j + 1] = key;
            sortDistances[j + 1] = keyDist;
        }
    }

    public void render() {
        if (aliveCount == 0 || camera == null) return;

        ensureGLInitialized();

        if (config.renderer.renderMode == ParticleSystem3DComponent.RenderMode.MESH) {
            renderMeshParticles();
            return;
        }

        if (shader == null || !shader.isCompiled()) {
            return;
        }

        
        if (config.renderer.sortMode == 1) {
            sortByDistance();
        }

        
        int totalFrames = 1;
        int tilesX = 1, tilesY = 1;
        boolean useTextureSheet = config.textureSheetAnimation.enabled;
        if (useTextureSheet) {
            tilesX = Math.max(1, config.textureSheetAnimation.tilesX);
            tilesY = Math.max(1, config.textureSheetAnimation.tilesY);
            totalFrames = tilesX * tilesY;
        }

        
        Vector3 camRight = tmpV3.set(camera.direction).crs(camera.up).nor();
        Vector3 camUp = tmpV4.set(camRight).crs(camera.direction).nor();

        int vertexOffset = 0;

        for (int s = 0; s < aliveCount; s++) {
            int idx = (config.renderer.sortMode == 1) ? sortedIndices[s] : s;

            float halfSize = size[idx] * 0.5f;
            float px = posX[idx], py = posY[idx], pz = posZ[idx];

            
            float rotRad = rotation[idx] * MathUtils.degreesToRadians;
            float cosR = MathUtils.cos(rotRad);
            float sinR = MathUtils.sin(rotRad);

            
            float rX, rY, rZ, uX, uY, uZ;

            RendererModule rm = config.renderer;
            switch (rm.renderMode) {
                case STRETCHED_BILLBOARD: {
                    
                    float vx = posX[idx] - prevPosX[idx];
                    float vy = posY[idx] - prevPosY[idx];
                    float vz = posZ[idx] - prevPosZ[idx];
                    float vLen = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
                    if (vLen < 0.0001f) { vx = 0; vy = 1; vz = 0; vLen = 1; }
                    vx /= vLen; vy /= vLen; vz /= vLen;

                    
                    float stretchLen = halfSize * rm.lengthScale + vLen * rm.speedScale;
                    
                    float crX = vy * camera.direction.z - vz * camera.direction.y;
                    float crY = vz * camera.direction.x - vx * camera.direction.z;
                    float crZ = vx * camera.direction.y - vy * camera.direction.x;
                    float crLen = (float) Math.sqrt(crX*crX + crY*crY + crZ*crZ);
                    if (crLen < 0.0001f) { crX = 1; crY = 0; crZ = 0; crLen = 1; }
                    crX /= crLen; crY /= crLen; crZ /= crLen;

                    rX = crX * halfSize; rY = crY * halfSize; rZ = crZ * halfSize;
                    uX = vx * stretchLen; uY = vy * stretchLen; uZ = vz * stretchLen;
                    break;
                }
                case HORIZONTAL_BILLBOARD: {
                    rX = halfSize; rY = 0; rZ = 0;
                    uX = 0; uY = 0; uZ = halfSize;
                    
                    float tempRX = rX * cosR - rZ * sinR;
                    float tempRZ = rX * sinR + rZ * cosR;
                    float tempUX = uX * cosR - uZ * sinR;
                    float tempUZ = uX * sinR + uZ * cosR;
                    rX = tempRX; rZ = tempRZ; uX = tempUX; uZ = tempUZ;
                    break;
                }
                case VERTICAL_BILLBOARD: {
                    
                    Vector3 toCamera = tmpV1.set(camera.position.x - px, 0, camera.position.z - pz).nor();
                    Vector3 right = tmpV2.set(toCamera).crs(Vector3.Y).nor();
                    rX = right.x * halfSize; rY = 0; rZ = right.z * halfSize;
                    uX = 0; uY = halfSize; uZ = 0;
                    break;
                }
                case BILLBOARD:
                default: {
                    
                    float rx = camRight.x * cosR + camUp.x * sinR;
                    float ry = camRight.y * cosR + camUp.y * sinR;
                    float rz = camRight.z * cosR + camUp.z * sinR;
                    float ux = -camRight.x * sinR + camUp.x * cosR;
                    float uy = -camRight.y * sinR + camUp.y * cosR;
                    float uz = -camRight.z * sinR + camUp.z * cosR;
                    rX = rx * halfSize; rY = ry * halfSize; rZ = rz * halfSize;
                    uX = ux * halfSize; uY = uy * halfSize; uZ = uz * halfSize;
                    break;
                }
            }

            
            float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
            if (useTextureSheet && totalFrames > 1) {
                float normalizedLife = 1f - (life[idx] / maxLife[idx]);
                float frameFloat = config.textureSheetAnimation.frameOverTime.evaluate(
                        normalizedLife, randomSeed[idx]) * totalFrames;
                int frame = MathUtils.clamp((int) frameFloat, 0, totalFrames - 1);
                int col = frame % tilesX;
                int row = frame / tilesX;
                float tileW = 1f / tilesX;
                float tileH = 1f / tilesY;
                u0 = col * tileW;
                v0 = row * tileH;
                u1 = u0 + tileW;
                v1 = v0 + tileH;
            }

            
            float packedColor = Color.toFloatBits(
                    colorR[idx], colorG[idx], colorB[idx], colorA[idx]);

            
            
            vertices[vertexOffset++] = px - rX - uX;
            vertices[vertexOffset++] = py - rY - uY;
            vertices[vertexOffset++] = pz - rZ - uZ;
            vertices[vertexOffset++] = u0; vertices[vertexOffset++] = v1;
            vertices[vertexOffset++] = packedColor;

            
            vertices[vertexOffset++] = px + rX - uX;
            vertices[vertexOffset++] = py + rY - uY;
            vertices[vertexOffset++] = pz + rZ - uZ;
            vertices[vertexOffset++] = u1; vertices[vertexOffset++] = v1;
            vertices[vertexOffset++] = packedColor;

            
            vertices[vertexOffset++] = px + rX + uX;
            vertices[vertexOffset++] = py + rY + uY;
            vertices[vertexOffset++] = pz + rZ + uZ;
            vertices[vertexOffset++] = u1; vertices[vertexOffset++] = v0;
            vertices[vertexOffset++] = packedColor;

            
            vertices[vertexOffset++] = px - rX + uX;
            vertices[vertexOffset++] = py - rY + uY;
            vertices[vertexOffset++] = pz - rZ + uZ;
            vertices[vertexOffset++] = u0; vertices[vertexOffset++] = v0;
            vertices[vertexOffset++] = packedColor;
        }

        if (aliveCount == 0) return;

        
        billboardMesh.setVertices(vertices, 0, aliveCount * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX);

        
        Gdx.gl.glEnable(GL20.GL_BLEND);
        if (config.renderer.isAdditive) {
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        } else {
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(!config.renderer.isAdditive); 

        shader.begin();
        shader.setUniformMatrix("u_projViewTrans", camera.combined);

        if (texture != null) {
            texture.bind(0);
            shader.setUniformi("u_texture", 0);
        }

        billboardMesh.render(shader, GL20.GL_TRIANGLES, 0, aliveCount * INDICES_PER_PARTICLE);
        shader.end();

        
        if (config.trails.enabled) {
            renderTrails();
        }

        
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderMeshParticles() {
        
        if (meshBatcherDirty || meshBatcher == null) {
            if (meshBatcher != null) {
                meshBatcher.dispose();
                meshBatcher = null;
            }
            initMeshBatcher();
            meshBatcherDirty = false;
        }

        meshBatcher.ensureGLReady();

        meshBatcher.batch(aliveCount,
                posX, posY, posZ,
                size,
                rotationX, rotationY, rotationZ,
                colorR, colorG, colorB, colorA,
                velX, velY, velZ,
                config.renderer.alignToVelocity);

        Vector3 lightDir = new Vector3(1, -1.5f, 1).nor();
        Color ambient = new Color(0.4f, 0.4f, 0.4f, 1f);
        if (engineRef != null) {
            lightDir = engineRef.getSunLightDirection().cpy().nor();
            ambient.set(0.3f, 0.3f, 0.3f, 1f);
        }

        meshBatcher.render(camera, lightDir, ambient, config.renderer.isAdditive);

        if (config.trails.enabled && shader != null && shader.isCompiled()) {
            renderTrails();
        }
    }

    

    private Mesh trailMesh;
    private float[] trailVertices;
    private short[] trailIndices;
    private static final int TRAIL_FLOATS_PER_VERT = 9; 
    private static final int MAX_TRAIL_VERTS = 8192;
    private static final int MAX_TRAIL_INDICES = MAX_TRAIL_VERTS * 3;

    private void ensureTrailMesh() {
        if (trailMesh != null) return;

        trailVertices = new float[MAX_TRAIL_VERTS * TRAIL_FLOATS_PER_VERT];
        trailIndices = new short[MAX_TRAIL_INDICES];
        trailMesh = new Mesh(false, MAX_TRAIL_VERTS, MAX_TRAIL_INDICES,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"));
    }

    private void renderTrails() {
        if (trailData == null || !config.trails.enabled) return;
        ensureTrailMesh();

        int vertIdx = 0;
        int idxIdx = 0;
        int vertCount = 0;

        Vector3 camRight = tmpV3;

        for (int p = 0; p < aliveCount; p++) {
            if (!hasTrail[p]) continue;
            List<TrailPoint> trail = trailData.get(p);
            if (trail.size() < 2) continue;

            float trailMaxLife = maxLife[p] * config.trails.lifetime;

            for (int t = 0; t < trail.size() - 1; t++) {
                if (vertCount + 4 > MAX_TRAIL_VERTS) break;

                TrailPoint a = trail.get(t);
                TrailPoint b = trail.get(t + 1);

                
                float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
                float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (len < 0.0001f) continue;
                dx /= len; dy /= len; dz /= len;

                
                float cx = dy * camera.direction.z - dz * camera.direction.y;
                float cy = dz * camera.direction.x - dx * camera.direction.z;
                float cz = dx * camera.direction.y - dy * camera.direction.x;
                float cLen = (float) Math.sqrt(cx*cx + cy*cy + cz*cz);
                if (cLen < 0.0001f) continue;
                cx /= cLen; cy /= cLen; cz /= cLen;

                float normA = MathUtils.clamp(a.age / trailMaxLife, 0, 1);
                float normB = MathUtils.clamp(b.age / trailMaxLife, 0, 1);

                float widthA = a.width * config.trails.widthOverTrail.evaluate(normA) * 0.5f;
                float widthB = b.width * config.trails.widthOverTrail.evaluate(normB) * 0.5f;

                float alphaA = 1f - normA;
                float alphaB = 1f - normB;

                float colorA = Color.toFloatBits(a.r, a.g, a.b, a.a * alphaA);
                float colorB = Color.toFloatBits(b.r, b.g, b.b, b.a * alphaB);

                float uA = (float) t / (trail.size() - 1);
                float uB = (float) (t + 1) / (trail.size() - 1);

                int baseVert = vertCount;

                
                
                trailVertices[vertIdx++] = a.x - cx * widthA;
                trailVertices[vertIdx++] = a.y - cy * widthA;
                trailVertices[vertIdx++] = a.z - cz * widthA;
                trailVertices[vertIdx++] = uA; trailVertices[vertIdx++] = 0;
                trailVertices[vertIdx++] = colorA;

                
                trailVertices[vertIdx++] = a.x + cx * widthA;
                trailVertices[vertIdx++] = a.y + cy * widthA;
                trailVertices[vertIdx++] = a.z + cz * widthA;
                trailVertices[vertIdx++] = uA; trailVertices[vertIdx++] = 1;
                trailVertices[vertIdx++] = colorA;

                
                trailVertices[vertIdx++] = b.x - cx * widthB;
                trailVertices[vertIdx++] = b.y - cy * widthB;
                trailVertices[vertIdx++] = b.z - cz * widthB;
                trailVertices[vertIdx++] = uB; trailVertices[vertIdx++] = 0;
                trailVertices[vertIdx++] = colorB;

                
                trailVertices[vertIdx++] = b.x + cx * widthB;
                trailVertices[vertIdx++] = b.y + cy * widthB;
                trailVertices[vertIdx++] = b.z + cz * widthB;
                trailVertices[vertIdx++] = uB; trailVertices[vertIdx++] = 1;
                trailVertices[vertIdx++] = colorB;

                
                trailIndices[idxIdx++] = (short) baseVert;
                trailIndices[idxIdx++] = (short) (baseVert + 1);
                trailIndices[idxIdx++] = (short) (baseVert + 2);
                trailIndices[idxIdx++] = (short) (baseVert + 1);
                trailIndices[idxIdx++] = (short) (baseVert + 3);
                trailIndices[idxIdx++] = (short) (baseVert + 2);

                vertCount += 4;
            }
        }

        if (vertCount == 0) return;

        trailMesh.setVertices(trailVertices, 0, vertIdx);
        trailMesh.setIndices(trailIndices, 0, idxIdx);

        
        shader.begin();
        shader.setUniformMatrix("u_projViewTrans", camera.combined);
        if (texture != null) {
            texture.bind(0);
            shader.setUniformi("u_texture", 0);
        }
        trailMesh.render(shader, GL20.GL_TRIANGLES, 0, idxIdx);
        shader.end();
    }

    

    public void reconfigure(ParticleSystem3DComponent newConfig) {
        boolean needsRealloc = (newConfig.maxParticles != this.maxParticles);
        boolean meshModeChanged = (this.config.renderer.renderMode != newConfig.renderer.renderMode);
        boolean meshTypeChanged = (this.config.renderer.meshType != newConfig.renderer.meshType);

        String oldPath = this.config.renderer.meshPath;
        String newPath = newConfig.renderer.meshPath;
        boolean meshPathChanged = (oldPath == null) != (newPath == null)
                || (oldPath != null && !oldPath.equals(newPath));

        this.config = newConfig;

        if (needsRealloc) {
            this.maxParticles = newConfig.maxParticles;
            aliveCount = Math.min(aliveCount, maxParticles);
            allocateArrays(maxParticles);

            if (glInitialized) {
                if (billboardMesh != null) billboardMesh.dispose();
                buildMesh(maxParticles);
            }
            
            meshBatcherDirty = true;
        }

        if (meshModeChanged || meshTypeChanged || meshPathChanged) {
            meshBatcherDirty = true;
        }

        this.isPlaying = newConfig.isPlaying;
        this.isAdditive = newConfig.renderer.isAdditive;
    }

    

    @Override
    public void dispose() {
        if (billboardMesh != null) billboardMesh.dispose();
        if (trailMesh != null) trailMesh.dispose();
        if (shader != null) shader.dispose();
        if (meshBatcher != null) { meshBatcher.dispose(); meshBatcher = null; }
        billboardMesh = null;
        trailMesh = null;
        shader = null;
        glInitialized = false;
    }
}