package org.catrobat.catroid.raptor;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btGeneric6DofSpringConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btPoint2PointConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.danvexteam.lunoscript_annotations.LunoClass;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.utils.ModelPathProcessor;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@LunoClass
public class ThreeDManager implements Disposable {

    public ModelBatch getWireframeBatch() {
        return wireframeBatch;
    }

    public enum PhysicsState {
        NONE,
        STATIC,
        DYNAMIC,
        MESH_STATIC
    }

    public enum PhysicsShape {
        BOX,
        SPHERE,
        CAPSULE
    }

    private static class AudioAsset {
        public Sound sound = null;
        public Music music = null;
        public final String filePath;

        public AudioAsset(FileHandle fileHandle, boolean asMusic) {
            this.filePath = fileHandle.path();
            if (asMusic) {
                music = Gdx.audio.newMusic(fileHandle);
            } else {
                sound = Gdx.audio.newSound(fileHandle);
            }
        }
        public void dispose() {
            if (sound != null) sound.dispose();
            if (music != null) music.dispose();
        }
    }

    private final Map<String, AudioAsset> loadedAudioAssets = new HashMap<>();
    private final List<PlayableAudio> active3DSounds = new ArrayList<>();

    private float globalSoundVolume = 1.0f;
    private static final float MAX_HEARING_DISTANCE = 250f;

    private final Vector3 cameraRight = new Vector3();
    private final Vector3 soundToListener = new Vector3();

    private final boolean debugEnabled = false;
    private static final boolean LOG_THREED_MANAGER_DEBUG = false;

    private static class RayCastResult {
        public boolean hasHit = false;
        public String hitObjectId = "";
        public float hitDistance = -1.0f;

        public final Vector3 hitPoint = new Vector3();
        public final Vector3 hitNormal = new Vector3();
    }

