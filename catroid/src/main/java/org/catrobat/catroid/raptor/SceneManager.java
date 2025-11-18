package org.catrobat.catroid.raptor;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.editor.EditorActivity;
import org.catrobat.catroid.pocketmusic.note.Project;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.LightComponent;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.RenderComponent;
import org.catrobat.catroid.raptor.TransformComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Высокоуровневый менеджер сцены. Оперирует GameObject'ами и Компонентами,
 * делегируя всю низкоуровневую работу классу ThreeDManager.
 * Это основная точка взаимодействия для редактора и системы загрузки сцен.
 */
public class SceneManager {

    public final ThreeDManager engine;
    private final Map<String, GameObject> gameObjects = new ConcurrentHashMap<>();
    private final BoundingBox tempBoundingBox = new BoundingBox();

    private boolean isEditorMode = false;

    private GameObject mainCameraObject = null;

    private boolean isPlaying = false;

    private final Vector3 tmpPos = new Vector3();
    private final Quaternion tmpRot = new Quaternion();
    private final Vector3 tmpScale = new Vector3();
    private final Matrix4 tmpMat1 = new Matrix4();
    private final Matrix4 tmpMat2 = new Matrix4();
    private final Matrix4 tmpMat3 = new Matrix4();

    public void setEditorMode(boolean isEditor) {
        this.isEditorMode = isEditor;
    }

    public void findAndSetMainCamera() {
        mainCameraObject = null;
        for (GameObject go : gameObjects.values()) {
            CameraComponent camComp = go.getComponent(CameraComponent.class);
            if (camComp != null && camComp.isMainCamera) {
                mainCameraObject = go;

                return;
            }
        }
    }


    public SceneManager(ThreeDManager lowLevelEngine) {
        this.engine = lowLevelEngine;
        this.engine.setSceneManager(this);
    }

    private void synchronizeTransformsFromEngine() {
        Matrix4 parentWorldInverse = new Matrix4();
        Matrix4 childWorldTransform = new Matrix4();


        for (GameObject go : gameObjects.values()) {


            btRigidBody body = engine.getPhysicsBody(go.id);




            if (body == null || body.isStaticObject()) {
                continue;
            }



            Matrix4 bodyTransform = body.getWorldTransform();
            Vector3 engineWorldPos = bodyTransform.getTranslation(new Vector3());
            Quaternion engineWorldRot = bodyTransform.getRotation(new Quaternion(), true);



            if (go.parentId == null) {

                go.transform.position.set(engineWorldPos);
                go.transform.rotation.set(engineWorldRot);
            } else {

                GameObject parent = findGameObject(go.parentId);
                if (parent != null) {

                    if (Math.abs(parent.transform.worldTransform.det()) < 0.000001f) {
                        continue;
                    }

                    parentWorldInverse.set(parent.transform.worldTransform).inv();
                    childWorldTransform.set(engineWorldPos, engineWorldRot, go.transform.scale);
                    Matrix4 newLocalTransform = parentWorldInverse.mul(childWorldTransform);

                    newLocalTransform.getTranslation(go.transform.position);
                    newLocalTransform.getRotation(go.transform.rotation, true);
                    newLocalTransform.getScale(go.transform.scale);
                }
            }
        }
    }

    public void update(float delta) {
        if (!isEditorMode) {
            synchronizeTransformsFromEngine();
        }


        updateWorldTransforms();


        for (GameObject go : gameObjects.values()) {
            applyTransformToEngine(go);
            if (go.hasComponent(LightComponent.class)) {
                applyLightAndTransform(go);
            }
        }

        if (mainCameraObject != null) {
            Matrix4 cameraWorldTransform = mainCameraObject.transform.worldTransform;
            Vector3 worldPos = cameraWorldTransform.getTranslation(new Vector3());
            Quaternion worldRot = cameraWorldTransform.getRotation(new Quaternion(), true);
            engine.setCameraPosition(worldPos.x, worldPos.y, worldPos.z);
            engine.setCameraRotation(worldRot);
        }
    }

    public void updateWorldTransforms() {
        for (GameObject go : gameObjects.values()) {
            if (go.parentId == null) {
                updateTransformRecursive(go, null);
            }
        }
    }