    private com.badlogic.gdx.graphics.Color skyColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0);

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private boolean editorMode = false;

    private Map<String, Model> loadedModels = new HashMap<>();

    private Map<String, Texture> loadedTextures = new HashMap<>();

    private Map<String, ModelInstance> sceneObjects = new HashMap<>();

    private Map<String, DirectionalLight> directionalLights = new HashMap<>();

    private final BoundingBox bounds1 = new BoundingBox();
    private final BoundingBox bounds2 = new BoundingBox();

    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btBroadphaseInterface broadphase;
    private btConstraintSolver solver;
    private btDiscreteDynamicsWorld dynamicsWorld;
    private com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder;
    private CollisionCallback collisionCallback;
    private Map<String, btRigidBody> physicsBodies = new HashMap<>();
    private Map<String, RayCastResult> rayCastResults = new HashMap<>();
    private ShaderProvider defaultShaderProvider;
    private ShaderProvider customShaderProvider;

    private final Map<String, Object> customUniforms = new HashMap<>();
    private float time = 0f;

    private ModelBatch shadowBatch;
    private com.badlogic.gdx.graphics.glutils.FrameBuffer shadowFBO;
    private PerspectiveCamera lightCamera;
    private com.badlogic.gdx.math.Matrix4 lightSpaceMatrix = new com.badlogic.gdx.math.Matrix4();
    private final int SHADOW_MAP_SIZE = 2048;
    private ShaderProvider depthShaderProvider;

    private boolean realisticMode = false;
    private net.mgsx.gltf.scene3d.scene.SceneManager sceneManager;
    private com.badlogic.gdx.graphics.Texture brdfLUT;
    private com.badlogic.gdx.graphics.Cubemap diffuseCubemap;
    private com.badlogic.gdx.graphics.Cubemap specularCubemap;
    private net.mgsx.gltf.scene3d.lights.DirectionalLightEx pbrLight;

    private DebugDrawer debugDrawer;

    private final Map<String, net.mgsx.gltf.scene3d.lights.PointLightEx> pointLights = new HashMap<>();
    private final Map<String, net.mgsx.gltf.scene3d.lights.SpotLightEx> spotLights = new HashMap<>();
    private final java.util.Set<String> gltfObjectIds = new java.util.HashSet<>();
    private final Map<String, AnimationController> animationControllers = new HashMap<>();
    private net.mgsx.gltf.scene3d.scene.SceneSkybox skybox;
    private Cubemap skyboxCubemap;
    private PanoramicConverter panoramicConverter;
    private ModelInstance gridInstance;
    private final Map<String, ModelInstance> editorProxies = new HashMap<>();
    private Model lightProxyModel;
    private ModelBatch wireframeBatch;
    private Model wireframeBoxModel;
    private Model wireframeSphereModel;
    private Model wireframeCylinderModel;
    private Model cameraProxyModel;
    public String cameraTargetId = null;
    private final Vector3 cameraOffset = new Vector3();
    private float cameraDistance = 10.0f;
    private float cameraPitch = 20.0f;
    private float cameraYaw = 0.0f;

    private Map<String, btTypedConstraint> physicsConstraints = new HashMap<>();

    private final Set<String> inactiveRenderObjects = new HashSet<>();
    private final Map<String, btRigidBody> inactivePhysicsBodies = new HashMap<>();
    private final Map<String, net.mgsx.gltf.scene3d.scene.Scene> inactivePbrScenes = new HashMap<>();

    private SceneSettings currentSettings;

    private final Vector3 tmpPos = new Vector3();
    private final Quaternion tmpRot = new Quaternion();
    private final Vector3 tmpScale = new Vector3();
    private final Vector3 tmpPos2 = new Vector3();

    public static class SceneSettings {
        public int numPointLights = 5;
        public int numSpotLights = 2;
        public int numDirectionalLights = 1;
        public int numBones = 110;
    }

    private SceneManager manager;

    public void init() {
        init(new SceneSettings());
    }

    public void init(SceneSettings settings) {
        this.currentSettings = settings;
        modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        defaultShaderProvider = new DefaultShaderProvider();
        modelBatch = new ModelBatch(defaultShaderProvider);
        environment = new Environment();
        setAmbientLight(0.4f, 0.4f, 0.4f);

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setCameraPosition(100f, 100f, 100f);
        cameraLookAt(0, 0, 0);

        camera.near = 0.1f;
        camera.far = 2500f;

        shadowFBO = new com.badlogic.gdx.graphics.glutils.FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, true);

        lightCamera = new PerspectiveCamera(90, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        lightCamera.near = 1f;
        lightCamera.far = 500f;

        String depthVertexShader = "attribute vec3 a_position; uniform mat4 u_projViewTrans; void main() { gl_Position = u_projViewTrans * vec4(a_position, 1.0); }";
        String depthFragmentShader = "#ifdef GL_ES\n" +
                "precision highp float;\n" +
                "#endif\n" +
                "void main() {\n" +
                "    float depth = gl_FragCoord.z;\n" +
                "    gl_FragColor = vec4(depth, 0.0, 0.0, 1.0);\n" +
                "}\n";
        depthShaderProvider = new DefaultShaderProvider(depthVertexShader, depthFragmentShader);
        shadowBatch = new ModelBatch(depthShaderProvider);

        Bullet.init();
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -9.81f, 0));

        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        dynamicsWorld.setDebugDrawer(debugDrawer);
        collisionCallback = new CollisionCallback();

        PBRShaderProvider prov = PBRShaderProvider.createDefault(settings.numBones);
        DefaultShader.Config config = prov.config;
        config.numPointLights = settings.numPointLights;
        config.numSpotLights = settings.numSpotLights;
        config.numDirectionalLights = settings.numDirectionalLights;
        config.numBones = settings.numBones;

        sceneManager = new net.mgsx.gltf.scene3d.scene.SceneManager(prov, PBRShaderProvider.createDefaultDepth(config.numBones));
        sceneManager.setCamera(camera);
        net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight = new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(2048, 2048);
        shadowLight.direction.set(1, -1.5f, 1).nor();
        shadowLight.color.set(com.badlogic.gdx.graphics.Color.WHITE);
        shadowLight.intensity = 5.0f;

        shadowLight.getCamera().far = 1000f;

        sceneManager.environment.add(shadowLight);

        pbrLight = shadowLight;

        net.mgsx.gltf.scene3d.utils.IBLBuilder iblBuilder = net.mgsx.gltf.scene3d.utils.IBLBuilder.createOutdoor(pbrLight);

        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        brdfLUT = new com.badlogic.gdx.graphics.Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute(net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute.SpecularEnv, specularCubemap));

        dynamicsWorld.getSolverInfo().setNumIterations(20); 

        panoramicConverter = new PanoramicConverter();

        lightProxyModel = modelBuilder.createSphere(0.25f, 0.25f, 0.25f, 8, 8, 
                new com.badlogic.gdx.graphics.g3d.Material(ColorAttribute.createDiffuse(com.badlogic.gdx.graphics.Color.YELLOW)),
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal);

        Material camMat = new Material(ColorAttribute.createDiffuse(Color.CYAN));
        Model box = modelBuilder.createBox(0.4f, 0.4f, 0.4f, camMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        Model cone = modelBuilder.createCone(0.4f, 0.5f, 0.4f, 4, camMat, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        mb.part("box", box.meshes.get(0), GL20.GL_TRIANGLES, camMat);

        Node coneNode = mb.node(); 
        coneNode.id = "cone";
        
        coneNode.rotation.set(Vector3.X, 90);
        coneNode.translation.set(0, 0, -0.2f); 

        mb.part("cone", cone.meshes.get(0), GL20.GL_TRIANGLES, camMat);

        cameraProxyModel = mb.end();

        wireframeBatch = new ModelBatch();
        Material wireframeMaterial = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        long usage = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        wireframeBoxModel = modelBuilder.createBox(1f, 1f, 1f, GL20.GL_LINES, wireframeMaterial, usage);
        wireframeSphereModel = modelBuilder.createSphere(1f, 1f, 1f, 16, 12, GL20.GL_LINES, wireframeMaterial, usage);
        wireframeCylinderModel = modelBuilder.createCylinder(1f, 2f, 1f, 16, GL20.GL_LINES, wireframeMaterial, usage);
    }

    public void setObjectVisibility(String objectId, boolean visible) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        if (visible) {
            btRigidBody body = inactivePhysicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.addRigidBody(body);
                physicsBodies.put(objectId, body);
            }
        } else {
            btRigidBody body = physicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.removeRigidBody(body);
                inactivePhysicsBodies.put(objectId, body);
            }
        }

        if (visible) {
            inactiveRenderObjects.remove(objectId);
            if (realisticMode && inactivePbrScenes.containsKey(objectId)) {
                sceneManager.addScene(inactivePbrScenes.remove(objectId));
            }
        } else {
            inactiveRenderObjects.add(objectId);
            if (realisticMode) {
                net.mgsx.gltf.scene3d.scene.Scene sceneToRemove = null;
                for (RenderableProvider provider : sceneManager.getRenderableProviders()) {
                    if (provider instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                        net.mgsx.gltf.scene3d.scene.Scene scene = (net.mgsx.gltf.scene3d.scene.Scene) provider;
                        if (scene.modelInstance == instance) {
                            sceneToRemove = scene;
                            break;
                        }
                    }
                }
                if (sceneToRemove != null) {
                    sceneManager.removeScene(sceneToRemove);
                    inactivePbrScenes.put(objectId, sceneToRemove);
                }
            }
        }
    }

    public void setSceneManager(SceneManager manager) {
        this.manager = manager;
    }


    @Deprecated
    public void setObjectRenderState(String objectId, boolean active) {
        setObjectVisibility(objectId, active);
    }

    public void setObjectPhysicsState(String objectId, boolean active) {
        if (active) {
            btRigidBody body = inactivePhysicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.addRigidBody(body);
                physicsBodies.put(objectId, body);
            }
        } else {
            btRigidBody body = physicsBodies.remove(objectId);
            if (body != null) {
                dynamicsWorld.removeRigidBody(body);
                inactivePhysicsBodies.put(objectId, body);
            }
        }
    }

    public SceneSettings getSceneSettings() {
        return currentSettings != null ? currentSettings : new SceneSettings();
    }

    public void setCameraFov(float fieldOfView, float near, float far) {
        camera.fieldOfView = fieldOfView;
        camera.near = near;
        camera.far = far;
        camera.update();
    }

    public DebugDrawer getDebugDrawer() { return debugDrawer; }

    public void createCameraProxy(String ownerId) {
        if (editorProxies.containsKey(ownerId) || cameraProxyModel == null) return;
        ModelInstance proxyInstance = new ModelInstance(cameraProxyModel);
        editorProxies.put(ownerId, proxyInstance);
    }

    public boolean createPoint2PointConstraint(String constraintId, String objectIdA, String objectIdB, Vector3 pivotInA, Vector3 pivotInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create constraint: one or both bodies not found.");
            return false;
        }

        btPoint2PointConstraint constraint = new btPoint2PointConstraint(bodyA, bodyB, pivotInA, pivotInB);
        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }

    public boolean createFixedConstraint(String constraintId, String objectIdA, String objectIdB, Matrix4 transformA, Matrix4 transformB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create fixed constraint: one or both bodies not found.");
            return false;
        }

        btFixedConstraint constraint = new btFixedConstraint(bodyA, bodyB, transformA, transformB);
        dynamicsWorld.addConstraint(constraint, true);
        physicsConstraints.put(constraintId, constraint);
        return true;
    }


    public void removeConstraint(String constraintId) {
        btTypedConstraint constraint = physicsConstraints.remove(constraintId);
        if (constraint != null) {
            dynamicsWorld.removeConstraint(constraint);
            constraint.dispose();
        }
    }


    private final Vector3 cyanColorVec = new Vector3(Color.CYAN.r, Color.CYAN.g, Color.CYAN.b);

    
    /**
     * Рисует линии, представляющие поле зрения (frustum) камеры.
     * @param ownerTransform Трансформация GameObject'а, к которому прикреплена камера.
     * @param fov Угол обзора.
     * @param far Дальняя плоскость.
     */
    public void renderCameraFrustum(Matrix4 ownerTransform, float fov, float far) {
        if (debugDrawer == null) return;

        PerspectiveCamera tempCam = new PerspectiveCamera(fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        tempCam.far = far;
        Quaternion rot = ownerTransform.getRotation(new Quaternion());
        tempCam.position.set(ownerTransform.getTranslation(new Vector3()));
        tempCam.direction.set(0, 0, -1);
        rot.transform(tempCam.direction);
        tempCam.up.set(0, 1, 0);
        rot.transform(tempCam.up);

        tempCam.update();

        Vector3[] points = tempCam.frustum.planePoints;
        
        debugDrawer.drawLine(points[4], points[5], cyanColorVec);
        debugDrawer.drawLine(points[5], points[6], cyanColorVec);
        debugDrawer.drawLine(points[6], points[7], cyanColorVec);
        debugDrawer.drawLine(points[7], points[4], cyanColorVec);
        
        debugDrawer.drawLine(points[0], points[1], cyanColorVec);
        debugDrawer.drawLine(points[1], points[2], cyanColorVec);
        debugDrawer.drawLine(points[2], points[3], cyanColorVec);
        debugDrawer.drawLine(points[3], points[0], cyanColorVec);
        
        debugDrawer.drawLine(points[0], points[4], cyanColorVec);
        debugDrawer.drawLine(points[1], points[5], cyanColorVec);
        debugDrawer.drawLine(points[2], points[6], cyanColorVec);
        debugDrawer.drawLine(points[3], points[7], cyanColorVec);
    }

    
    public void setCameraRotation(Quaternion rotation) {
        camera.direction.set(0, 0, -1);
        rotation.transform(camera.direction);
        camera.up.set(0, 1, 0);
        rotation.transform(camera.up);
        camera.update();
    }

    public void setEditorMode(boolean enabled) {
        this.editorMode = enabled;
    }

    public void setPhysicsStateFromComponent(String objectId, PhysicsComponent component) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        removePhysicsBody(objectId);

        float bodyMass = (component.state == PhysicsState.DYNAMIC) ? component.mass : 0f;

        if (component.state == PhysicsState.NONE) return;

        if (component.state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
            return;
        }
        
        if (component.colliders.isEmpty()) {
            Gdx.app.error("3DManager", "setPhysicsStateFromComponent called with no colliders for object: " + objectId);
            return;
        }

        createCompoundPhysicsBody(objectId, instance, component, bodyMass);
    }

    
    private void createCompoundPhysicsBody(String objectId, ModelInstance instance, PhysicsComponent component, float mass) {
        Log.d("PhysicsDebug", "--- Creating Compound Physics Body for '" + objectId + "' ---");
        Log.d("PhysicsDebug", "  - Input Mass: " + mass);
        Log.d("PhysicsDebug", "  - Instance World Transform on entry:\n" + instance.transform);

        btCompoundShape compoundShape = new btCompoundShape();
        Array<Disposable> disposables = new Array<>();
        disposables.add(compoundShape);

        final Matrix4 worldTransform = instance.transform;
        final float[] vals = worldTransform.val;

        boolean isFlippedX = vals[Matrix4.M00] < 0;
        boolean isFlippedY = vals[Matrix4.M11] < 0;
        boolean isFlippedZ = vals[Matrix4.M22] < 0;
        Log.d("PhysicsDebug", "  - Axis flip detection: X=" + isFlippedX + ", Y=" + isFlippedY + ", Z=" + isFlippedZ);

        for (ColliderShapeData shapeData : component.colliders) {
            Vector3 correctedOffset = new Vector3(shapeData.centerOffset);
            if (isFlippedX) correctedOffset.x *= -1;
            if (isFlippedY) correctedOffset.y *= -1;
            if (isFlippedZ) correctedOffset.z *= -1;

            Log.d("PhysicsDebug", "  - Processing Collider #(тут был номер, но я его убрал)" + ": Type=" + shapeData.type + ", Local Offset=" + shapeData.centerOffset + ", Size=" + shapeData.size);
            Log.d("PhysicsDebug", "    - Original Offset: " + shapeData.centerOffset);
            Log.d("PhysicsDebug", "    - Corrected Offset for Physics: " + correctedOffset);

            btCollisionShape shape;

            switch (shapeData.type) {
                case SPHERE:
                    shape = new btSphereShape(shapeData.radius);
                    break;
                case CAPSULE:
                    float height = Math.max(0, shapeData.size.y - (shapeData.radius * 2));
                    shape = new btCapsuleShape(shapeData.radius, height);
                    break;
                case BOX:
                default:
                    shape = new btBoxShape(shapeData.size.cpy().scl(0.5f));
                    break;
            }
            disposables.add(shape);
            Matrix4 localTransform = new Matrix4().setToTranslation(correctedOffset);
            compoundShape.addChildShape(localTransform, shape);
            Log.d("PhysicsDebug", "    - Added child shape with local transform:\n" + localTransform);
        }

        Vector3 absScale = worldTransform.getScale(new Vector3());
        if (absScale.x == 0) absScale.x = 0.0001f;
        if (absScale.y == 0) absScale.y = 0.0001f;
        if (absScale.z == 0) absScale.z = 0.0001f;

        compoundShape.setLocalScaling(absScale);
        Log.d("PhysicsDebug", "  - Compound Shape Local Scaling set to (abs value): " + absScale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        Matrix4 bodyTransform = new Matrix4(worldTransform);
        bodyTransform.scale(1f / absScale.x, 1f / absScale.y, 1f / absScale.z);

        Log.d("PhysicsDebug", "  - Final RigidBody Transform for creation:\n" + bodyTransform);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        if (mass > 0) {
            body.setAngularFactor(1f);
            body.setDamping(0.5f, 0.5f);
        }

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        body.userData = disposables;
        physicsResources.put(objectId, disposables);
        bodyInfo.dispose();

        Matrix4 finalBodyTransform = body.getWorldTransform();
        Log.d("PhysicsDebug", "  - SUCCESS: Body created. Final transform from physics world:\n" + finalBodyTransform);
        Log.d("PhysicsDebug", "--------------------------------------------------------");
    }

    public void setPhysicsStateFromComponent(String objectId, PhysicsComponent component, Matrix4 worldTransform) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        instance.transform.set(worldTransform);

        removePhysicsBody(objectId);

        float bodyMass = (component.state == PhysicsState.DYNAMIC) ? component.mass : 0f;

        if (component.state == PhysicsState.NONE) return;

        if (component.state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
            return;
        }

        if (component.colliders.isEmpty()) {
            Gdx.app.error("3DManager", "setPhysicsStateFromComponent called with no colliders for object: " + objectId);
            return;
        }

        createCompoundPhysicsBody(objectId, instance, component, bodyMass);
    }

    private final Vector3 tmpVec = new Vector3();

    /**
     * Отрисовывает контур хитбокса для редактора.
     * НЕ ИСПОЛЬЗУЕТ glPolygonMode, работает на Android.
     * @param shapeData Данные о форме коллайдера.
     * @param parentTransform Трансформация родительского GameObject.
     */
    public void renderWireframeShape(ModelBatch batch, ColliderShapeData shapeData, Matrix4 parentTransform, Color color) {
        if (batch == null) return;

        ModelInstance instance;
        Matrix4 finalTransform = new Matrix4(parentTransform);

        switch (shapeData.type) {
            case BOX:
                instance = new ModelInstance(wireframeBoxModel);
                instance.transform.scl(shapeData.size);
                instance.transform.mulLeft(finalTransform.translate(shapeData.centerOffset));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);
                break;

            case SPHERE:
                instance = new ModelInstance(wireframeSphereModel);
                instance.transform.scl(shapeData.radius * 2);
                instance.transform.mulLeft(finalTransform.translate(shapeData.centerOffset));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);
                break;

            case CAPSULE:
                float cylinderHeight = Math.max(0, shapeData.size.y - (shapeData.radius * 2));
                Vector3 capsuleCenter = shapeData.centerOffset;
                instance = new ModelInstance(wireframeCylinderModel);
                instance.transform.scl(shapeData.radius * 2, cylinderHeight, shapeData.radius * 2);
                instance.transform.mulLeft(finalTransform.cpy().translate(capsuleCenter));
                ((ColorAttribute)instance.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(instance);

                ModelInstance topSphere = new ModelInstance(wireframeSphereModel);
                tmpVec.set(capsuleCenter).add(0, cylinderHeight / 2f, 0); 
                topSphere.transform.scl(shapeData.radius * 2);
                topSphere.transform.mulLeft(finalTransform.cpy().translate(tmpVec));
                ((ColorAttribute)topSphere.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(topSphere);

                ModelInstance bottomSphere = new ModelInstance(wireframeSphereModel);
                tmpVec.set(capsuleCenter).add(0, -cylinderHeight / 2f, 0); 
                bottomSphere.transform.scl(shapeData.radius * 2);
                bottomSphere.transform.mulLeft(finalTransform.cpy().translate(tmpVec));
                ((ColorAttribute)bottomSphere.materials.get(0).get(ColorAttribute.Diffuse)).color.set(color);
                batch.render(bottomSphere);
                break;
        }
    }


    /**
     * Устанавливает интенсивность фонового света (IBL).
     * Этот метод управляет атрибутом AmbientLight, который PBR-шейдер использует
     * как множитель для яркости карты окружения (diffuseCubemap).
     * @param intensity Множитель. 1.0 - нормальная яркость, 0.1 - очень темно (ночь), 0.0 - полностью выключен.
     */
    public void setBackgroundLightIntensity(float intensity) {
        if (sceneManager != null) {
            ColorAttribute ambientLight = (ColorAttribute) sceneManager.environment.get(ColorAttribute.AmbientLight);
            if (ambientLight == null) {
                ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, intensity, intensity, intensity, 1.0f);
                sceneManager.environment.set(ambientLight);
            } else {
                ambientLight.color.set(intensity, intensity, intensity, 1.0f);
            }
        }
    }

    /**
     * Добавляет или обновляет точечный источник света в PBR-сцене.
     * Если свет с таким ID уже существует, его параметры будут обновлены.
     * @param lightId Уникальный ID света.
     * @param x, y, z Позиция.
     * @param r, g, b Цвет (0-1).
     * @param intensity Интенсивность. Для лампочки накаливания ~500-1000, для уличного фонаря ~5000+.
     * @param range Дальность действия света. 0 - бесконечная дальность.
     */
    public void setPointLight(String lightId, float x, float y, float z, float r, float g, float b, float intensity, float range) {
        net.mgsx.gltf.scene3d.lights.PointLightEx light = pointLights.get(lightId);
        if (light == null) {
            light = new net.mgsx.gltf.scene3d.lights.PointLightEx();
            pointLights.put(lightId, light);
            sceneManager.environment.add(light);
        }
        light.position.set(x, y, z);
        light.color.set(r, g, b, 1);
        light.intensity = intensity;
        light.range = range > 0 ? range : null;
    }

    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    /**
     * Добавляет или обновляет прожектор (конус света) в PBR-сцене.
     * @param lightId ID света.
     * @param x, y, z Позиция.
     * @param dirX, dirY, dirZ Направление.
     * @param r, g, b Цвет.
     * @param intensity Интенсивность.
     * @param cutoffAngle Угол конуса в градусах (1-90).
     * @param exponent Плавность затухания по краям конуса (обычно 1.0).
     * @param range Дальность.
     */
    public void setSpotLight(String lightId, float x, float y, float z, float dirX, float dirY, float dirZ, float r, float g, float b, float intensity, float cutoffAngle, float exponent, float range) {
        net.mgsx.gltf.scene3d.lights.SpotLightEx light = spotLights.get(lightId);
        if (light == null) {
            light = new net.mgsx.gltf.scene3d.lights.SpotLightEx();
            spotLights.put(lightId, light);
            sceneManager.environment.add(light);
        }
        light.position.set(x, y, z);
        light.direction.set(dirX, dirY, dirZ).nor();
        light.color.set(r, g, b, 1);
        light.intensity = intensity;
        light.cutoffAngle = map(cutoffAngle, 0, 360, -1000, 1000);
        light.exponent = exponent * 1000;
        light.range = range > 0 ? range : null;
    }

    /**
     * Удаляет источник света (точечный или прожектор) из сцены.
     * @param lightId ID удаляемого света.
     * @return true, если свет был найден и удален.
     */
    public boolean removePBRLight(String lightId) {
        net.mgsx.gltf.scene3d.lights.PointLightEx pointLight = pointLights.remove(lightId);
        if (pointLight != null) {
            sceneManager.environment.remove(pointLight);
            return true;
        }
        net.mgsx.gltf.scene3d.lights.SpotLightEx spotLight = spotLights.remove(lightId);
        if (spotLight != null) {
            sceneManager.environment.remove(spotLight);
            return true;
        }
        return false;
    }

    /**
     * Устанавливает уровень анизотропной фильтрации для всех материалов объекта.
     * @param objectId ID объекта.
     * @param level Уровень фильтрации. Хорошие значения: 2.0, 4.0, 8.0. Максимум зависит от GPU. 1.0 - выключено.
     */
    public void setObjectAnisotropicFilter(String objectId, float level) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            for (com.badlogic.gdx.graphics.g3d.Attribute attr : material) {
                if (attr instanceof com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) {
                    Texture texture = ((com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) attr).textureDescription.texture;
                    if (texture != null) {
                        texture.setAnisotropicFilter(level);
                    }
                }
            }
        }
    }

    /**
     * Устанавливает экспоненциальный туман в сцене.
     * @param r, g, b Цвет тумана (0-1).
     * @param density Плотность тумана. Хорошие значения от 0.001 (очень легкий) до 0.1 (плотный).
     *                Установите в 0 или меньше, чтобы выключить туман.
     */
    public void setFog(float r, float g, float b, float density) {
        if (density > 0) {
            environment.set(new ColorAttribute(ColorAttribute.Fog, r, g, b, 1f));
            camera.far = 1f / density;
        } else {
            environment.remove(ColorAttribute.Fog);
            camera.far = 1000f;
        }
        camera.update();
    }

    /**
     * Устанавливает цвет фона (неба).
     */
    public void setSkyColor(float r, float g, float b) {
        skyColor.set(r, g, b, 1f);
    }

    /**
     * Возвращает текущую позицию камеры.
     * @return Vector3 с координатами (x, y, z).
     */
    public Vector3 getCameraPosition() {
        return camera.position;
    }

    public void renameObject(String oldId, String newId) {
        
        if (sceneObjects.containsKey(oldId)) {
            sceneObjects.put(newId, sceneObjects.remove(oldId));
        }
        
        if (physicsBodies.containsKey(oldId)) {
            physicsBodies.put(newId, physicsBodies.remove(oldId));
        }
        
        if (physicsResources.containsKey(oldId)) {
            physicsResources.put(newId, physicsResources.remove(oldId));
        }
        
        if (animationControllers.containsKey(oldId)) {
            animationControllers.put(newId, animationControllers.remove(oldId));
        }
        
        if (gltfObjectIds.contains(oldId)) {
            gltfObjectIds.remove(oldId);
            gltfObjectIds.add(newId);
        }
        
        if (editorProxies.containsKey(oldId)) {
            editorProxies.put(newId, editorProxies.remove(oldId));
        }
        
        if (pointLights.containsKey(oldId)) {
            pointLights.put(newId, pointLights.remove(oldId));
        }
        if (spotLights.containsKey(oldId)) {
            spotLights.put(newId, spotLights.remove(oldId));
        }
    }

    /**
     * Возвращает текущий вектор направления камеры.
     * ВАЖНО: Этот вектор уже нормализован (его длина равна 1).
     * @return Vector3 с направлением (x, y, z).
     */
    public Vector3 getCameraDirection() {
        return camera.direction;
    }

    public Matrix4 getWorldTransform(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            return instance.transform;
        }
        return null;
    }

    public void setPhysicsState(String objectId, PhysicsState state, PhysicsShape shape, float mass, Matrix4 worldTransform) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;


        instance.transform.set(worldTransform);


        setPhysicsState(objectId, state, shape, mass);
    }

    private int frameCounter = 0;

    public void update(float delta) {
        if (LOG_THREED_MANAGER_DEBUG) Log.d("TDM_DEBUG", "--- ThreeDManager.update() START (Delta: " + delta + ") ---");
        if (cameraTargetId != null) {
            updateThirdPersonCamera();
        }
        /*frameCounter++;
        if (frameCounter % 60 == 0) { 
            Gdx.app.log("PhysicsDebug", "--- Frame " + frameCounter + " ---");
            btRigidBody playerBody = physicsBodies.get("player"); 
            if (playerBody != null) {
                Matrix4 transform = playerBody.getWorldTransform();
                Vector3 position = new Vector3();
                transform.getTranslation(position);
                Gdx.app.log("PhysicsDebug", "Player position: " + position);
            } else {
                Gdx.app.log("PhysicsDebug", "Player physics body not found!");
            }

            btRigidBody mapBody = physicsBodies.get("myObject2"); 
            if (mapBody != null) {
                Matrix4 transform = mapBody.getWorldTransform();
                Vector3 position = new Vector3();
                transform.getTranslation(position);
                Gdx.app.log("PhysicsDebug", "Map position: " + position);
            } else {
                Gdx.app.log("PhysicsDebug", "Map physics body not found!");
            }
        }*/

        for (com.badlogic.gdx.graphics.g3d.utils.AnimationController controller : animationControllers.values()) {
            controller.update(delta);
        }

        if (realisticMode) {
            sceneManager.update(delta);
        }

        time += delta;

        if (customShaderProvider != null) {
            customUniforms.put("u_time", time);
            customUniforms.put("u_resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            customUniforms.put("u_cameraPosition", camera.position);
            customUniforms.put("u_cameraDirection", camera.direction);
            customUniforms.put("u_viewMatrix", camera.view);
            customUniforms.put("u_projectionMatrix", camera.projection);
        }

        if (!editorMode) {
            dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                String objectId = entry.getKey();
                btRigidBody body = entry.getValue();



                boolean isManagedBySceneManager = (Objects.requireNonNull(StageActivity.getActiveStageListener()).sceneManager != null && Objects.requireNonNull(StageActivity.getActiveStageListener()).sceneManager.findGameObject(objectId) != null);


                if (!isManagedBySceneManager) {
                    ModelInstance instance = sceneObjects.get(objectId);
                    if (instance != null && !body.isStaticObject() && body.isActive()) {

                        Matrix4 bodyTransform = body.getWorldTransform();

                        bodyTransform.getTranslation(tmpPos);
                        bodyTransform.getRotation(tmpRot, true);

                        instance.transform.getScale(tmpScale);

                        instance.transform.set(tmpPos, tmpRot, tmpScale);
                    }
                }
            }

            /*for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                ModelInstance instance = sceneObjects.get(entry.getKey());
                btRigidBody body = entry.getValue();

                if (instance != null && !body.isStaticObject() && body.getMotionState() != null) {
                    com.badlogic.gdx.math.Matrix4 bodyTransform = body.getWorldTransform();

                    Vector3 position = new Vector3();
                    bodyTransform.getTranslation(position);
                    Quaternion rotation = new Quaternion();
                    bodyTransform.getRotation(rotation);

                    Vector3 scale = new Vector3();
                    instance.transform.getScale(scale); 

                    instance.transform.set(position, rotation, scale);
                }
            }*/
        }

        if (!editorMode && LOG_THREED_MANAGER_DEBUG) {
            Log.d("TDM_DEBUG", "    [Physics] After stepSimulation:");
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                Vector3 physBodyPos = new Vector3();
                entry.getValue().getWorldTransform().getTranslation(physBodyPos);
                Log.d("TDM_DEBUG", "      - '" + entry.getKey() + "' Physics Pos: " + physBodyPos);
            }
        }
        if (!editorMode) {
            update3DAudio();
        }
        if (LOG_THREED_MANAGER_DEBUG) Log.d("TDM_DEBUG", "--- ThreeDManager.update() END ---");
    }

    private void update3DAudio() {
        if (active3DSounds.isEmpty()) {
            return;
        }

        Vector3 listenerPos = camera.position;
        cameraRight.set(camera.direction).crs(camera.up).nor();

        Iterator<PlayableAudio> iterator = active3DSounds.iterator();
        while (iterator.hasNext()) {
            PlayableAudio audio = iterator.next();

            if (!audio.isPlaying()) {
                audio.dispose();
                iterator.remove();
                continue;
            }

            String attachedId = audio.getAttachedObjectId();
            if (attachedId != null) {
                ModelInstance attachedObject = sceneObjects.get(attachedId);
                if (attachedObject != null) {
                    attachedObject.transform.getTranslation(tmpPos);
                    audio.setPosition(tmpPos);
                } else {
                    audio.stop();
                    audio.dispose();
                    iterator.remove();
                    continue;
                }
            }

            float distance = listenerPos.dst(audio.getPosition());

            float finalVolume;
            float finalPan;

            if (distance > MAX_HEARING_DISTANCE) {
                finalVolume = 0;
                finalPan = 0;
            } else {
                finalVolume = (1.0f - (distance / MAX_HEARING_DISTANCE)) * globalSoundVolume;

                soundToListener.set(audio.getPosition()).sub(listenerPos).nor();
                finalPan = soundToListener.dot(cameraRight);
            }

            audio.update3D(finalVolume, finalPan);
        }
    }

    public void updateSoundPosition(String instanceName, float x, float y, float z) {
        if (instanceName == null || instanceName.isEmpty()) return;

        for (PlayableAudio audio : active3DSounds) {
            if (instanceName.equals(audio.getInstanceName())) {
                audio.setAttachedObjectId(null);
                audio.setPosition(new Vector3(x, y, z));
                break;
            }
        }
    }

    public boolean createSpringConstraint(String constraintId, String objectIdA, String objectIdB, Matrix4 frameInA, Matrix4 frameInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);

        btRigidBody bodyB = physicsBodies.get(objectIdB);
        if (bodyB == null) {
            //bodyB = btRigidBody.upcast(dynamicsWorld.getCollisionObjectArray().at(0));
        }

        if (bodyA == null) {
            Gdx.app.error("3DManager", "Cannot create spring constraint: bodyA not found.");
            return false;
        }

        btGeneric6DofSpringConstraint springConstraint = new btGeneric6DofSpringConstraint(bodyA, bodyB, frameInA, frameInB, true);
        dynamicsWorld.addConstraint(springConstraint, true);
        physicsConstraints.put(constraintId, springConstraint);
        return true;
    }

    public boolean createHingeConstraint(String constraintId, String objectIdA, String objectIdB,
                                         Vector3 pivotInA, Vector3 axisInA,
                                         Vector3 pivotInB, Vector3 axisInB) {
        if (physicsConstraints.containsKey(constraintId)) return false;
        btRigidBody bodyA = physicsBodies.get(objectIdA);
        btRigidBody bodyB = physicsBodies.get(objectIdB);

        if (bodyA == null || bodyB == null) {
            Gdx.app.error("3DManager", "Cannot create hinge: one or both bodies not found.");
            return false;
        }
        btHingeConstraint hinge = new btHingeConstraint(bodyA, bodyB, pivotInA, pivotInB, axisInA, axisInB, true);
        dynamicsWorld.addConstraint(hinge, true);
        physicsConstraints.put(constraintId, hinge);
        return true;
    }

    public void setHingeMotorTarget(String constraintId, float targetAngleDegrees, float maxMotorImpulse) {
        btTypedConstraint constraint = physicsConstraints.get(constraintId);
        if (constraint instanceof btHingeConstraint) {
            btHingeConstraint hinge = (btHingeConstraint) constraint;
            hinge.enableAngularMotor(true, 0, maxMotorImpulse);
            hinge.setMotorTarget(targetAngleDegrees * MathUtils.degreesToRadians, 0.1f);
        }
    }

    public void setAngularSpringProperties(String constraintId, int axisIndex, float stiffness, float damping) {
        btTypedConstraint constraint = physicsConstraints.get(constraintId);
        if (constraint instanceof btGeneric6DofSpringConstraint) {
            btGeneric6DofSpringConstraint spring = (btGeneric6DofSpringConstraint) constraint;
            int bulletAxisIndex = axisIndex + 3;

            spring.enableSpring(bulletAxisIndex, true);
            spring.setStiffness(bulletAxisIndex, stiffness);
            spring.setDamping(bulletAxisIndex, damping);
        }
    }


    public btRigidBody getPhysicsBody(String objectId) {
        return physicsBodies.get(objectId);
    }

    public void setFreeCamera() {
        this.cameraTargetId = null;
    }

    public void setThirdPersonCamera(String targetObjectId, float distance, float height, float pitch) {
        ModelInstance target = sceneObjects.get(targetObjectId);
        if (target == null) {
            Gdx.app.error("3DManager", "Camera target object not found: " + targetObjectId);
            this.cameraTargetId = null;
            return;
        }
        this.cameraTargetId = targetObjectId;
        this.cameraDistance = distance;
        this.cameraOffset.set(0, height, 0);
        this.cameraPitch = pitch;

        this.cameraYaw = target.transform.getRotation(new Quaternion()).getYaw();
    }

    private void updateThirdPersonCamera() {
        ModelInstance target = sceneObjects.get(cameraTargetId);
        if (target == null) {
            setFreeCamera();
            return;
        }

        Vector3 targetPosition = target.transform.getTranslation(new Vector3());
        Vector3 lookAtPoint = new Vector3(targetPosition).add(cameraOffset);

        Quaternion rotation = new Quaternion();
        rotation.set(Vector3.Y, cameraYaw);
        rotation.mul(new Quaternion(Vector3.X, -cameraPitch));

        Vector3 positionOffset = new Vector3(0, 0, cameraDistance);
        positionOffset.mul(rotation);

        Vector3 cameraPosition = new Vector3(lookAtPoint).add(positionOffset);

        camera.position.set(cameraPosition);
        camera.lookAt(lookAtPoint);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    public void addCameraRotation(float yawDelta, float pitchDelta) {
        if (cameraTargetId == null) return;

        cameraYaw += yawDelta;
        cameraPitch += pitchDelta;

        if (cameraPitch > 89.0f) cameraPitch = 89.0f;
        if (cameraPitch < -89.0f) cameraPitch = -89.0f;
    }

    public void setCameraRotation(float yaw, float pitch) {
        if (cameraTargetId == null) return;

        cameraYaw = yaw;
        cameraPitch = pitch;

        if (cameraPitch > 89.0f) cameraPitch = 89.0f;
        if (cameraPitch < -89.0f) cameraPitch = -89.0f;
    }

    public void playSoundAt(String instanceName, String soundName, float x, float y, float z, float volume, float pitch, boolean loop) {
        playSoundInternal(instanceName, soundName, volume, pitch, loop, new Vector3(x, y, z), null);
    }

    public void playSoundAttached(String instanceName, String soundName, String objectId, float volume, float pitch, boolean loop) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;
        Vector3 initialPos = instance.transform.getTranslation(new Vector3());
        playSoundInternal(instanceName, soundName, volume, pitch, loop, initialPos, objectId);
    }

    public boolean prepareAudio(String soundName, String fileName, boolean asMusic) {
        if (soundName == null || soundName.isEmpty() || loadedAudioAssets.containsKey(soundName)) {
            return false;
        }
        try {
            File soundFile = ProjectManager.getInstance().getCurrentProject().getFile(fileName);
            if (soundFile != null && soundFile.exists()) {
                FileHandle fileHandle = Gdx.files.absolute(soundFile.getAbsolutePath());
                AudioAsset asset = new AudioAsset(fileHandle, asMusic);
                loadedAudioAssets.put(soundName, asset);
                return true;
            }
        } catch (Exception e) {
            Gdx.app.error("ThreeDManager_Audio", "Failed to load audio: " + fileName, e);
        }
        return false;
    }


    private void playSoundInternal(String instanceName, String soundName, float volume, float pitch, boolean loop, Vector3 position, String attachedToId) {
        if (instanceName == null || instanceName.isEmpty()) return;
        stopSound(instanceName);

        AudioAsset asset = loadedAudioAssets.get(soundName);
        if (asset == null) return;

        PlayableAudio playableAudio;
        if (asset.music != null) {

            Music newMusicInstance = Gdx.audio.newMusic(Gdx.files.absolute(asset.filePath));
            playableAudio = new MusicWrapper(newMusicInstance, volume, pitch, loop);
        } else {
            playableAudio = new SoundWrapper(asset.sound, volume, pitch, loop);
        }

        playableAudio.setInstanceName(instanceName);
        playableAudio.setPosition(position);
        playableAudio.setAttachedObjectId(attachedToId);

        playableAudio.play();
        active3DSounds.add(playableAudio);
    }


    public void stopSound(String instanceName) {
        Iterator<PlayableAudio> iterator = active3DSounds.iterator();
        while (iterator.hasNext()) {
            PlayableAudio audio = iterator.next();
            if (audio.getInstanceName().equals(instanceName)) {
                audio.stop();
                audio.dispose();
                iterator.remove();
                break;
            }
        }
    }


    public void setGlobalSoundVolume(float volume) {
        this.globalSoundVolume = Math.max(0, Math.min(1, volume));
    }

    public Array<String> getAnimationNames(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        Array<String> names = new Array<>();
        if (instance != null && instance.animations.size > 0) {
            for (Animation anim : instance.animations) {
                names.add(anim.id);
            }
        }
        return names;
    }

    public void setSkybox(String panoramicTexturePath) {
        if (!realisticMode) {
            Gdx.app.error("Skybox", "Skybox can only be enabled in realistic rendering mode.");
            return;
        }

        try {
            FileHandle textureFile = Gdx.files.absolute(panoramicTexturePath);
            if (!textureFile.exists()) {
                Gdx.app.error("Skybox", "Skybox texture not found: " + panoramicTexturePath);
                return;
            }

            Texture panoramicTexture = new Texture(textureFile);

            if (skyboxCubemap != null) skyboxCubemap.dispose();
            if (skybox != null) skybox.dispose();

            skyboxCubemap = panoramicConverter.convert(panoramicTexture, 1024);

            skybox = new net.mgsx.gltf.scene3d.scene.SceneSkybox(skyboxCubemap);
            sceneManager.setSkyBox(skybox);

            updateProceduralIBL();

            panoramicTexture.dispose();

            Gdx.app.log("Skybox", "Skybox set successfully. Using procedural IBL as fallback.");

        } catch (Exception e) {
            Gdx.app.error("Skybox", "Failed to set skybox", e);
        }
    }

    /**
     * Вспомогательный метод для обновления процедурных карт освещения.
     */
    private void updateProceduralIBL() {
        if (diffuseCubemap != null) diffuseCubemap.dispose();
        if (specularCubemap != null) specularCubemap.dispose();
        
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(pbrLight);

        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);

        iblBuilder.dispose();

        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.DiffuseEnv, diffuseCubemap));
        sceneManager.environment.set(new PBRCubemapAttribute(PBRCubemapAttribute.SpecularEnv, specularCubemap));
    }

    /**
     * Включает или выключает режим реалистичного PBR-рендеринга.
     * @param enabled true для включения, false для возврата к старому рендеру.
     */
    public void enableRealisticRendering(boolean enabled) {
        this.realisticMode = enabled;
    }

    /**
     * Задает линейную скорость физического объекта.
     * Работает только для динамических объектов (Dynamic).
     * @param objectId ID объекта.
     * @param vx Скорость по оси X.
     * @param vy Скорость по оси Y.
     * @param vz Скорость по оси Z.
     */
    public void setVelocity(String objectId, float vx, float vy, float vz) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            body.activate();
            body.setLinearVelocity(new Vector3(vx, vy, vz));
        }
    }

    public void setGravity(float x, float y, float z) {
        dynamicsWorld.setGravity(new Vector3(x, y, z));
    }

    public void render() {
        try {
            camera.update();

            if (realisticMode) {
                if (skyColor.a != 0) {
                    Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                    Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
                }

                sceneManager.render();

            } else {
                Vector3 lightPosition = new Vector3(150, 200, 100);
                Vector3 lightLookAt = new Vector3(0, 0, 0);
                lightCamera.position.set(lightPosition);
                lightCamera.lookAt(lightLookAt);
                lightCamera.update();

                lightSpaceMatrix.set(lightCamera.combined);

                shadowFBO.begin();
                Gdx.gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
                Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);

                shadowBatch.begin(lightCamera);
                for (ModelInstance instance : sceneObjects.values()) {
                    shadowBatch.render(instance);
                }
                shadowBatch.end();

                shadowFBO.end();

                Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

                if (skyColor.a != 0) {
                    Gdx.gl.glClearColor(skyColor.r, skyColor.g, skyColor.b, skyColor.a);
                    Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT | com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
                }

                modelBatch.begin(camera);

                if (customShaderProvider != null) {
                    int shadowMapTextureUnit = 5;
                    shadowFBO.getColorBufferTexture().bind(shadowMapTextureUnit);
                    setShaderUniform("shadowMap", shadowMapTextureUnit);
                    customUniforms.put("u_lightSpaceMatrix", lightSpaceMatrix);
                    customUniforms.put("u_shadowMapSize", new Vector2(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE));
                }

                for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
                    if (!inactiveRenderObjects.contains(entry.getKey())) {
                        modelBatch.render(entry.getValue(), environment);
                    }
                }
                modelBatch.end();
            }

            if (debugEnabled) {
                debugDrawer.begin(camera);
                dynamicsWorld.debugDrawWorld();
                debugDrawer.end();
            }
        } catch (Exception e) {
            Log.e("ThreeDManager", "FATAL RENDER ERROR: " + e);
        }
    }

    private <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Задает сплошной цвет для всех материалов объекта.
     * Примечание: Это удалит текстуру, если она была установлена.
     * @param objectId ID объекта.
     * @param r Красный компонент (0.0 - 1.0).
     * @param g Зеленый компонент (0.0 - 1.0).
     * @param b Синий компонент (0.0 - 1.0).
     */
    public void setObjectColor(String objectId, float r, float g, float b) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.remove(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);
            material.set(ColorAttribute.createDiffuse(r, g, b, 1.0f));
        }
    }

    /**
     * Задает текстуру для всех материалов объекта из файла проекта.
     * @param objectId ID объекта.
     * @param texturePath Абсолютный путь к файлу текстуры (PNG/JPG).
     */
    public void setObjectTexture(String objectId, String texturePath) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null || texturePath == null || texturePath.isEmpty()) return;

        Texture texture = loadedTextures.get(texturePath);

        if (texture == null) {
            try {
                FileHandle textureFile = Gdx.files.absolute(texturePath);
                if (textureFile.exists()) {
                    texture = new com.badlogic.gdx.graphics.Texture(textureFile);
                    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                    loadedTextures.put(texturePath, texture);
                } else {
                    Gdx.app.error("3DManager", "Texture file not found: " + texturePath);
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Could not load texture: " + texturePath, e);
                return;
            }
        }


        for (com.badlogic.gdx.graphics.g3d.Material material : instance.materials) {
            material.set(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(texture));

            material.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(
                    com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
            ));
        }
    }

    /**
     * Создает СЛОЖНОЕ физическое тело, основанное на геометрии модели.
     * Только для статичных объектов!
     */
    private void createMeshPhysicsBody(String objectId, ModelInstance instance) {

        com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray vertexArray =
                new com.badlogic.gdx.physics.bullet.collision.btTriangleIndexVertexArray(instance.model.meshParts);

        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);

        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        meshShape.setLocalScaling(scale);

        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        float mass = 0f;
        Vector3 localInertia = new Vector3(0, 0, 0);

        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, meshShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }



    /**
     * Проверяет, есть ли физический контакт между двумя объектами.
     * @param objectId1 ID первого объекта.
     * @param objectId2 ID второго объекта.
     * @return true, если объекты касаются, иначе false.
     */
    public boolean checkCollision(String objectId1, String objectId2) {
        btRigidBody body1 = physicsBodies.get(objectId1);
        btRigidBody body2 = physicsBodies.get(objectId2);

        if (body1 == null || body2 == null) {
            return false;
        }

        collisionCallback.collided = false;
        dynamicsWorld.contactPairTest(body1, body2, collisionCallback);

        return collisionCallback.collided;
    }

    /**
     * Пускает луч в физическом мире и сохраняет результат.
     * @param rayName Имя для этого луча, чтобы потом получить результат.
     * @param from Начальная точка луча в мировых координатах.
     * @param direction Нормализованный вектор направления луча.
     */
    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        tmpPos.set(from).add(direction.x * camera.far, direction.y * camera.far, direction.z * camera.far);

        com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback callback =
                new com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback(from, tmpPos);

        dynamicsWorld.rayTest(from, tmpPos, callback);

        RayCastResult result = rayCastResults.get(rayName);
        if (result == null) {
            result = new RayCastResult();
            rayCastResults.put(rayName, result);
        }
        if (callback.hasHit()) {
            result.hasHit = true;

            callback.getHitPointWorld(result.hitPoint);
            callback.getHitNormalWorld(result.hitNormal);

            com.badlogic.gdx.physics.bullet.collision.btCollisionObject hitObject = callback.getCollisionObject();
            for (Map.Entry<String, btRigidBody> entry : physicsBodies.entrySet()) {
                if (entry.getValue().equals(hitObject)) {
                    result.hitObjectId = entry.getKey();
                    break;
                }
            }
            result.hitDistance = from.dst(result.hitPoint);
        } else {
            result.hasHit = false;
            result.hitObjectId = "";
            result.hitDistance = -1.0f;
        }

        rayCastResults.put(rayName, result);
        callback.dispose();
    }

    public boolean getRayDidHit(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return result != null && result.hasHit;
    }

    public float getRayHitPointX(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.x : 0f;
    }

    public float getRayHitPointY(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.y : 0f;
    }

    public float getRayHitPointZ(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitPoint.z : 0f;
    }

    public float getRayHitNormalX(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.x : 0f;
    }

    public float getRayHitNormalY(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.y : 0f;
    }

    public float getRayHitNormalZ(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null && result.hasHit) ? result.hitNormal.z : 0f;
    }

    /**
     * Возвращает дистанцию последнего столкновения для именованного луча.
     * @param rayName Имя луча.
     * @return Дистанция или -1, если луч ни во что не попал или не существует.
     */
    public float getRaycastDistance(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitDistance : -1.0f;
    }

    public void setRotation(String objectId, Quaternion newRotation) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        instance.transform.set(position, newRotation, scale);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 transform = body.getWorldTransform();
            transform.getTranslation(position); 
            transform.set(position, newRotation);
            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }

    /**
     * Возвращает ID объекта, с которым столкнулся именованный луч.
     * @param rayName Имя луча.
     * @return ID объекта или пустая строка, если луч ни во что не попал или не существует.
     */
    public String getRaycastHitObjectId(String rayName) {
        RayCastResult result = rayCastResults.get(rayName);
        return (result != null) ? result.hitObjectId : "";
    }



    /**
     * Создает 3D-объект из файла модели.
     * Автоматически определяет, является ли путь абсолютным (начинается с "/")
     * или внутренним (относительно папки assets/models).
     * @param objectId Уникальный ID для нового объекта.
     * @param modelPath Путь к файлу модели (.obj).
     *                  Может быть абсолютным (e.g., "/storage/emulated/0/Download/car.obj")
     *                  или относительным (e.g., "car.obj").
     * @return true в случае успеха, false если модель не найдена или ID занят.
     */
    public boolean createObject(String objectId, String modelPath) {
        if (sceneObjects.containsKey(objectId)) return false;

        try {
            FileHandle fileHandle;
            if (modelPath.startsWith("/")) {
                fileHandle = Gdx.files.absolute(modelPath);
            } else {
                fileHandle = Gdx.files.internal("models/" + modelPath);
            }

            if (!fileHandle.exists()) {
                Gdx.app.error("3DManager", "Model file not found: " + fileHandle.path());
                return false;
            }

            String lowerCasePath = modelPath.toLowerCase();

            if (lowerCasePath.endsWith(".glb") || lowerCasePath.endsWith(".gltf")) {
                net.mgsx.gltf.scene3d.scene.SceneAsset sceneAsset;

                if (lowerCasePath.endsWith(".glb")) {
                    sceneAsset = new net.mgsx.gltf.loaders.glb.GLBLoader().load(fileHandle, true);
                } else {
                    sceneAsset = new net.mgsx.gltf.loaders.gltf.GLTFLoader().load(fileHandle, true);
                }

                if (sceneAsset != null) {

                    net.mgsx.gltf.scene3d.scene.Scene scene = new net.mgsx.gltf.scene3d.scene.Scene(sceneAsset.scene);

                    sceneManager.addScene(scene);
                    sceneObjects.put(objectId, scene.modelInstance);

                    gltfObjectIds.add(objectId);

                    if (scene.modelInstance.animations.size > 0) {
                        com.badlogic.gdx.graphics.g3d.utils.AnimationController controller = new com.badlogic.gdx.graphics.g3d.utils.AnimationController(scene.modelInstance);
                        animationControllers.put(objectId, controller);
                    }
                    return true;
                }


            } else {
                Model model = loadedModels.get(modelPath);

                if (model == null) {
                    try {
                        FileHandle modelFileHandle;

                        if (modelPath.startsWith("/")) {
                            modelFileHandle = Gdx.files.absolute(modelPath);
                        } else {
                            modelFileHandle = Gdx.files.internal("models/" + modelPath);
                        }

                        if (!modelFileHandle.exists()) {
                            Gdx.app.error("3DManager", "Model file does not exist: " + modelPath);
                            return false;
                        }

                        FileHandle patchedModelHandle = ModelPathProcessor.process(modelFileHandle);

                        Gdx.app.log("3DManager", "--- Verification before loading model ---");
                        FileHandle textureToVerify = Gdx.files.local("Lowpoly_Laptop_Nor_2.jpg");
                        Gdx.app.log("3DManager", "Verifying texture path: " + textureToVerify.path());
                        Gdx.app.log("3DManager", "Does texture exist at path? -> " + textureToVerify.exists());
                        Gdx.app.log("3DManager", "--- Verification finished ---");

                        FileHandleResolver resolver = new FileHandleResolver() {
                            @Override
                            public FileHandle resolve(String fileName) {
                                return patchedModelHandle.parent().child(fileName);
                            }
                        };

                        ObjLoader loader = new ObjLoader(resolver);

                        model = loader.loadModel(patchedModelHandle, true);
                        loadedModels.put(modelPath, model);
                    } catch (Exception e) {
                        Gdx.app.error("3DManager", "Could not load model: " + modelPath, e);
                        return false;
                    }
                }

                ModelInstance instance = new ModelInstance(model);
                sceneObjects.put(objectId, instance);

                return true;
            }



        } catch (Exception e) {
            Gdx.app.error("3DManager_PBR", "Failed to load GLTF model: " + modelPath, e);
            e.printStackTrace();
            return false;
        }

        return false;
    }

    /**
     * Запускает проигрывание анимации для объекта.
     * @param objectId ID объекта.
     * @param animationName Название анимации (например, "Walk", "Run", "Action").
     * @param loops Количество повторов. -1 для бесконечного зацикливания. 1 для одного раза.
     * @param speed Скорость воспроизведения. 1.0 - нормальная. 2.0 - в два раза быстрее.
     * @param transitionTime Время плавного перехода от предыдущей анимации (в секундах). 0.2 - хорошее значение.
     */
    public void playAnimation(String objectId, String animationName, int loops, float speed, float transitionTime) {
        AnimationController controller = animationControllers.get(objectId);
        if (controller != null) {
            controller.animate(animationName, loops, speed, new AnimationController.AnimationListener() {
                @Override
                public void onLoop(AnimationController.AnimationDesc animation) {

                }

                @Override
                public void onEnd(AnimationController.AnimationDesc animation) {

                }
            }, transitionTime);
        }
    }

    /**
     * Останавливает все анимации на объекте.
     * @param objectId ID объекта.
     */
    public void stopAnimation(String objectId) {
        AnimationController controller = animationControllers.get(objectId);
        if (controller != null) {
            controller.setAnimation(null);
        }
    }

    /**
     * Создает или воссоздает базовое освещение сцены (солнце с тенями и IBL).
     * Этот метод - гарантия того, что тени всегда будут.
     */
    private void setupDefaultLighting() {
        if (pbrLight != null) {
            sceneManager.environment.remove(pbrLight);
            pbrLight = null;
        }

        net.mgsx.gltf.scene3d.lights.DirectionalShadowLight shadowLight = new net.mgsx.gltf.scene3d.lights.DirectionalShadowLight(2048, 2048);
        shadowLight.direction.set(1, -1.5f, 1).nor();
        shadowLight.color.set(com.badlogic.gdx.graphics.Color.WHITE);
        shadowLight.intensity = 5.0f;
        shadowLight.getCamera().far = 1000f;

        sceneManager.environment.add(shadowLight);
        pbrLight = shadowLight;

        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        updateProceduralIBL();
    }

    /**
     * Управляет направленным светом в режиме реалистичного рендеринга.
     * @param dirX Направление по X
     * @param dirY Направление по Y
     * @param dirZ Направление по Z
     * @param intensity Интенсивность (для PBR хорошие значения от 1.0 до 10.0)
     */
    public void setRealisticSunLight(float dirX, float dirY, float dirZ, float intensity) {
        
        if (pbrLight instanceof net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) {
            net.mgsx.gltf.scene3d.lights.DirectionalShadowLight sun = (net.mgsx.gltf.scene3d.lights.DirectionalShadowLight) pbrLight;

            sun.direction.set(dirX, dirY, dirZ).nor();
            sun.intensity = intensity;

            Gdx.app.log("ThreeDManager", "Sun light updated and IBL recalculated.");
        } else {
            Gdx.app.error("ThreeDManager", "setRealisticSunLight called, but no DirectionalShadowLight was found!");
        }
    }

    
    
    public void setSunLightColor(float r, float g, float b) {
        if (pbrLight != null) {
            pbrLight.color.set(r, g, b, 1);
        }
    }

    public void applyForce(String objectId, float forceX, float forceY, float forceZ) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.activate();
            body.applyCentralForce(new Vector3(forceX, forceY, forceZ));
        }
    }

    /**
     * Задает или изменяет физическое состояние объекта с выбором формы.
     * @param objectId ID объекта.
     * @param state Тип физики (NONE, STATIC, DYNAMIC).
     * @param shape Форма коллайдера (BOX, SPHERE, CAPSULE).
     * @param mass Масса объекта (для DYNAMIC).
     */
    public void setPhysicsState(String objectId, PhysicsState state, PhysicsShape shape, float mass) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        removePhysicsBody(objectId);
        if (state == PhysicsState.NONE) return;

        float bodyMass = (state == PhysicsState.DYNAMIC) ? mass : 0f;
        if (state == PhysicsState.DYNAMIC && mass <= 0) {
            Gdx.app.error("3DManager", "Dynamic body must have mass > 0.");
            return;
        }

        if (state == PhysicsState.MESH_STATIC) {
            if (gltfObjectIds.contains(objectId)) {
                createGltfMeshPhysicsBody(objectId, instance);
            } else {
                createMeshPhysicsBody(objectId, instance);
            }
        } else {
            createPrimitivePhysicsBody(objectId, instance, shape, bodyMass);
        }
    }

    
    public void setPhysicsState(String objectId, PhysicsState state, float mass) {
        setPhysicsState(objectId, state, PhysicsShape.BOX, mass);
    }

    /**
     * Вспомогательный метод для создания простых физических тел.
     * ЭТО ОРИГИНАЛЬНЫЙ, РАБОЧИЙ МЕТОД.
     */
    private void createPrimitivePhysicsBody(String objectId, ModelInstance instance, PhysicsShape shapeType, float mass) {
        BoundingBox bbox = new BoundingBox();
        instance.calculateBoundingBox(bbox);
        Vector3 dimensions = bbox.getDimensions(new Vector3());
        
        Vector3 center = bbox.getCenter(new Vector3());
        btCollisionShape shape;
        switch (shapeType) {
            case SPHERE:
                float radius = Math.max(dimensions.x, Math.max(dimensions.y, dimensions.z)) / 2f;
                shape = new btSphereShape(radius);
                break;
            case CAPSULE:
                float capsuleRadius = Math.max(dimensions.x, dimensions.z) / 2f;
                float capsuleHeight = dimensions.y - (2 * capsuleRadius);
                shape = new btCapsuleShape(capsuleRadius, Math.max(0, capsuleHeight));
                break;
            case BOX:
            default:
                shape = new btBoxShape(dimensions.cpy().scl(0.5f));
                break;
        }

        btCompoundShape compoundShape = new btCompoundShape();
        
        Matrix4 shapeOffsetTransform = new Matrix4().setToTranslation(center);
        compoundShape.addChildShape(shapeOffsetTransform, shape);

        Vector3 scale = instance.transform.getScale(new Vector3());
        compoundShape.setLocalScaling(scale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        Matrix4 bodyTransform = new Matrix4();
        bodyTransform.set(instance.transform.getRotation(new Quaternion()));
        bodyTransform.setTranslation(instance.transform.getTranslation(new Vector3()));

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        if (mass > 0) {
            body.setAngularFactor(1f);

            body.setDamping(0.5f, 0.5f);
        }

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        Array<Disposable> disposables = new Array<>();
        disposables.add(shape);
        disposables.add(compoundShape);
        body.userData = disposables;
        physicsResources.put(objectId, disposables);

        bodyInfo.dispose();
    }

    

    /**
     * Создает СЛОЖНОЕ физическое тело для GLTF/GLB моделей (метод "запекания").
     * Этот метод гарантирует, что все трансформации узлов будут применены.
     */
    private void createGltfMeshPhysicsBody(String objectId, ModelInstance instance) {
        Gdx.app.log("PhysicsDebug", "============================================================");
        Gdx.app.log("PhysicsDebug", "=== STARTING MESH BODY CREATION (BAKING METHOD) for object: '" + objectId + "'");
        Gdx.app.log("PhysicsDebug", "============================================================");

        instance.calculateTransforms();

        btCompoundShape compoundShape = new btCompoundShape();
        Array<Disposable> disposables = new Array<>();

        if (instance.nodes.size == 0) {
            Gdx.app.error("PhysicsDebug", "FATAL: ModelInstance has NO nodes!");
        } else {
            Gdx.app.log("PhysicsDebug", "Model has " + instance.nodes.size + " root nodes. Starting recursion...");

            addPartsToCompoundShapeRecursive(instance.nodes, instance.transform, compoundShape, disposables, "  ");
        }

        int childCount = compoundShape.getNumChildShapes();
        Gdx.app.log("PhysicsDebug", "RECURSION FINISHED. Total child shapes in CompoundShape: " + childCount);

        if (childCount == 0) {
            Gdx.app.error("PhysicsDebug", "FATAL: CompoundShape is EMPTY!");
            compoundShape.dispose();
            for (Disposable d : disposables) d.dispose();
            Gdx.app.log("PhysicsDebug", "=================== CREATION FAILED ===================");
            return;
        }

        Matrix4 bodyTransform = new Matrix4().idt();

        Gdx.app.log("PhysicsDebug", "RigidBody initial transform is IDENTITY because geometry is pre-transformed.");

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(0f, motionState, compoundShape, new Vector3(0, 0, 0));
        btRigidBody body = new btRigidBody(bodyInfo);

        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
        body.setActivationState(Collision.DISABLE_DEACTIVATION);

        dynamicsWorld.addRigidBody(body);
        dynamicsWorld.updateSingleAabb(body);

        physicsBodies.put(objectId, body);
        disposables.add(compoundShape);
        body.userData = disposables;
        physicsResources.put(objectId, disposables);

        Gdx.app.log("PhysicsDebug", "SUCCESS: RigidBody added to dynamicsWorld. AABB was manually updated.");
        Gdx.app.log("PhysicsDebug", "=================== CREATION FINISHED for: '" + objectId + "' ===================");

        bodyInfo.dispose();
    }

    /**
     * Создает визуальный прокси-объект для редактора (например, иконку для света).
     * @param ownerId ID основного GameObject, которому принадлежит этот прокси.
     */
    public void createEditorProxy(String ownerId) {
        if (editorProxies.containsKey(ownerId) || lightProxyModel == null) return;
        ModelInstance proxyInstance = new ModelInstance(lightProxyModel);
        editorProxies.put(ownerId, proxyInstance);
    }

    /**
     * Удаляет прокси-объект редактора.
     * @param ownerId ID основного GameObject.
     */
    public void removeEditorProxy(String ownerId) {
        editorProxies.remove(ownerId);
    }

    /**
     * Обновляет позицию прокси-объекта.
     * @param ownerId ID основного GameObject.
     * @param position Новая позиция.
     */
    public void updateEditorProxyPosition(String ownerId, Vector3 position) {
        ModelInstance proxy = editorProxies.get(ownerId);
        if (proxy != null) {
            proxy.transform.setTranslation(position);
        }
    }

    /**
     * Возвращает карту всех прокси-объектов для рейкастинга и рендеринга.
     */
    public Map<String, ModelInstance> getEditorProxies() {
        return editorProxies;
    }

    /**
     * Рекурсивно обходит узлы и добавляет их геометрию в составную форму.
     */
    
    private void addPartsToCompoundShapeRecursive(Iterable<Node> nodes, Matrix4 rootTransform, btCompoundShape compoundShape, Array<Disposable> disposables, String indent) {
        for (Node node : nodes) {
            Gdx.app.log("PhysicsDebug", indent + "-> Processing Node: '" + node.id + "'");

            Matrix4 finalTransform = new Matrix4(rootTransform).mul(node.globalTransform);

            if (node.parts.size > 0) {
                for (NodePart nodePart : node.parts) {
                    MeshPart originalMeshPart = nodePart.meshPart;
                    if (originalMeshPart != null && originalMeshPart.mesh != null && originalMeshPart.mesh.getNumVertices() > 0) {
                        Mesh bakedMesh = originalMeshPart.mesh.copy(true);

                        bakedMesh.transform(finalTransform);
                        disposables.add(bakedMesh); 

                        MeshPart bakedMeshPart = new MeshPart("baked_part_" + node.id, bakedMesh, 0, bakedMesh.getNumIndices(), GL20.GL_TRIANGLES);

                        btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(bakedMeshPart);
                        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);
                        meshShape.setMargin(0.04f);

                        disposables.add(vertexArray);
                        disposables.add(meshShape);

                        compoundShape.addChildShape(new Matrix4(), meshShape); 
                        Gdx.app.log("PhysicsDebug", indent + "  SUCCESS: Baked and added child shape.");
                    }
                }
            }

            if (node.hasChildren()) {
                addPartsToCompoundShapeRecursive(node.getChildren(), rootTransform, compoundShape, disposables, indent + "  ");
            }
        }
    }

    Boolean objectExists(String id) {
        return sceneObjects.containsKey(id);
    }

    /**
     * Возвращает текущую линейную скорость физического объекта.
     * @param objectId ID объекта.
     * @return Vector3 со скоростью. Возвращает (0,0,0), если у объекта нет физики.
     */
    public Vector3 getVelocity(String objectId) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && body.getInvMass() > 0) {
            return body.getLinearVelocity();
        }
        return Vector3.Zero;
    }

    /**
     * Возвращает текущий поворот камеры в виде углов Эйлера (в градусах).
     * @return Vector3, где: x = тангаж, y = рыскание, z = крен.
     */
    public Vector3 getCameraRotation() {
        Quaternion q = new Quaternion();

        q.setFromMatrix(camera.view);

        q.conjugate();

        return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
    }

    public ModelInstance getModelInstance(String objectId) {
        return sceneObjects.get(objectId);
    }

    /**
     * Создает простую 3D-сферу.
     * @param objectId Уникальный ID для нового объекта.
     * @return true в случае успеха, false если ID уже занят.
     */
    public boolean createSphere(String objectId) {
        if (sceneObjects.containsKey(objectId)) return false;

        final String SPHERE_MODEL_KEY = "__PRIMITIVE_SPHERE__";
        Model sphereModel = loadedModels.get(SPHERE_MODEL_KEY);

        if (sphereModel == null) {
            Material pbrMaterial = new Material(
                    PBRColorAttribute.createBaseColorFactor(Color.WHITE)
            );

            final long attributes = VertexAttributes.Usage.Position |
                    VertexAttributes.Usage.Normal |
                    VertexAttributes.Usage.TextureCoordinates |
                    VertexAttributes.Usage.Tangent;

            sphereModel = modelBuilder.createSphere(50f, 50f, 50f, 16, 16,
                    pbrMaterial,
                    attributes);
            loadedModels.put(SPHERE_MODEL_KEY, sphereModel);
        }

        ModelInstance instance = new ModelInstance(sphereModel);
        sceneObjects.put(objectId, instance);

        if (realisticMode && sceneManager != null) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(instance));
        }

        return true;
    }

    private void createPhysicsBody(String objectId, ModelInstance instance, float mass) {
        BoundingBox bbox = new BoundingBox();
        instance.model.calculateBoundingBox(bbox);
        Vector3 dimensions = new Vector3();
        bbox.getDimensions(dimensions);
        Vector3 center = new Vector3();
        bbox.getCenter(center);

        btBoxShape boxShape = new btBoxShape(dimensions.scl(0.5f));

        btCompoundShape compoundShape = new btCompoundShape();
        com.badlogic.gdx.math.Matrix4 shapeOffsetTransform = new com.badlogic.gdx.math.Matrix4();
        shapeOffsetTransform.setToTranslation(center);
        compoundShape.addChildShape(shapeOffsetTransform, boxShape);

        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        compoundShape.setLocalScaling(scale);

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            compoundShape.calculateLocalInertia(mass, localInertia);
        }

        com.badlogic.gdx.math.Matrix4 bodyTransform = new com.badlogic.gdx.math.Matrix4();
        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);
        bodyTransform.set(position, rotation);

        btMotionState motionState = new btDefaultMotionState(bodyTransform);
        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);
        btRigidBody body = new btRigidBody(bodyInfo);

        dynamicsWorld.addRigidBody(body);
        physicsBodies.put(objectId, body);

        bodyInfo.dispose();
    }

    /**
     * Создает СЛОЖНОЕ физическое тело для GLTF/GLB моделей,
     * учитывая иерархию узлов (Nodes).
     */
    /**
     * Создаёт физическое тело для GLTF/GLB модели, корректно формируя btTriangleIndexVertexArray из MeshPart.
     * Надёжно поддерживает 16- и 32-битные индексы.
     */

    private final Map<String, com.badlogic.gdx.utils.Array<Disposable>> physicsResources = new HashMap<>();


    /*private void createGltfMeshPhysicsBody(String objectId, ModelInstance instance) {
        Gdx.app.log("PhysicsDebug", "--- Creating GLTF Mesh Body for: " + objectId + " (CompoundShape fixed) ---");
        instance.calculateTransforms();

        btCompoundShape compoundShape = new btCompoundShape();
        Array<Disposable> disposables = new Array<>();

        
        addPartsToCompoundShapeRecursive(instance.nodes, compoundShape, disposables);

        Gdx.app.log("PhysicsDebug", "CompoundShape num child shapes: " + compoundShape.getNumChildShapes());
        if (compoundShape.getNumChildShapes() == 0) {
            Gdx.app.error("PhysicsDebug", "FATAL: CompoundShape is empty!");
            compoundShape.dispose();
            for (Disposable d : disposables) try { d.dispose(); } catch (Exception ignored) {}
            return;
        }

        
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        compoundShape.setLocalScaling(scale);

        Gdx.app.log("PhysicsDebug", "--- CompoundShape info for " + objectId + " ---");
        for (int i = 0; i < compoundShape.getNumChildShapes(); i++) {
            btCollisionShape child = compoundShape.getChildShape(i);
            Matrix4 localTransform = new Matrix4(compoundShape.getChildTransform(i));
            Gdx.app.log("PhysicsDebug", "Child " + i + ": shape=" + child + " transform=" + localTransform);
        }


        
        Matrix4 bodyTransform = new Matrix4(instance.transform);
        
        

        btMotionState motionState = new btDefaultMotionState(bodyTransform);

        float mass = 0f; 
        Vector3 localInertia = new Vector3(0, 0, 0);

        btRigidBody.btRigidBodyConstructionInfo bodyInfo =
                new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, compoundShape, localInertia);

        btRigidBody body = new btRigidBody(bodyInfo);

        
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_STATIC_OBJECT);
        body.setContactProcessingThreshold(0f);
        body.setActivationState(Collision.DISABLE_DEACTIVATION);

        
        dynamicsWorld.addRigidBody(body);

        
        dynamicsWorld.updateSingleAabb(body);

        
        disposables.add(compoundShape);
        disposables.add(motionState);
        disposables.add(bodyInfo);
        

        body.userData = disposables;
        physicsResources.put(objectId, disposables);
        physicsBodies.put(objectId, body);

        
        Gdx.app.log("PhysicsDebug", "--- Finished GLTF Mesh Body creation for: " + objectId + " ---");
    }



    private void addPartsToCompoundShapeRecursive(Iterable<Node> nodes, btCompoundShape compoundShape, Array<Disposable> disposables) {
        for (Node node : nodes) {
            if (node.parts.size > 0) {
                for (NodePart nodePart : node.parts) {
                    MeshPart meshPart = nodePart.meshPart;
                    if (meshPart != null && meshPart.size > 0 && meshPart.mesh != null) {


                        btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(meshPart);


                        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);


                        disposables.add(vertexArray);
                        disposables.add(meshShape);




                        compoundShape.addChildShape(node.globalTransform, meshShape);
                    }
                }
            }


            if (node.hasChildren()) {
                addPartsToCompoundShapeRecursive(node.getChildren(), compoundShape, disposables);
            }
        }
    }



    private void collectMeshPartsRecursive(Node node, Array<MeshPart> out) {
        for (NodePart part : node.parts) {
            if (part.meshPart != null)
                out.add(part.meshPart);
        }
        for (Node child : node.getChildren()) {
            collectMeshPartsRecursive(child, out);
        }
    }


    private void addNodePartsToCompoundShapeRecursive(Iterable<com.badlogic.gdx.graphics.g3d.model.Node> nodes,
                                                      btCompoundShape compoundShape,
                                                      com.badlogic.gdx.utils.Array<Disposable> disposables) {
        for (com.badlogic.gdx.graphics.g3d.model.Node node : nodes) {
            if (node.parts.size > 0) {
                for (com.badlogic.gdx.graphics.g3d.model.NodePart nodePart : node.parts) {
                    com.badlogic.gdx.graphics.g3d.model.MeshPart meshPart = nodePart.meshPart;
                    if (meshPart.size > 0 && meshPart.mesh != null) {

                        btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(meshPart);
                        disposables.add(vertexArray);

                        btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);
                        disposables.add(meshShape);


                        compoundShape.addChildShape(node.globalTransform, meshShape);
                    }
                }
            }

            if (node.hasChildren()) {
                addNodePartsToCompoundShapeRecursive(node.getChildren(), compoundShape, disposables);
            }
        }
    }*/

    /**
     * Вспомогательная рекурсивная функция для сбора всех MeshPart из модели.
     */
    /*private void collectMeshPartsRecursive(com.badlogic.gdx.graphics.g3d.model.Node node, com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.model.MeshPart> out) {
        for (com.badlogic.gdx.graphics.g3d.model.NodePart part : node.parts) {
            out.add(part.meshPart);
        }
        for (com.badlogic.gdx.graphics.g3d.model.Node child : node.getChildren()) {
            collectMeshPartsRecursive(child, out);
        }
    }*/

    /**
     * Рекурсивная вспомогательная функция для обхода дерева узлов.
     * @param node Узел для обработки.
     * @param compoundShape Составная форма, в которую добавляются части.
     */
    private void addNodePartsToCompoundShape(com.badlogic.gdx.graphics.g3d.model.Node node, btCompoundShape compoundShape) {
        if (node.parts.size > 0) {
            Gdx.app.log("PhysicsDebug", "Processing Node: " + node.id + " with " + node.parts.size + " parts.");

            com.badlogic.gdx.utils.Array<Disposable> disposables = new com.badlogic.gdx.utils.Array<>();

            for (com.badlogic.gdx.graphics.g3d.model.NodePart nodePart : node.parts) {
                com.badlogic.gdx.graphics.g3d.model.MeshPart meshPart = nodePart.meshPart;
                if (meshPart.size > 0) {
                    btTriangleIndexVertexArray vertexArray = new btTriangleIndexVertexArray(meshPart);
                    disposables.add(vertexArray);

                    btBvhTriangleMeshShape meshShape = new btBvhTriangleMeshShape(vertexArray, true);
                    disposables.add(meshShape);

                    compoundShape.addChildShape(node.globalTransform, meshShape);
                }
            }

            for (Disposable d : disposables) {
                d.dispose();
            }
        }


        for (com.badlogic.gdx.graphics.g3d.model.Node child : node.getChildren()) {
            addNodePartsToCompoundShape(child, compoundShape);
        }
    }

    /**
     * Полностью удаляет физическое тело, связанное с ID.
     */
    void removePhysicsBody(String objectId) {
        btRigidBody body = physicsBodies.remove(objectId);
        if (body != null) {
            dynamicsWorld.removeRigidBody(body);

            com.badlogic.gdx.utils.Array<Disposable> resources = physicsResources.remove(objectId);
            if (resources != null) {
                for (Disposable d : resources) {
                    try {
                        d.dispose();
                    } catch (Exception ignored) {
                    }
                }
            }

            if (body.userData instanceof com.badlogic.gdx.utils.Array) {
                com.badlogic.gdx.utils.Array<Disposable> disposables = (com.badlogic.gdx.utils.Array<Disposable>) body.userData;
                for (Disposable d : disposables) {
                    try {
                        d.dispose();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (body.getMotionState() != null) body.getMotionState().dispose();
            if (body.getCollisionShape() != null) body.getCollisionShape().dispose();
            body.dispose();
        }
    }

    /**
     * Проверяет, пересекаются ли ограничивающие коробки двух 3D-объектов.
     * @param objectId1 ID первого объекта.
     * @param objectId2 ID второго объекта.
     * @return true, если объекты пересекаются, иначе false.
     */
    public boolean checkIntersection(String objectId1, String objectId2) {
        ModelInstance instance1 = sceneObjects.get(objectId1);
        ModelInstance instance2 = sceneObjects.get(objectId2);

        if (instance1 == null || instance2 == null) {
            return false;
        }

        instance1.calculateBoundingBox(bounds1);

        bounds1.mul(instance1.transform);

        instance2.calculateBoundingBox(bounds2);
        bounds2.mul(instance2.transform);

        return bounds1.intersects(bounds2);
    }

    public boolean removeObject(String objectId) {
        manager.removeGameObject(manager.findGameObject(objectId));
        ModelInstance instance = sceneObjects.remove(objectId);
        if (instance != null) {
            removePhysicsBody(objectId);
            animationControllers.remove(objectId);

            gltfObjectIds.remove(objectId);

            com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g3d.RenderableProvider> providers = sceneManager.getRenderableProviders();

            for (com.badlogic.gdx.graphics.g3d.RenderableProvider provider : providers) {

                if (provider instanceof net.mgsx.gltf.scene3d.scene.Scene) {
                    net.mgsx.gltf.scene3d.scene.Scene scene = (net.mgsx.gltf.scene3d.scene.Scene) provider;


                    if (scene.modelInstance == instance) {

                        sceneManager.removeScene(scene);

                        break;
                    }
                }
            }

            return true;
        }
        return false;
    }

    public boolean removeLight(String id) {
        DirectionalLight light = directionalLights.remove(id);
        return light != null;
    }

    public void setPosition(String objectId, float x, float y, float z) {
        if (manager != null && manager.findGameObject(objectId) != null) {

            GameObject go = manager.findGameObject(objectId);
            manager.setPosition(go, new Vector3(x, y, z));

            return;
        }


        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) {
            return;
        }

        instance.transform.setTranslation(x, y, z);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 worldTransform = body.getWorldTransform();

            Quaternion rotation = worldTransform.getRotation(new Quaternion());
            worldTransform.set(new Vector3(x, y, z), rotation);

            body.setWorldTransform(worldTransform);
            body.getMotionState().setWorldTransform(worldTransform);

            body.activate();
        }
    }

    public void setRotation(String objectId, float yaw, float pitch, float roll) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Quaternion newRotation = new Quaternion().setEulerAngles(yaw, pitch, roll);

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);
        instance.transform.set(position, newRotation, scale);

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();

            transform.getTranslation(position);

            transform.set(position, newRotation);

            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }

    public Quaternion getRotationQuaternion(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Quaternion q = new Quaternion();
            instance.transform.getRotation(q, true);
            return q;
        }
        return null;
    }

    public void setCameraPosition(float x, float y, float z) {
        camera.position.set(x, y, z);
        camera.update();
    }

    public void cameraLookAt(float x, float y, float z) {
        camera.lookAt(x, y, z);
        camera.update();
    }

    public void objectLookAt(String id, float x, float y, float z) {
        ModelInstance instance = sceneObjects.get(id);
        if (instance == null) return;

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        Vector3 scale = new Vector3();
        instance.transform.getScale(scale);

        instance.transform.setToLookAt(new Vector3(x, y, z), Vector3.Y);
        Quaternion newRotation = new Quaternion();
        instance.transform.getRotation(newRotation);

        instance.transform.set(position, newRotation, scale);

        btRigidBody body = physicsBodies.get(id);
        if (body != null) {
            com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
            transform.getTranslation(position);
            transform.set(position, newRotation);

            body.setWorldTransform(transform);
            body.getMotionState().setWorldTransform(transform);
            body.activate();
        }
    }

    /**
     * Устанавливает вращение камеры напрямую через углы Эйлера.
     * Этот метод полностью перезаписывает текущее направление камеры.
     * @param yaw Рыскание (поворот вокруг оси Y, "влево-вправо").
     * @param pitch Тангаж (поворот вокруг оси X, "вверх-вниз").
     * @param roll Крен (поворот вокруг оси Z, "наклон головы").
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        Quaternion rotation = new Quaternion();
        rotation.setEulerAngles(yaw, pitch, roll);

        camera.direction.set(0, 0, -1);
        rotation.transform(camera.direction);
        camera.up.set(0, 1, 0);
        rotation.transform(camera.up);

        camera.update();
    }

    public void setAmbientLight(float r, float g, float b) {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, r, g, b, 1f));
    }

    public void setDirectionalLight(String lightId, float r, float g, float b, float dirX, float dirY, float dirZ) {
        DirectionalLight light = directionalLights.get(lightId);
        if (light == null) {
            light = new DirectionalLight();
            directionalLights.put(lightId, light);
            environment.add(light);
        }
        light.set(r, g, b, dirX, dirY, dirZ);
    }

    public Camera getCamera() {
        return camera;
    }

    /**
     * Устанавливает масштаб объекта.
     * @param objectId ID объекта.
     * @param scaleX Масштаб по оси X.
     * @param scaleY Масштаб по оси Y.
     * @param scaleZ Масштаб по оси Z.
     */
    public void setScale(String objectId, float scaleX, float scaleY, float scaleZ) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null) return;

        Quaternion rotation = new Quaternion();
        instance.transform.getRotation(rotation);

        Vector3 position = new Vector3();
        instance.transform.getTranslation(position);
        instance.transform.set(position, rotation, new Vector3(scaleX, scaleY, scaleZ));

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            float mass = (body.getInvMass() > 0f) ? 1f / body.getInvMass() : 0f;
            float friction = body.getFriction();
            float restitution = body.getRestitution();
            Vector3 velocity = body.getLinearVelocity();
            PhysicsState state = (mass > 0f) ? PhysicsState.DYNAMIC : PhysicsState.STATIC;

            setPhysicsState(objectId, state, mass);

            btRigidBody newBody = physicsBodies.get(objectId);
            if (newBody != null) {
                com.badlogic.gdx.math.Matrix4 transform = newBody.getWorldTransform();
                transform.getTranslation(position);
                transform.set(position, rotation);
                newBody.setWorldTransform(transform);
                newBody.getMotionState().setWorldTransform(transform);

                newBody.setFriction(friction);
                newBody.setRestitution(restitution);
                if (state == PhysicsState.DYNAMIC) {
                    newBody.setLinearVelocity(velocity);
                }
            }
        }
    }

    /**
     * Создает и добавляет в сцену отладочную сетку.
     * @param size Размер сетки (например, 100).
     * @param divisions Количество линий (например, 100).
     */
    public void createGrid(float size, int divisions) {
        if (gridInstance != null) {
            sceneManager.removeScene(new net.mgsx.gltf.scene3d.scene.Scene(gridInstance));
            gridInstance.model.dispose();
        }

        com.badlogic.gdx.graphics.g3d.utils.ModelBuilder modelBuilder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        Model gridModel = modelBuilder.createLineGrid(divisions, divisions, size / divisions, size / divisions,
                new com.badlogic.gdx.graphics.g3d.Material(),
                com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.ColorUnpacked);

        gridInstance = new ModelInstance(gridModel);
        sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(gridInstance));
    }

    /**
     * Создает простой 3D-куб.
     * @param objectId Уникальный ID для нового объекта.
     * @return true в случае успеха, false если ID уже занят.
     */
    public boolean createCube(String objectId) {
        if (sceneObjects.containsKey(objectId)) return false;

        final String CUBE_MODEL_KEY = "__PRIMITIVE_CUBE__";
        Model cubeModel = loadedModels.get(CUBE_MODEL_KEY);

        if (cubeModel == null) {
            Material pbrMaterial = new Material(
                    PBRColorAttribute.createBaseColorFactor(Color.WHITE)
            );

            final long attributes = VertexAttributes.Usage.Position |
                    VertexAttributes.Usage.Normal |
                    VertexAttributes.Usage.TextureCoordinates |
                    VertexAttributes.Usage.Tangent;

            cubeModel = modelBuilder.createBox(50f, 50f, 50f,
                    pbrMaterial,
                    attributes);
            loadedModels.put(CUBE_MODEL_KEY, cubeModel);
        }

        ModelInstance instance = new ModelInstance(cubeModel);
        sceneObjects.put(objectId, instance);

        if (realisticMode && sceneManager != null) {
            sceneManager.addScene(new net.mgsx.gltf.scene3d.scene.Scene(instance));
        }

        return true;
    }

    public void applyPBRMaterial(String objectId, MaterialComponent materialData) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance == null || materialData == null) return;

        for (Material material : instance.materials) {
            PBRColorAttribute colorAttr = (PBRColorAttribute) material.get(PBRColorAttribute.BaseColorFactor);
            if (colorAttr != null) {
                colorAttr.color.set(materialData.baseColor);
            } else {
                material.set(PBRColorAttribute.createBaseColorFactor(materialData.baseColor));
            }

            if (materialData.baseColor.a < 1.0f) {
                material.set(new com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(
                        GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA
                ));
            } else {
                material.remove(com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute.Type);
            }

            applyTextureOrColor(material, materialData.baseColorTexturePath,
                    PBRTextureAttribute.BaseColorTexture, PBRColorAttribute.BaseColorFactor, materialData.baseColor);

            material.set(PBRFloatAttribute.createMetallic(materialData.metallic));
            material.set(PBRFloatAttribute.createRoughness(materialData.roughness));

            applyTexture(material, materialData.metallicRoughnessTexturePath, PBRTextureAttribute.MetallicRoughnessTexture);

            applyTexture(material, materialData.normalTexturePath, PBRTextureAttribute.NormalTexture);

            applyTexture(material, materialData.occlusionTexturePath, PBRTextureAttribute.OcclusionTexture);

            applyTextureOrColor(material, materialData.emissiveTexturePath,
                    PBRTextureAttribute.EmissiveTexture, PBRColorAttribute.Emissive, materialData.emissiveColor);
        }
    }

    private void applyTextureOrColor(Material material, String texturePath, long textureType, long colorType, Color color) {
        if (texturePath != null && !texturePath.isEmpty()) {
            Texture texture = loadTexture(texturePath);
            if (texture != null) {
                material.remove(colorType);
                material.set(new PBRTextureAttribute(textureType, texture));
            }
        } else {
            material.remove(textureType);
            material.set(new PBRColorAttribute(colorType, color));
        }
    }

    private void applyTexture(Material material, String texturePath, long textureType) {
        if (texturePath != null && !texturePath.isEmpty()) {
            Texture texture = loadTexture(texturePath);
            if (texture != null) {
                material.set(new PBRTextureAttribute(textureType, texture));
            }
        } else {
            material.remove(textureType);
        }
    }

    private Texture loadTexture(String textureFileName) {
        Texture texture = loadedTextures.get(textureFileName);
        if (texture == null) {
            try {
                File textureFileHandle = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject().getFile(textureFileName);
                if (textureFileHandle != null && textureFileHandle.exists()) {
                    texture = new Texture(Gdx.files.absolute(textureFileHandle.getAbsolutePath()));
                    texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                    loadedTextures.put(textureFileName, texture);
                } else {
                    Gdx.app.error("3DManager", "Texture file not found in project: " + textureFileName);
                    return null;
                }
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Could not load texture: " + textureFileName, e);
                return null;
            }
        }
        return texture;
    }

    /**
     * Возвращает текущую позицию объекта.
     * @param objectId ID объекта.
     * @return Vector3 с координатами (x, y, z) или null, если объект не найден.
     */
    public Vector3 getPosition(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 position = new Vector3();
            instance.transform.getTranslation(position);
            return position;
        }
        return null;
    }

    /**
     * Возвращает текущий поворот объекта в виде углов Эйлера (в градусах).
     * @param objectId ID объекта.
     * @return Vector3, где:
     *         x = наклон (pitch),
     *         y = рыскание (yaw),
     *         z = крен (roll).
     *         Возвращает null, если объект не найден.
     */
    public Vector3 getRotation(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Quaternion q = new Quaternion();
            instance.transform.getRotation(q);

            return new Vector3(q.getPitch(), q.getYaw(), q.getRoll());
        }
        return null;
    }

    /**
     * Возвращает текущий масштаб объекта.
     * @param objectId ID объекта.
     * @return Vector3 с масштабом по осям (x, y, z) или null, если объект не найден.
     */
    public Vector3 getScale(String objectId) {
        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            Vector3 scale = new Vector3();
            instance.transform.getScale(scale);
            return scale;
        }
        return null;
    }

    public Float getDistance(String id1, String id2) {
        if (getPosition(id1) == null || getPosition(id2) == null) return -1f;
        return getPosition(id1).dst(getPosition(id2));
    }

    /**
     * Задает коэффициент трения для физического объекта.
     * @param objectId ID объекта.
     * @param friction Коэффициент трения (обычно от 0.0 до 1.0).
     */


    /**
     * Задает коэффициент трения для физического объекта.
     * @param objectId ID объекта.
     * @param friction Коэффициент трения (обычно от 0.0 до 1.0).
     */
    public void setFriction(String objectId, float friction) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            if (friction > 9999) {
                body.setDamping(friction - 10000, friction - 10000);
            } else {
                body.setFriction(friction);
            }

            body.activate();
        }
    }

    /**
     * Задает коэффициент упругости (отскока) для физического объекта.
     * @param objectId ID объекта.
     * @param restitution Коэффициент упругости (0.0 - нет отскока, 1.0 - идеальный отскок).
     */
    public void setRestitution(String objectId, float restitution) {
        btRigidBody body = physicsBodies.get(objectId);
        if (body != null) {
            body.setRestitution(restitution);

            body.activate();
        }
    }

    /**
     * Включает или выключает Continuous Collision Detection (CCD) для объекта.
     * Помогает предотвратить "туннелирование" (проход сквозь стены).
     * @param objectId ID объекта.
     * @param enabled true - включить, false - выключить.
     */
    public void setContinuousCollisionDetection(String objectId, boolean enabled) {
        btRigidBody body = physicsBodies.get(objectId);
        ModelInstance instance = sceneObjects.get(objectId);
        if (body == null || instance == null || body.isStaticObject()) {
            return;
        }
        if (enabled) {
            BoundingBox bbox = new BoundingBox();
            instance.model.calculateBoundingBox(bbox);
            Vector3 dimensions = new Vector3();
            bbox.getDimensions(dimensions);
            float minSize = Math.min(Math.min(dimensions.x, dimensions.y), dimensions.z);
            body.setCcdMotionThreshold(minSize * 0.5f);
            body.setCcdSweptSphereRadius(minSize * 0.5f);
        } else {
            body.setCcdMotionThreshold(0);
            body.setCcdSweptSphereRadius(0);
        }
    }

    /**
     * Устанавливает кастомный GLSL шейдер из текстовых строк.
     * @param vertexCode   Строка с кодом вершинного шейдера.
     * @param fragmentCode Строка с кодом фрагментного шейдера.
     */
    public void setShaderCode(String vertexCode, String fragmentCode) {
        Gdx.app.postRunnable(() -> {
            Gdx.app.log("ShaderDebug", "--- setShaderCode CALLED ---");
            if (vertexCode == null || vertexCode.isEmpty() || fragmentCode == null || fragmentCode.isEmpty()) {
                resetSceneShader();
                return;
            }

            try {
                DefaultShader.Config config = new DefaultShader.Config(vertexCode, fragmentCode);

                ShaderProgram tempProgram = new ShaderProgram(vertexCode, fragmentCode);
                if (!tempProgram.isCompiled()) {
                    throw new Exception("Shader compilation failed: " + tempProgram.getLog());
                }
                tempProgram.dispose();

                if (customShaderProvider != null) customShaderProvider.dispose();
                if (modelBatch != null) modelBatch.dispose();

                customShaderProvider = new CustomShaderProvider(config, customUniforms);

                modelBatch = new ModelBatch(customShaderProvider);

                Gdx.app.log("ShaderDebug", "--- CustomShaderProvider and new ModelBatch CREATED successfully ---");
            } catch (Exception e) {
                Gdx.app.error("3DManager", "Shader setup failed: " + e.getMessage());
                resetSceneShader();
            }
        });
    }

    /**
     * Сбрасывает шейдер к стандартному.
     */
    public void resetSceneShader() {
        Gdx.app.postRunnable(() -> {
            if (customShaderProvider != null) {
                customShaderProvider.dispose();
                customShaderProvider = null;
            }

            if (modelBatch != null) modelBatch.dispose();

            customUniforms.clear();

            modelBatch = new ModelBatch(defaultShaderProvider);


        });
    }

    /**
     * Устанавливает uniform-переменную (float) по имени.
     */
    public void setShaderUniform(String name, float value) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }

    /**
     * Устанавливает uniform-переменную (vec3) по имени.
     */
    public void setShaderUniform(String name, float x, float y, float z) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector3(x, y, z));
        }
    }

    public void setShaderUniform(String name, int value) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, value);
        }
    }

    /**
     * Устанавливает uniform-переменную (vec2).
     */
    public void setShaderUniform(String name, float x, float y) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector2(x, y));
        }
    }

    /**
     * Устанавливает uniform-переменную (vec4 или цвет с альфа-каналом).
     */
    public void setShaderUniform(String name, float x, float y, float z, float w) {
        if (name != null && !name.isEmpty()) {
            customUniforms.put("u_" + name, new Vector4(x, y, z, w));
        }
    }

    public Vector3 getSunLightDirection() {
        if (pbrLight != null) {
            return pbrLight.direction;
        }
        return new Vector3(1, -1.5f, 1); 
    }

    public Color getSunLightColor() {
        if (pbrLight != null) {
            return pbrLight.color;
        }
        return Color.WHITE; 
    }

    public float getSunLightIntensity() {
        if (pbrLight != null) {
            return pbrLight.intensity;
        }
        return 0; 
    }

    public void setWorldTransform(String objectId, Matrix4 worldTransform) {
        if (LOG_THREED_MANAGER_DEBUG) {
            Vector3 receivedPos = new Vector3();
            Quaternion receivedRot = new Quaternion();
            worldTransform.getTranslation(receivedPos);
            worldTransform.getRotation(receivedRot, true);
            Log.d("TDM_DEBUG", "  >>> ThreeDManager.setWorldTransform() called for '" + objectId + "' <<<");
            Log.d("TDM_DEBUG", "    [SetWorldTrans] Received World Pos: " + receivedPos + ", Rot: " + receivedRot);
        }

        ModelInstance instance = sceneObjects.get(objectId);
        if (instance != null) {
            instance.transform.set(worldTransform);
            if (LOG_THREED_MANAGER_DEBUG) {
                Vector3 modelPos = new Vector3();
                instance.transform.getTranslation(modelPos);
                Log.d("TDM_DEBUG", "    [SetWorldTrans] ModelInstance '" + objectId + "' set to Pos: " + modelPos);
            }
        } else {
            if (LOG_THREED_MANAGER_DEBUG) Log.w("TDM_DEBUG", "    [SetWorldTrans] ModelInstance for '" + objectId + "' not found. Skipping model update.");
        }

        btRigidBody body = physicsBodies.get(objectId);
        if (body != null && !editorMode) {
            Matrix4 bodyTransform = body.getWorldTransform();
            Vector3 position = new Vector3();
            Quaternion rotation = new Quaternion();
            worldTransform.getTranslation(position);
            worldTransform.getRotation(rotation, true);

            bodyTransform.set(position, rotation);

            body.setWorldTransform(bodyTransform);
            if (body.getMotionState() != null) {
                body.getMotionState().setWorldTransform(bodyTransform);
            }
            body.activate();
            if (LOG_THREED_MANAGER_DEBUG) {
                Vector3 physicsPos = new Vector3();
                body.getWorldTransform().getTranslation(physicsPos);
                Log.d("TDM_DEBUG", "    [SetWorldTrans] Physics body '" + objectId + "' set to Pos: " + physicsPos);
            }
        } else {
            if (LOG_THREED_MANAGER_DEBUG && body == null) Log.w("TDM_DEBUG", "    [SetWorldTrans] Physics body for '" + objectId + "' not found. Skipping physics update.");
            else if (LOG_THREED_MANAGER_DEBUG && editorMode) Log.d("TDM_DEBUG", "    [SetWorldTrans] In editor mode, skipping physics update for '" + objectId + "'.");
        }
    }

    public void clearScene() {
        Array<String> objectIds = new Array<>();
        for (String id : sceneObjects.keySet()) {
            objectIds.add(id);
        }
        
        for (String id : objectIds) {
            removeObject(id);
        }
        sceneObjects.clear();

        Array<String> bodyIds = new Array<>();
        for (String id : physicsBodies.keySet()) {
            bodyIds.add(id);
        }
        for (String id : bodyIds) {
            removePhysicsBody(id);
        }
        physicsBodies.clear();
        physicsResources.clear();

        if (skybox != null) {
            skybox.dispose();
            skybox = null;
        }
        if (skyboxCubemap != null) {
            skyboxCubemap.dispose();
            skyboxCubemap = null;
        }
        if (diffuseCubemap != null) {
            diffuseCubemap.dispose();
            diffuseCubemap = null;
        }
        if (specularCubemap != null) {
            specularCubemap.dispose();
            specularCubemap = null;
        }
        if (gridInstance != null) {
            sceneManager.removeScene(new net.mgsx.gltf.scene3d.scene.Scene(gridInstance));
            gridInstance.model.dispose();
            gridInstance = null;
        }
        Array<String> constraintIds = new Array<>();
        for (String id : physicsConstraints.keySet()) {
            constraintIds.add(id);
        }
        for (String id : constraintIds) {
            removeConstraint(id);
        }


        pointLights.clear();
        editorProxies.clear();
        spotLights.clear();
        directionalLights.clear();
        if (sceneManager != null) {
            sceneManager.environment.clear();

            try {
                setupDefaultLighting();
            } catch (Exception e) {
                Log.e("ThreeDManager", "Cloudn't setup lightning: " + e);
            }
            
            /*sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
            try {
                updateProceduralIBL();
            } catch (Exception e) {
                Log.e("ThreeDManager", "Cloudn't update procedural ibl: " + e);
            }*/
        }

        animationControllers.clear();
        gltfObjectIds.clear();
        rayCastResults.clear();
    }


    @Override
    public void dispose() {
        clearScene();
        physicsConstraints.clear();

        if (modelBatch != null) modelBatch.dispose();
        if (shadowBatch != null) shadowBatch.dispose();

        for (Model model : loadedModels.values()) model.dispose();
        loadedModels.clear();
        for (Texture texture : loadedTextures.values()) texture.dispose();
        loadedTextures.clear();

        for (PlayableAudio audio : active3DSounds) {
            audio.stop();
            audio.dispose();
        }
        active3DSounds.clear();
        for (AudioAsset asset : loadedAudioAssets.values()) {
            asset.dispose();
        }
        loadedAudioAssets.clear();

        if (defaultShaderProvider != null) defaultShaderProvider.dispose();
        if (customShaderProvider != null) customShaderProvider.dispose();
        if (depthShaderProvider != null) depthShaderProvider.dispose();
        if (sceneManager != null) sceneManager.dispose();
        if (lightProxyModel != null) lightProxyModel.dispose();
        if (cameraProxyModel != null) cameraProxyModel.dispose();
        if (wireframeBoxModel != null) wireframeBoxModel.dispose();
        if (wireframeSphereModel != null) wireframeSphereModel.dispose();
        if (wireframeCylinderModel != null) wireframeCylinderModel.dispose();
        if (wireframeBatch != null) wireframeBatch.dispose();
        if (brdfLUT != null) brdfLUT.dispose();
        if (panoramicConverter != null) panoramicConverter.dispose();
        if (debugDrawer != null) debugDrawer.dispose();
        if (dynamicsWorld != null) dynamicsWorld.dispose();
        if (solver != null) solver.dispose();
        if (broadphase != null) broadphase.dispose();
        if (dispatcher != null) dispatcher.dispose();
        if (collisionConfig != null) collisionConfig.dispose();
        if (collisionCallback != null) collisionCallback.dispose();
        if (shadowFBO != null) shadowFBO.dispose();

        modelBatch = null;
        dynamicsWorld = null;
        sceneManager = null;
    }

    private class CollisionCallback extends com.badlogic.gdx.physics.bullet.collision.ContactResultCallback {
        public boolean collided = false;

        @Override
        public float addSingleResult(com.badlogic.gdx.physics.bullet.collision.btManifoldPoint cp,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj0Wrap, int partId0, int index0,
                                     com.badlogic.gdx.physics.bullet.collision.btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {
            collided = true;
            return 0;
        }
    }
}