    private void updateTransformRecursive(GameObject current, GameObject parent) {

        current.transform.worldTransform.set(
                current.transform.position,
                current.transform.rotation,
                current.transform.scale
        );


        if (parent != null) {
            current.transform.worldTransform.mulLeft(parent.transform.worldTransform);
        }


        for (String childId : current.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                updateTransformRecursive(child, current);
            }
        }
    }

    private String generateUniqueName(String baseName) {
        String finalName = baseName.replaceAll(" \\(\\d+\\)$", "");
        if (!gameObjects.containsKey(finalName)) {
            return finalName;
        }
        int counter = 1;
        while (gameObjects.containsKey(finalName + " (" + counter + ")")) {
            counter++;
        }
        return finalName + " (" + counter + ")";
    }

    private void applyTransformToEngine(GameObject go) {
        if (go == null) return;


        Quaternion tempRotation = new Quaternion();
        go.transform.worldTransform.getRotation(tempRotation, true);


        if (Float.isNaN(tempRotation.x) || Float.isNaN(tempRotation.y) || Float.isNaN(tempRotation.z) || Float.isNaN(tempRotation.w)) {
            Log.e("SceneManager_Apply", "CRITICAL: NaN rotation detected for GameObject '" + go.id + "'. Aborting transform update for this frame to prevent engine corruption.");
            return;
        }



        engine.setWorldTransform(go.id, go.transform.worldTransform);
    }

    /*public void enterPlayMode() {
        this.isPlaying = true;
        for (GameObject go : gameObjects.values()) {
            List<ScriptComponent> scriptComponents = go.getComponents(ScriptComponent.class);
            for (ScriptComponent scriptComp : scriptComponents) {
                initializeScript(go, scriptComp);
            }
        }
    }

    public void exitPlayMode() {
        this.isPlaying = false;
        for (GameObject go : gameObjects.values()) {
            go.getComponents(ScriptComponent.class).forEach(sc -> sc.scriptInstance = null);
        }
    }

    private void initializeScript(GameObject go, ScriptComponent scriptComp) {
        if (scriptComp.scriptPath != null && !scriptComp.scriptPath.isEmpty()) {
            GameScript instance = ScriptLoader.INSTANCE.loadScript(scriptComp.scriptPath);
            if (instance != null) {
                scriptComp.scriptInstance = instance;
                instance.setGameObject(go);
                instance.setSceneManager(this);
                try {
                    instance.onInit();
                } catch (Exception e) {
                    Log.e("SceneManager", "Error in onInit() for script: " + scriptComp.scriptPath, e);
                }
            }
        }
    }*/

    public void setPosition(GameObject go, Vector3 position) {
        go.transform.position.set(position);
    }

    public void setRotation(GameObject go, Quaternion rotation) {
        go.transform.rotation.set(rotation);
    }

    public void setScale(GameObject go, Vector3 scale) {
        go.transform.scale.set(scale);
    }

    /**
     * Полностью очищает текущую сцену и загружает новую из файла.
     * Предназначен для использования из игрового движка.
     * @param fileHandle Файл сцены .rscene для загрузки.
     */
    public void loadAndReplaceScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            clearScene_internal();
            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
                return;
            }
            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);

            if (sceneData == null) { return; }
            setBackgroundLightIntensity(sceneData.ambientIntensity);
            setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);
            if (sceneData.gameObjects == null) { return; }


            for (GameObject go : sceneData.gameObjects) {
                gameObjects.put(go.id, go);
            }


            for (GameObject go : sceneData.gameObjects) {
                rebuildGameObject_internal(go);
            }

            findAndSetMainCamera();
        });
    }

    private void createRenderableForGameObject(GameObject go) {
        RenderComponent render = go.getComponent(RenderComponent.class);
        if (render != null && render.modelFileName != null && !render.modelFileName.isEmpty()) {
            String modelName = render.modelFileName;
            if (modelName.startsWith("primitive:")) {
                if (modelName.equals("primitive:cube")) engine.createCube(go.id);
                else if (modelName.equals("primitive:sphere")) engine.createSphere(go.id);
            } else {
                String absolutePath;
                if (modelName.startsWith("assets://")) {
                    absolutePath = modelName.substring("assets://".length());
                } else {
                    File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelName);
                    if (modelFile != null && modelFile.exists()) {
                        absolutePath = modelFile.getAbsolutePath();
                    } else {
                        Gdx.app.error("SceneManager", "Rebuild failed: Model file not found: " + modelName);
                        return;
                    }
                }
                if (!engine.createObject(go.id, absolutePath)) {
                    Gdx.app.error("SceneManager", "Rebuild failed: Could not create render object for " + go.id);
                }
            }
        }
    }

    private void rebuildComponentsForGameObject(GameObject go) {
        engine.setWorldTransform(go.id, go.transform.worldTransform);

        Log.d("PhysicsDebug", "Rebuilding components for '" + go.name + "' with final World Transform:\n" + go.transform.worldTransform);

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null) {
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, go.transform.worldTransform);
            } else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, go.transform.worldTransform);
            }
            engine.setFriction(go.id, physics.friction);
            engine.setRestitution(go.id, physics.restitution);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light != null) {
            engine.createEditorProxy(go.id);
            applyLightAndTransform(go);
        }

        CameraComponent camera = go.getComponent(CameraComponent.class);
        if (camera != null) {
            engine.createCameraProxy(go.id);
            if (camera.isMainCamera) {
                applyCameraComponentToEngine(go, camera);
            }
        }

        playAnimationFromComponent(go);

        MaterialComponent material = go.getComponent(MaterialComponent.class);
        if (material != null) {
            engine.applyPBRMaterial(go.id, material);
        }
    }


    /**
     * Создает новый пустой GameObject в сцене.
     * @param baseName Имя объекта для отображения в иерархии.
     * @return Созданный GameObject.
     */
    public GameObject createGameObject(String baseName) {
        String finalName = baseName;
        int counter = 1;

        while (gameObjects.containsKey(finalName)) {
            finalName = baseName + " (" + counter + ")";
            counter++;
        }

        GameObject go = new GameObject(finalName);
        gameObjects.put(go.id, go);
        return go;
    }

    /**
     * Безопасно переименовывает GameObject, обновляя его ID и все ссылки в движке.
     * @param go Объект для переименования.
     * @param newName Новое уникальное имя.
     * @return true, если переименование успешно.
     */
    public boolean renameGameObject(GameObject go, String newName) {
        if (go == null || newName == null || newName.isEmpty() || gameObjects.containsKey(newName)) {
            return false;
        }
        String oldId = go.id;

        if (oldId.equals(newName)) {
            return true;
        }

        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                parent.childrenIds.remove(oldId);
                parent.childrenIds.add(newName);
            }
        }

        for (String childId : go.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                child.parentId = newName;
            }
        }

        gameObjects.remove(oldId);

        go.id = newName;
        go.name = newName;

        gameObjects.put(go.id, go);

        engine.renameObject(oldId, newName);

        return true;
    }

    /**
     * Создает и возвращает точную копию существующего GameObject.
     * @param original Объект для дублирования.
     * @return Новый, добавленный в сцену GameObject, или null в случае ошибки.
     */
    public GameObject duplicateGameObject(GameObject original) {
        if (original == null) return null;

        List<GameObject> newObjects = new ArrayList<>();
        Map<String, String> oldIdToNewId = new HashMap<>();

        GameObject newRoot = cloneRecursive(original, newObjects, oldIdToNewId);

        for (GameObject copy : newObjects) {
            if (copy.parentId != null) {
                String newParentId = oldIdToNewId.get(copy.parentId);
                if (newParentId != null) {
                    copy.parentId = newParentId;
                }
            }
        }

        if (newRoot != null) {
            newRoot.parentId = original.parentId;
        }

        for (GameObject copy : newObjects) {
            gameObjects.put(copy.id, copy);
            rebuildGameObject_internal(copy);
        }

        if (newRoot != null && newRoot.parentId != null) {
            GameObject parent = findGameObject(newRoot.parentId);
            if (parent != null && !parent.childrenIds.contains(newRoot.id)) {
                parent.childrenIds.add(newRoot.id);
            }
        }

        updateWorldTransforms();
        return newRoot;
    }

    private GameObject cloneRecursive(GameObject original, List<GameObject> copies, Map<String, String> idMapping) {
        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        copy.name = generateUniqueName(original.name);
        copy.id = copy.name;

        copies.add(copy);
        idMapping.put(original.id, copy.id);

        List<String> originalChildrenIds = new ArrayList<>(copy.childrenIds);
        copy.childrenIds.clear();

        for (String childId : originalChildrenIds) {
            GameObject originalChild = findGameObject(childId);
            if (originalChild != null) {
                GameObject newChild = cloneRecursive(originalChild, copies, idMapping);
                copy.childrenIds.add(newChild.id);
            }
        }
        return copy;
    }

    private GameObject duplicateRecursive(GameObject original, Map<String, String> idMapping) {
        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        String newName = generateUniqueName(original.name);
        copy.name = newName;
        copy.id = newName;

        idMapping.put(original.id, copy.id);

        gameObjects.put(copy.id, copy);

        List<String> originalChildrenIds = new ArrayList<>(copy.childrenIds);
        copy.childrenIds.clear();

        for (String originalChildId : originalChildrenIds) {
            GameObject originalChild = findGameObject(originalChildId);
            if (originalChild != null) {
                GameObject newChild = duplicateRecursive(originalChild, idMapping);
                newChild.parentId = copy.id;
                copy.childrenIds.add(newChild.id);
            }
        }

        rebuildGameObject(copy);

        return copy;
    }

    /**
     * Находит ближайший объект, пересекаемый лучом,
     * используя геометрический тест (Bounding Box), а не физический.
     * Идеально подходит для выбора объектов в редакторе.
     * @param ray Луч, выпущенный из камеры.
     * @return Найденный GameObject или null.
     */
    public GameObject getObjectByRaycast(Ray ray) {
        List<GameObject> candidates = new ArrayList<>();
        Vector3 intersectionPoint = new Vector3();


        for (GameObject go : gameObjects.values()) {
            if (!go.hasComponent(RenderComponent.class)) continue;
            ModelInstance instance = engine.getModelInstance(go.id);
            if (instance == null) continue;

            instance.calculateBoundingBox(tempBoundingBox).mul(instance.transform);
            if (Intersector.intersectRayBounds(ray, tempBoundingBox, intersectionPoint)) {
                candidates.add(go);
            }
        }


        for (Map.Entry<String, ModelInstance> entry : engine.getEditorProxies().entrySet()) {
            String ownerId = entry.getKey();
            ModelInstance proxyInstance = entry.getValue();
            GameObject owner = findGameObject(ownerId);
            if (owner == null) continue;

            proxyInstance.transform.set(owner.transform.position, owner.transform.rotation);
            proxyInstance.calculateBoundingBox(tempBoundingBox).mul(proxyInstance.transform);

            if (Intersector.intersectRayBounds(ray, tempBoundingBox, intersectionPoint)) {
                candidates.add(owner);
            }
        }


        if (candidates.isEmpty()) {
            return null;
        }


        if (candidates.size() == 1) {
            return candidates.get(0);
        }


        GameObject bestCandidate = null;
        float smallestVolume = Float.MAX_VALUE;

        for (GameObject candidate : candidates) {
            ModelInstance instance = engine.getModelInstance(candidate.id);

            if (instance == null) {
                if (candidate.hasComponent(LightComponent.class) || candidate.hasComponent(CameraComponent.class)) {
                    instance = engine.getEditorProxies().get(candidate.id);
                }
            }
            if (instance == null) continue;

            instance.calculateBoundingBox(tempBoundingBox);
            float volume = tempBoundingBox.getWidth() * tempBoundingBox.getHeight() * tempBoundingBox.getDepth();

            if (volume < smallestVolume) {
                smallestVolume = volume;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    /**
     * Безопасно применяет относительный поворот к объекту, избегая проблем
     * с неравномерным масштабированием.
     * @param go Объект для вращения.
     * @param deltaRotation Кватернион, представляющий поворот, который нужно добавить.
     */
    public void rotate(GameObject go, Quaternion deltaRotation) {
        if (go == null) return;

        TransformComponent transform = go.transform;

        Vector3 originalScale = transform.scale.cpy();

        engine.setScale(go.id, 1, 1, 1);

        transform.rotation.mulLeft(deltaRotation);
        engine.setRotation(go.id, transform.rotation);

        engine.setScale(go.id, originalScale.x, originalScale.y, originalScale.z);

        transform.scale.set(originalScale);

        applyLightAndTransform(go);
    }

    /**
     * Собирает текущее состояние сцены в объект SceneData.
     * @return объект SceneData, готовый к кэшированию или сохранению.
     */
    public SceneData getCurrentSceneData() {
        SceneData sceneData = new SceneData();
        for (GameObject go : gameObjects.values()) {
            sceneData.gameObjects.add(go);
        }
        sceneData.skyR = this.skyR;
        sceneData.skyG = this.skyG;
        sceneData.skyB = this.skyB;
        sceneData.ambientIntensity = this.ambientIntensity;
        return sceneData;
    }

    /**
     * Загружает сцену из объекта SceneData. Используется для восстановления из кэша.
     */
    public void loadSceneFromData(SceneData sceneData) {
        if (sceneData == null) return;

        clearScene_internal();

        setBackgroundLightIntensity(sceneData.ambientIntensity);
        setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);

        if (sceneData.gameObjects == null) return;

        for (GameObject go : sceneData.gameObjects) {
            gameObjects.put(go.id, go);
            rebuildGameObject_internal(go);
        }
        findAndSetMainCamera();

        Gdx.app.log("SceneManager", "Scene loaded from cache.");
    }

    public void removeGameObject(GameObject go) {
        if (go == null) return;

        List<String> childrenIdsCopy = new ArrayList<>(go.childrenIds);
        for (String childId : childrenIdsCopy) {
            removeGameObject(findGameObject(childId));
        }

        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                parent.childrenIds.remove(go.id);
            }
        }

        gameObjects.remove(go.id);
        engine.removeObject(go.id);
        if (go.hasComponent(LightComponent.class)) {
            engine.removePBRLight(go.id);
            engine.removeEditorProxy(go.id);
        }
    }

    public GameObject findGameObject(String id) {
        return gameObjects.get(id);
    }

    public Map<String, GameObject> getAllGameObjects() {
        return gameObjects;
    }



    /**
     * Добавляет или обновляет RenderComponent, создавая видимую 3D-модель.
     */
    public void setRenderComponent(GameObject go, String modelFileName) {
        Gdx.app.postRunnable(() -> {
            File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelFileName);
            if (modelFile == null || !modelFile.exists()) {
                Gdx.app.error("SceneManager", "Model file not found in project: " + modelFileName);
            }
            String absolutePath = modelFile.getAbsolutePath();

            if (go.hasComponent(RenderComponent.class)) {
                engine.removeObject(go.id);
            }
            RenderComponent render = go.getComponent(RenderComponent.class);

            if (render == null) {
                render = new RenderComponent();
                go.addComponent(render);
            }
            render.modelFileName = modelFileName;

            boolean success = engine.createObject(go.id, absolutePath);
            if (!success) { return; }

            applyTransform(go);

            if (go.hasComponent(PhysicsComponent.class)) {
                PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
                setPhysicsComponent(go, physics.state, physics.shape, physics.mass);
            }
        });
    }

    public void setAnimationComponent(GameObject go, AnimationComponent animComponent) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof AnimationComponent);

        if (animComponent != null) {
            Log.d("SceneManager", "Is added: " + go.addComponent(animComponent));
        }

        playAnimationFromComponent(go);
    }


    /**
     * Устанавливает простое физическое состояние объекта.
     * ЭТОТ МЕТОД ПРЕДНАЗНАЧЕН ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ С БЛОКАМИ.
     * Он полностью перезаписывает любые сложные настройки коллайдеров,
     * заменяя их одним примитивом (Box, Sphere, Capsule).
     *
     * @param go    GameObject для изменения.
     * @param state Тип физического тела (Static, Dynamic и т.д.).
     * @param shape Форма единственного коллайдера.
     * @param mass  Масса (используется только для Dynamic).
     */
    public void setPhysicsComponent(GameObject go, ThreeDManager.PhysicsState state, ThreeDManager.PhysicsShape shape, float mass) {
        Gdx.app.postRunnable(() -> {
            PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
            if (physics == null) {
                physics = new PhysicsComponent();
                go.addComponent(physics);
            }

            physics.state = state;
            physics.mass = mass;
            physics.colliders.clear();
            if (state == ThreeDManager.PhysicsState.STATIC || state == ThreeDManager.PhysicsState.DYNAMIC) {
                ColliderShapeData singleCollider = new ColliderShapeData();
                switch (shape) {
                    case SPHERE:
                        singleCollider.type = ColliderShapeData.ShapeType.SPHERE;
                        break;
                    case CAPSULE:
                        singleCollider.type = ColliderShapeData.ShapeType.CAPSULE;
                        break;
                    case BOX:
                    default:
                        singleCollider.type = ColliderShapeData.ShapeType.BOX;
                        break;
                }



                physics.colliders.add(singleCollider);
            }



            updateWorldTransforms();


            engine.setPhysicsState(go.id, state, shape, mass, go.transform.worldTransform);
        });
    }

    /**
     * Обновляет физическое состояние объекта, используя данные напрямую из компонента.
     * Предназначен для использования из редактора, где компонент уже содержит
     * все нужные данные (включая список коллайдеров).
     * @param go        GameObject для обновления.
     * @param component Полностью настроенный PhysicsComponent.
     */
    public void setPhysicsComponent(GameObject go, PhysicsComponent component) {
        Gdx.app.postRunnable(() -> {
            go.components.removeIf(c -> c instanceof PhysicsComponent);
            go.addComponent(component);


            if (component.colliders != null && !component.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, component);
            } else {
                updateWorldTransforms();


                engine.setPhysicsState(go.id, component.state, component.shape, component.mass, go.transform.worldTransform);
            }
        });
    }

    /**
     * Добавляет или обновляет LightComponent.
     */
    public void setLightComponent(GameObject go, LightComponent lightData) {
        Gdx.app.postRunnable(() -> {
            boolean wasLightBefore = go.hasComponent(LightComponent.class);
            go.components.removeIf(c -> c instanceof LightComponent);
            go.addComponent(lightData);

            if (!wasLightBefore) {
                engine.createEditorProxy(go.id);
            }

            applyLightAndTransform(go);
        });
    }

    public void setCameraComponent(GameObject go, CameraComponent cameraData) {
        Gdx.app.postRunnable(() -> {
            boolean wasCameraBefore = go.hasComponent(CameraComponent.class);
            go.components.removeIf(c -> c instanceof CameraComponent);
            go.addComponent(cameraData);

            if (!wasCameraBefore) {
                engine.createCameraProxy(go.id);
            }


            if (cameraData.isMainCamera) {
                applyCameraComponentToEngine(go, cameraData);
            }

            findAndSetMainCamera();
        });
    }


    private void applyCameraComponentToEngine(GameObject go, CameraComponent camComp) {
        engine.setCameraFov(camComp.fieldOfView, camComp.nearPlane, camComp.farPlane);
        engine.setCameraPosition(go.transform.position.x, go.transform.position.y, go.transform.position.z);
        engine.setCameraRotation(go.transform.rotation);
    }



    /*public void setPosition(GameObject go, Vector3 position) {
        go.transform.position.set(position);
        applyLightAndTransform(go);
    }

    public void setRotation(GameObject go, Quaternion rotation) {
        go.transform.rotation.set(rotation);
        applyLightAndTransform(go);
    }

    public void setScale(GameObject go, Vector3 scale) {
        go.transform.scale.set(scale);
        applyTransform(go);
    }*/



    public void playAnimation(GameObject go, String animationName, int loops, float speed, float transitionTime) {
        engine.playAnimation(go.id, animationName, loops, speed, transitionTime);
    }

    public void stopAnimation(GameObject go) {
        engine.stopAnimation(go.id);
    }



    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        engine.castRay(rayName, from, direction);
    }

    public GameObject getRaycastHitObject(String rayName) {
        String hitId = engine.getRaycastHitObjectId(rayName);
        if (hitId != null && !hitId.isEmpty()) {
            return findGameObject(hitId);
        }
        return null;
    }

    public float getRaycastDistance(String rayName) {
        return engine.getRaycastDistance(rayName);
    }

    public float skyR = 0.1f;
    public float skyG = 0.2f;
    public float skyB = 0.3f;
    public float ambientIntensity = 1f;

    public void setBackgroundLightIntensity(float ambientIntensity) {
        this.ambientIntensity = ambientIntensity;
        engine.setBackgroundLightIntensity(ambientIntensity);
    }

    public void setSkyColor(float r, float g, float b) {
        skyR = r;
        skyG = g;
        skyB = b;
        engine.setSkyColor(skyR, skyG, skyB);
    }

    /**
     * Находит ПЕРВЫЙ GameObject с указанным именем.
     * Этот метод следует использовать в игровых скриптах для поиска объектов.
     * Поиск не чувствителен к регистру.
     * @param name Имя объекта, заданное в редакторе.
     * @return GameObject или null, если объект с таким именем не найден.
     */
    public GameObject findObjectByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                return go;
            }
        }
        return null;
    }

    /**
     * Находит ВСЕ GameObject'ы с указанным именем.
     * Полезно для поиска групп объектов (например, всех врагов с именем "Goblin").
     * @param name Имя объектов.
     * @return Список (может быть пустым) всех найденных GameObjects.
     */
    public List<GameObject> findObjectsByName(String name) {
        List<GameObject> foundObjects = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            return foundObjects;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                foundObjects.add(go);
            }
        }
        return foundObjects;
    }



    private final Json json = new Json();

    /**
     * Сохраняет текущее состояние сцены в файл.
     * @param fileHandle Файл для сохранения (например, Gdx.files.local("scenes/level1.json")).
     */
    public void saveScene(FileHandle fileHandle) {
        SceneData sceneData = new SceneData();
        for (GameObject go : gameObjects.values()) {
            sceneData.gameObjects.add(go);
        }
        sceneData.skyR = skyR;
        sceneData.skyG = skyG;
        sceneData.skyB = skyB;
        sceneData.ambientIntensity = ambientIntensity;
        sceneData.renderSettings = engine.getSceneSettings();

        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);

        String sceneJson = json.prettyPrint(sceneData);

        fileHandle.writeString(sceneJson, false);
        Gdx.app.log("SceneManager", "Scene saved to " + fileHandle.path());
    }

    /**
     * Загружает сцену из файла, полностью заменяя текущую.
     * @param fileHandle Файл для загрузки.
     */
    public void loadScene(FileHandle fileHandle) {
        if (fileHandle == null || !fileHandle.exists()) {
            Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
            return;
        }

        String sceneJson = fileHandle.readString();
        json.setUsePrototypes(false);
        SceneData sceneData = json.fromJson(SceneData.class, sceneJson);


        setBackgroundLightIntensity(sceneData.ambientIntensity);
        setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);

        if (sceneData == null || sceneData.gameObjects == null) { return; }

        for (GameObject go : sceneData.gameObjects) {
            gameObjects.put(go.id, go);
            rebuildGameObject(go);
        }

        findAndSetMainCamera();

        Gdx.app.log("SceneManager", "Scene build commands issued.");
    }

    private void rebuildGameObject_internal(GameObject go) {
        Log.d("PhysicsDebug", "============================================================");
        Log.d("PhysicsDebug", "=== Rebuilding GameObject: '" + go.name + "' (ID: " + go.id + ")");
        Log.d("PhysicsDebug", "============================================================");

        RenderComponent render = go.getComponent(RenderComponent.class);
        if (render != null && render.modelFileName != null && !render.modelFileName.isEmpty()) {
            String modelName = render.modelFileName;
            if (modelName.startsWith("primitive:")) {
                if (modelName.equals("primitive:cube")) {
                    engine.createCube(go.id);
                } else if (modelName.equals("primitive:sphere")) {
                    engine.createSphere(go.id);
                }
            } else {
                String absolutePath;
                if (modelName.startsWith("assets://")) {
                    absolutePath = modelName.substring("assets://".length());
                } else {
                    File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelName);
                    if (modelFile != null && modelFile.exists()) {
                        absolutePath = modelFile.getAbsolutePath();
                    } else {
                        Gdx.app.error("SceneManager", "Rebuild failed: Model file not found: " + modelName);
                        return;
                    }
                }
                if (!engine.createObject(go.id, absolutePath)) {
                    Gdx.app.error("SceneManager", "Rebuild failed: Could not create render object for " + go.id);
                    return;
                }
            }
        }

        updateWorldTransforms();

        engine.setWorldTransform(go.id, go.transform.worldTransform);

        Log.d("PhysicsDebug", "Transform for '" + go.name + "':");
        Log.d("PhysicsDebug", "  - Local Position: " + go.transform.position);
        Log.d("PhysicsDebug", "  - Local Rotation: " + go.transform.rotation);
        Log.d("PhysicsDebug", "  - Local Scale:    " + go.transform.scale);
        Log.d("PhysicsDebug", "  - World Transform Matrix:\n" + go.transform.worldTransform);
        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                Log.d("PhysicsDebug", "  - Parent ('"+parent.name+"') World Transform:\n" + parent.transform.worldTransform);
            }
        }

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null) {
            Matrix4 physicsTransform = new Matrix4(go.transform.worldTransform);

            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                Log.d("PhysicsDebug", "MESH_STATIC detected. Using rotation/scale only for physics baking.");
                physicsTransform.setTranslation(0, 0, 0);
            }
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, physicsTransform);
            }
            else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }

            engine.setFriction(go.id, physics.friction);
            engine.setRestitution(go.id, physics.restitution);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light != null) {
            engine.createEditorProxy(go.id);
            applyLightAndTransform(go);
        }

        CameraComponent camera = go.getComponent(CameraComponent.class);
        if (camera != null) {
            engine.createCameraProxy(go.id);
            if (camera.isMainCamera) {
                Gdx.app.log("SceneManager", "MainCamera found: " + go.name + ". Applying its transform.");
                applyCameraComponentToEngine(go, camera);
            }
        }

        playAnimationFromComponent(go);

        MaterialComponent material = go.getComponent(MaterialComponent.class);
        if (material != null) {
            engine.applyPBRMaterial(go.id, material);
        }

        /*List<ScriptComponent> scriptComponents = go.getComponents(ScriptComponent.class);
        for (ScriptComponent scriptComp : scriptComponents) {
            if (scriptComp.scriptPath != null && !scriptComp.scriptPath.isEmpty()) {
                GameScript instance = ScriptLoader.INSTANCE.loadScript(scriptComp.scriptPath);
                if (instance != null) {
                    scriptComp.scriptInstance = instance;
                    instance.setGameObject(go);
                    instance.setSceneManager(this);

                    try {
                        instance.onInit();
                    } catch (Exception e) {
                        Log.e("SceneManager", "Error in onInit() for script: " + scriptComp.scriptPath, e);
                    }
                }
            }
        }*/
    }

    public void repositionObjectAndChildren(GameObject root, Vector3 newWorldPosition) {
        if (root == null) return;

        Vector3 currentWorldPos = root.transform.worldTransform.getTranslation(new Vector3());
        Vector3 delta = new Vector3(newWorldPosition).sub(currentWorldPos);

        repositionRecursive(root, delta);

        updateWorldTransforms();
    }

    private void applyTransformToEngineRecursive(GameObject go) {
        if (go == null) return;

        engine.setWorldTransform(go.id, go.transform.worldTransform);

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {

            Log.d("SceneManager", "Re-creating static physics body for '" + go.name + "' after move.");

            engine.removePhysicsBody(go.id);

            Matrix4 physicsTransform = new Matrix4(go.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }

            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : go.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                applyTransformToEngineRecursive(child);
            }
        }
    }

    private void repositionRecursive(GameObject current, Vector3 delta) {
        current.transform.position.add(delta);

        updateTransformRecursive(current, findGameObject(current.parentId));

        engine.setWorldTransform(current.id, current.transform.worldTransform);

        PhysicsComponent physics = current.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {
            engine.removePhysicsBody(current.id);

            Matrix4 physicsTransform = new Matrix4(current.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }

            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(current.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(current.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : current.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                repositionRecursive_Child(child, current);
            }
        }
    }

    private void repositionRecursive_Child(GameObject current, GameObject newParent) {
        updateTransformRecursive(current, newParent);
        engine.setWorldTransform(current.id, current.transform.worldTransform);

        PhysicsComponent physics = current.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {
            engine.removePhysicsBody(current.id);
            Matrix4 physicsTransform = new Matrix4(current.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(current.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(current.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : current.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                repositionRecursive_Child(child, current);
            }
        }
    }


    public void rebuildGameObject(GameObject go) {
        Gdx.app.postRunnable(() -> rebuildGameObject_internal(go));
    }

    private void clearScene_internal() {
        engine.clearScene();
        gameObjects.clear();
    }

    /**
     * Полностью очищает текущую сцену, готовя ее к новой загрузке.
     */
    public void clearScene() {
        Gdx.app.postRunnable(this::clearScene_internal);
    }

    public void playAnimationFromComponent(GameObject go) {
        AnimationComponent anim = go.getComponent(AnimationComponent.class);
        if (anim == null || anim.animationName == null) {
            Gdx.app.postRunnable(() -> engine.stopAnimation(go.id));
            return;
        }
        Gdx.app.postRunnable(() -> {
            engine.playAnimation(go.id, anim.animationName, anim.loops, anim.speed, anim.transitionTime);
        });
    }

    public Array<String> getAnimationNames(GameObject go) {
        return engine.getAnimationNames(go.id);
    }



    /**
     * Применяет все трансформации из TransformComponent к объекту в движке.
     */
    private void applyTransform(GameObject go) {
        if (go == null) return;
        TransformComponent t = go.transform;


        if (!engine.objectExists(go.id)) return;

        engine.setPosition(go.id, t.position.x, t.position.y, t.position.z);

        engine.setRotation(go.id, t.rotation);

        engine.setScale(go.id, t.scale.x, t.scale.y, t.scale.z);
    }

    /**
     * Удаляет RenderComponent с объекта.
     * Это делает объект невидимым, но сохраняет его в иерархии.
     */
    public void removeRenderComponent(GameObject go) {
        if (go == null || !go.hasComponent(RenderComponent.class)) return;

        go.components.removeIf(c -> c instanceof RenderComponent);
        engine.removeObject(go.id);
    }

    /**
     * Удаляет PhysicsComponent с объекта.
     */
    public void removePhysicsComponent(GameObject go) {
        if (go == null || !go.hasComponent(PhysicsComponent.class)) return;

        go.components.removeIf(c -> c instanceof PhysicsComponent);
        engine.removePhysicsBody(go.id);
    }

    /**
     * Удаляет LightComponent с объекта.
     */
    public void removeLightComponent(GameObject go) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof LightComponent);
        engine.removePBRLight(go.id);
        engine.removeEditorProxy(go.id);
    }


    /**
     * Применяет параметры света и трансформации.
     */
    private void applyLightAndTransform(GameObject go) {
        if (go == null) return;




        if (go.hasComponent(LightComponent.class)) {

            Vector3 worldPosition = go.transform.worldTransform.getTranslation(new Vector3());
            engine.updateEditorProxyPosition(go.id, worldPosition);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light == null) return;

        if (!isObjectActiveInHierarchy(go)) {
            engine.removePBRLight(go.id);
            return;
        }


        Vector3 pos = go.transform.worldTransform.getTranslation(new Vector3());
        Quaternion rot = go.transform.worldTransform.getRotation(new Quaternion(), true);
        float r = light.color.r, g = light.color.g, b = light.color.b;

        switch (light.type) {
            case SPOT:
                Vector3 spotDir = new Vector3(0, 0, -1);
                rot.transform(spotDir);
                engine.setSpotLight(go.id, pos.x, pos.y, pos.z, spotDir.x, spotDir.y, spotDir.z,
                        r, g, b, light.intensity, light.cutoffAngle, light.exponent, light.range);
                break;
            case POINT:
                engine.setPointLight(go.id, pos.x, pos.y, pos.z, r, g, b, light.intensity, light.range);
                break;
            case DIRECTIONAL:
                Vector3 sunDir = new Vector3(0, 0, -1);
                rot.transform(sunDir);
                engine.setRealisticSunLight(sunDir.x, sunDir.y, sunDir.z, light.intensity);
                engine.setSunLightColor(r, g, b);
                light.direction.set(sunDir);
                break;
        }
    }

    public void setRestitution(String id, float restitution) {
        engine.setRestitution(id, restitution);
    }

    public void setFriction(String id, float friction) {
        engine.setFriction(id, friction);
    }

    public Json getJson() {
        return json;
    }

    public GameObject createPrimitive(String type) {
        String baseName = type.substring(0, 1).toUpperCase() + type.substring(1);
        GameObject go = createGameObject(baseName);

        boolean success = true;
        /*if ("cube".equals(type)) {
            success = engine.createCube(go.id);
        } else if ("sphere".equals(type)) {
            success = engine.createSphere(go.id);
        }*/

        if (success) {
            RenderComponent render = new RenderComponent();
            render.modelFileName = "primitive:" + type;
            go.addComponent(render);

            MaterialComponent material = new MaterialComponent();
            go.addComponent(material);

            go.transform.scale = new Vector3(0.1f, 0.1f, 0.1f);

            applyTransform(go);

            engine.applyPBRMaterial(go.id, material);

            rebuildGameObject(go);

            return go;
        } else {
            gameObjects.remove(go.id);
            return null;
        }
    }

    public void setMaterialComponent(GameObject go, MaterialComponent component) {
        Gdx.app.postRunnable(() -> {
            go.components.removeIf(c -> c instanceof MaterialComponent);
            go.addComponent(component);
            engine.applyPBRMaterial(go.id, component);
        });
    }

    public boolean isObjectActiveInHierarchy(GameObject go) {
        if (go == null) return false;
        if (!go.isActive) return false;

        if (go.parentId != null) {
            return isObjectActiveInHierarchy(findGameObject(go.parentId));
        }

        return true;
    }

    public void setObjectActive(GameObject go, boolean active) {
        if (go == null || go.isActive == active) return;
        go.isActive = active;

        updateVisibilityRecursive(go);

        updateLightStateRecursive(go);
    }

    private void updateLightStateRecursive(GameObject go) {
        if (go == null) return;

        if (go.hasComponent(LightComponent.class)) {
            applyLightAndTransform(go);
        }

        for (String childId : go.childrenIds) {
            updateLightStateRecursive(findGameObject(childId));
        }
    }

    private void updateVisibilityRecursive(GameObject go) {
        if (go == null) return;

        boolean shouldBeVisible = isObjectActiveInHierarchy(go);

        engine.setObjectVisibility(go.id, shouldBeVisible);

        for (String childId : go.childrenIds) {
            updateVisibilityRecursive(findGameObject(childId));
        }
    }

    public void setParent(GameObject child, GameObject parent) {
        Gdx.app.postRunnable(() -> {
            setParentInternal(child, parent);
        });
    }

    public void removeParent(GameObject child) {
        Gdx.app.postRunnable(() -> {
            setParentInternal(child, null);
        });
    }

    public void setParentInternal(GameObject child, GameObject parent) {
        if (child == null || child == parent) return;

        if (parent != null && isDescendant(parent, child)) {
            Log.e("SceneManager", "Cannot set parent: would create a cycle in hierarchy.");
            return;
        }

        updateWorldTransforms();
        Matrix4 childWorldTransform = new Matrix4(child.transform.worldTransform);

        if (child.parentId != null) {
            GameObject oldParent = findGameObject(child.parentId);
            if (oldParent != null) {
                oldParent.childrenIds.remove(child.id);
            }
        }
        child.parentId = null;

        if (parent != null) {
            child.parentId = parent.id;
            if (!parent.childrenIds.contains(child.id)) {
                parent.childrenIds.add(child.id);
            }
            Matrix4 parentWorldTransform = parent.transform.worldTransform;
            if (Math.abs(parentWorldTransform.det()) < 0.000001f) {
                Log.e("SceneManager", "Cannot attach to a parent with zero scale.");
                return;
            }
            Matrix4 parentWorldInverse = new Matrix4(parentWorldTransform).inv();
            Matrix4 newLocalTransform = parentWorldInverse.mul(childWorldTransform);
            newLocalTransform.getTranslation(child.transform.position);
            newLocalTransform.getRotation(child.transform.rotation, true);
            newLocalTransform.getScale(child.transform.scale);
        } else {
            childWorldTransform.getTranslation(child.transform.position);
            childWorldTransform.getRotation(child.transform.rotation, true);
            childWorldTransform.getScale(child.transform.scale);
        }
        updateWorldTransforms();
    }

    private boolean isDescendant(GameObject potentialDescendant, GameObject ancestor) {
        if (potentialDescendant.parentId == null) {
            return false;
        }
        if (potentialDescendant.parentId.equals(ancestor.id)) {
            return true;
        }
        GameObject nextParent = findGameObject(potentialDescendant.parentId);
        if (nextParent == null) {
            return false;
        }
        return isDescendant(nextParent, ancestor);
    }

    public GameObject cloneGameObject(GameObject original, String newName) {
        if (original == null || newName == null || newName.isEmpty() || gameObjects.containsKey(newName)) {
            return null;
        }

        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        copy.name = newName;
        copy.id = newName;

        copy.parentId = null;
        copy.childrenIds.clear();

        gameObjects.put(copy.id, copy);
        rebuildGameObject(copy);

        return copy;
    }


    public void loadAndAddScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Additive scene file not found.");
                return;
            }
            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);
            if (sceneData == null || sceneData.gameObjects == null) { return; }

            List<GameObject> newObjects = new ArrayList<>();
            Map<String, String> oldIdToNewId = new HashMap<>();


            for (GameObject go : sceneData.gameObjects) {
                String originalId = go.id;
                String newId = generateUniqueName(originalId);
                go.id = newId;
                go.name = newId;
                newObjects.add(go);
                oldIdToNewId.put(originalId, newId);
            }

            for (GameObject go : newObjects) {
                if (go.parentId != null) go.parentId = oldIdToNewId.get(go.parentId);
                ArrayList<String> newChildrenIds = new ArrayList<>();
                for (String oldChildId : go.childrenIds) {
                    String newChildId = oldIdToNewId.get(oldChildId);
                    if (newChildId != null) newChildrenIds.add(newChildId);
                }
                go.childrenIds = newChildrenIds;
            }

            for (GameObject go : newObjects) {
                gameObjects.put(go.id, go);
            }

            for (GameObject go : newObjects) {
                rebuildGameObject_internal(go);
            }
        });
    }
}