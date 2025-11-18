package org.catrobat.catroid.editor;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.raptor.AnimationComponent;
import org.catrobat.catroid.raptor.CameraComponent;
import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.LightComponent;
import org.catrobat.catroid.raptor.MaterialComponent;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.RenderComponent;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ScriptComponent;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.raptor.TransformComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InspectorManager {

    private final EditorActivity activity;
    private final LayoutInflater inflater;
    private SceneManager sceneManager;
    private final LinearLayout container;
    private final TextView inspectorTitle;

    private GameObject selectedObject;
    private ColliderShapeData selectedCollider = null;
    private Gizmo gizmo;

    private final ThreeDManager threeDManager;

    public InspectorManager(EditorActivity activity, SceneManager sceneManager, ThreeDManager threeDManager) {
        this.activity = activity;
        this.inflater = activity.getLayoutInflater();
        this.sceneManager = sceneManager;
        this.threeDManager = threeDManager;
        this.container = activity.findViewById(R.id.inspector_container);
        this.inspectorTitle = activity.findViewById(R.id.inspector_title);
    }

    public void setGizmo(Gizmo gizmo) {
        this.gizmo = gizmo;
    }

    public ColliderShapeData getSelectedCollider() {
        return selectedCollider;
    }

    public void updateSceneManager(SceneManager newManager) {
        this.sceneManager = newManager;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    public void populateInspector(GameObject go) {
        this.selectedObject = go;
        container.removeAllViews();

        if (go == null) {
            inspectorTitle.setText("Inspector");
            return;
        }

        inspectorTitle.setText(go.name);

        View headerView = inflater.inflate(R.layout.inspector_active_toggle, container, false);

        CheckBox activeCheckbox = headerView.findViewById(R.id.checkbox_is_active);
        activeCheckbox.setChecked(go.isActive);
        activeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (selectedObject != null) {
                sceneManager.setObjectActive(selectedObject, isChecked);
                activity.updateHierarchy(); // Обновляем иерархию для "серого" вида
            }
        });

        EditText nameEditor = headerView.findViewById(R.id.edit_object_name);
        nameEditor.setText(go.name);
        nameEditor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && selectedObject != null) {
                String newName = nameEditor.getText().toString();
                String oldName = selectedObject.name;

                if (newName.equals(oldName)) return;

                if (sceneManager.renameGameObject(selectedObject, newName)) {
                    // Имя обновится в иерархии после этого вызова
                    activity.updateHierarchy();
                } else {
                    Toast.makeText(activity, "Invalid or duplicate name!", Toast.LENGTH_SHORT).show();
                    nameEditor.setText(oldName);
                }
            }
        });

        container.addView(headerView);

        createTransformView(go);
        if (go.hasComponent(RenderComponent.class)) createRenderView(go);
        if (go.hasComponent(PhysicsComponent.class)) createPhysicsView(go);
        if (go.hasComponent(LightComponent.class)) createLightView(go);
        if (go.hasComponent(AnimationComponent.class)) createAnimationView(go);
        if (go.hasComponent(CameraComponent.class)) createCameraView(go);
        if (go.hasComponent(MaterialComponent.class)) createMaterialView(go);
        List<ScriptComponent> scripts = go.getComponents(ScriptComponent.class);
        for (ScriptComponent script : scripts) {
            createScriptView(go, script);
        }

        View footerView = inflater.inflate(R.layout.inspector_footer, container, false);
        footerView.findViewById(R.id.btn_add_component).setOnClickListener(v -> showAddComponentDialog(go));
        container.addView(footerView);

        View divider = new View(activity);
        int marginPx = (int) (16 * activity.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(0, marginPx, 0, marginPx);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(0x40FFFFFF);
        container.addView(divider);

        Button duplicateButton = new Button(activity, null, 0, R.style.Widget_App_Button_Outlined);
        duplicateButton.setText("Duplicate Object");
        duplicateButton.setOnClickListener(v -> {
            GameObject newObject = sceneManager.duplicateGameObject(go);
            if (newObject != null) {
                activity.onObjectSelected(newObject);
                activity.updateHierarchy();
            }
        });
        container.addView(duplicateButton);


        Button deleteObjectButton = new Button(activity);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        deleteParams.setMargins(0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0, 0);
        deleteObjectButton.setLayoutParams(deleteParams);

        deleteObjectButton.setText("Delete GameObject");
        deleteObjectButton.setTextColor(Color.parseColor("#FF5252"));

        deleteObjectButton.setOnClickListener(v -> {
            new AlertDialog.Builder(activity)
                    .setTitle("Delete Object")
                    .setMessage("Are you sure you want to delete '" + go.name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        sceneManager.removeGameObject(go);
                        activity.onObjectSelected(null);
                        activity.updateHierarchy();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        container.addView(deleteObjectButton);
    }

    private void setWhiteTextToAllChildren(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(0xFFFFFFFF);
                ((TextView) child).setHintTextColor(0xFFCCCCCC);
            } else if (child instanceof ViewGroup) {
                setWhiteTextToAllChildren((ViewGroup) child);
            }
        }
    }

    private int libGdxColorToAndroidColor(com.badlogic.gdx.graphics.Color gdxColor) {
        int r = (int)(gdxColor.r * 255);
        int g = (int)(gdxColor.g * 255);
        int b = (int)(gdxColor.b * 255);
        int a = (int)(gdxColor.a * 255);
        return android.graphics.Color.argb(a, r, g, b);
    }

    private void createAnimationView(GameObject go) {
        addComponentHeader("Animation", true,false, () -> {
            go.components.removeIf(c -> c instanceof AnimationComponent);
            sceneManager.playAnimationFromComponent(go);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_animation, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        Spinner animSpinner = view.findViewById(R.id.spinner_animations);
        EditText speedEditor = view.findViewById(R.id.edit_anim_speed);
        EditText transitionEditor = view.findViewById(R.id.edit_anim_transition);
        CheckBox loopCheckbox = view.findViewById(R.id.checkbox_anim_loop);

        Array<String> animationNamesGdx = sceneManager.getAnimationNames(go);
        List<String> animationNames = new ArrayList<>();
        animationNames.add("None");
        for(String name : animationNamesGdx) { animationNames.add(name); }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, animationNames);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        animSpinner.setAdapter(adapter);

        AnimationComponent animComponent = go.getComponent(AnimationComponent.class);
        if (animComponent == null) {
            animComponent = new AnimationComponent();
            go.addComponent(animComponent);
        }

        if (animComponent.animationName != null) {
            int selection = animationNames.indexOf(animComponent.animationName);
            animSpinner.setSelection(Math.max(0, selection));
        } else {
            animSpinner.setSelection(0);
        }
        speedEditor.setText(String.valueOf(animComponent.speed));
        transitionEditor.setText(String.valueOf(animComponent.transitionTime));
        loopCheckbox.setChecked(animComponent.loops == -1);

        final AnimationComponent finalAnimComponent = animComponent;

        animSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                finalAnimComponent.animationName = selected.equals("None") ? null : selected;
                sceneManager.playAnimationFromComponent(go);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        addSimpleTextListener(speedEditor, s -> {
            try { finalAnimComponent.speed = Float.parseFloat(s); } catch (Exception e) {}
            sceneManager.playAnimationFromComponent(go);
        });

        addSimpleTextListener(transitionEditor, s -> {
            try { finalAnimComponent.transitionTime = Float.parseFloat(s); } catch (Exception e) {}
            sceneManager.playAnimationFromComponent(go);
        });

        loopCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            finalAnimComponent.loops = isChecked ? -1 : 1;
            sceneManager.playAnimationFromComponent(go);
        });

        container.addView(view);
    }

    private void createCameraView(GameObject go) {
        addComponentHeader("Camera Component", true, false, new Runnable() {
            @Override
            public void run() {
                go.components.removeIf(c -> c instanceof CameraComponent);
                sceneManager.engine.removeEditorProxy(go.id);
                populateInspector(go);
            }
        });

        View view = inflater.inflate(R.layout.inspector_camera, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        CameraComponent camComp = go.getComponent(CameraComponent.class);

        CheckBox mainCamCheck = view.findViewById(R.id.check_main_camera);
        EditText fovEditor = view.findViewById(R.id.edit_cam_fov);
        EditText nearEditor = view.findViewById(R.id.edit_cam_near);
        EditText farEditor = view.findViewById(R.id.edit_cam_far);

        mainCamCheck.setChecked(camComp.isMainCamera);
        fovEditor.setText(String.valueOf(camComp.fieldOfView));
        nearEditor.setText(String.valueOf(camComp.nearPlane));
        farEditor.setText(String.valueOf(camComp.farPlane));

        mainCamCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                for (GameObject otherGo : sceneManager.getAllGameObjects().values()) {
                    CameraComponent otherCam = otherGo.getComponent(CameraComponent.class);
                    if (otherGo != go && otherCam != null) {
                        otherCam.isMainCamera = false;
                    }
                }
            }
            camComp.isMainCamera = isChecked;
            sceneManager.setCameraComponent(go, camComp);
            if(isChecked) populateInspector(go);
        });

        TextWatcher watcher = new DelayedTextWatcher(() -> {
            try {
                camComp.fieldOfView = Float.parseFloat(fovEditor.getText().toString());
                camComp.nearPlane = Float.parseFloat(nearEditor.getText().toString());
                camComp.farPlane = Float.parseFloat(farEditor.getText().toString());
                sceneManager.setCameraComponent(go, camComp);
            } catch (Exception e) {}
        });

        fovEditor.addTextChangedListener(watcher);
        nearEditor.addTextChangedListener(watcher);
        farEditor.addTextChangedListener(watcher);

        container.addView(view);
    }

    private void createTransformView(GameObject go) {
        addComponentHeader("Transform", false, false, null);
        View view = inflater.inflate(R.layout.inspector_transform, container, false);
        if (view instanceof ViewGroup) {
            setWhiteTextToAllChildren((ViewGroup) view);
        }

        ImageButton alignPosBtn = view.findViewById(R.id.btn_align_position_with_view);
        ImageButton alignRotBtn = view.findViewById(R.id.btn_align_rotation_with_view);

        alignPosBtn.setOnClickListener(v -> {
            if (threeDManager != null && selectedObject != null) {
                Vector3 cameraPos = threeDManager.getCameraPosition();
                selectedObject.transform.position.set(cameraPos);
                populateInspector(selectedObject);
            }
        });

        alignRotBtn.setOnClickListener(v -> {
            if (threeDManager != null && selectedObject != null) {
                Quaternion cameraRot = new Quaternion();
                threeDManager.getCamera().view.getRotation(cameraRot, true).conjugate();

                selectedObject.transform.rotation.set(cameraRot);
                populateInspector(selectedObject);
            }
        });

        TransformComponent t = go.transform;

        updateVector3Fields(view, R.id.edit_pos_x, R.id.edit_pos_y, R.id.edit_pos_z, t.position);
        updateEulerFields(view, R.id.edit_rot_x, R.id.edit_rot_y, R.id.edit_rot_z, t.rotation);
        updateVector3Fields(view, R.id.edit_scale_x, R.id.edit_scale_y, R.id.edit_scale_z, t.scale);

        addVector3Listener(go, view, R.id.edit_pos_x, R.id.edit_pos_y, R.id.edit_pos_z, sceneManager::setPosition);
        addEulerListener(go, view, R.id.edit_rot_x, R.id.edit_rot_y, R.id.edit_rot_z, sceneManager::setRotation);
        addVector3Listener(go, view, R.id.edit_scale_x, R.id.edit_scale_y, R.id.edit_scale_z, sceneManager::setScale);

        container.addView(view);
    }

    private void createRenderView(GameObject go) {
        addComponentHeader("Render Component", true, false, () -> {
            sceneManager.removeRenderComponent(go);
            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_render, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        RenderComponent render = go.getComponent(RenderComponent.class);
        TextView pathText = view.findViewById(R.id.text_model_path);
        Button selectButton = view.findViewById(R.id.btn_select_model);

        String modelName = render.modelFileName != null ? render.modelFileName : "No model selected";

        if (modelName.startsWith("primitive:")) {
            String shapeName = modelName.substring("primitive:".length());
            shapeName = shapeName.substring(0, 1).toUpperCase() + shapeName.substring(1);
            pathText.setText("Primitive Shape (" + shapeName + ")");
            selectButton.setVisibility(View.GONE);
        }
        else {
            pathText.setText(modelName);
            selectButton.setOnClickListener(v -> showModelPicker(go));
        }

        container.addView(view);
    }

    private void createMaterialView(GameObject go) {
        addComponentHeader("Material", true, false, () -> {
            go.components.removeIf(c -> c instanceof MaterialComponent);

            if (go.hasComponent(RenderComponent.class)) {
                threeDManager.applyPBRMaterial(go.id, new MaterialComponent());
            }

            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_material, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        MaterialComponent material = go.getComponent(MaterialComponent.class);


        setupColorPicker(go, view.findViewById(R.id.btn_material_color), material.baseColor,
                newColor -> material.baseColor.set(newColor));
        setupTextureSlot(go, view.findViewById(R.id.slot_base_color_texture), material.baseColorTexturePath,
                newPath -> material.baseColorTexturePath = newPath);


        setupSlider(go, view.findViewById(R.id.slider_metallic), material.metallic,
                newValue -> material.metallic = newValue);
        setupSlider(go, view.findViewById(R.id.slider_roughness), material.roughness,
                newValue -> material.roughness = newValue);


        setupColorPicker(go, view.findViewById(R.id.btn_emissive_color), material.emissiveColor,
                newColor -> material.emissiveColor.set(newColor));
        setupTextureSlot(go, view.findViewById(R.id.slot_emissive_texture), material.emissiveTexturePath,
                newPath -> material.emissiveTexturePath = newPath);


        setupTextureSlot(go, view.findViewById(R.id.slot_normal_texture), material.normalTexturePath,
                newPath -> material.normalTexturePath = newPath);
        setupTextureSlot(go, view.findViewById(R.id.slot_mr_texture), material.metallicRoughnessTexturePath,
                newPath -> material.metallicRoughnessTexturePath = newPath);
        setupTextureSlot(go, view.findViewById(R.id.slot_occlusion_texture), material.occlusionTexturePath,
                newPath -> material.occlusionTexturePath = newPath);

        container.addView(view);
    }



    private void showModelPicker(GameObject go) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();

        if (allFiles == null) {
            Toast.makeText(activity, "Could not read project files.", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<File> modelFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".obj") || name.endsWith(".gltf") || name.endsWith(".glb")) {
                modelFiles.add(file);
            }
        }

        if (modelFiles.isEmpty()) {
            Toast.makeText(activity, "No 3D models (.obj, .gltf, .glb) found in project files.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] modelNames = new String[modelFiles.size()];
        for (int i = 0; i < modelFiles.size(); i++) {
            modelNames[i] = modelFiles.get(i).getName();
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select a 3D Model")
                .setItems(modelNames, (dialog, which) -> {
                    String selectedFileName = modelNames[which];
                    sceneManager.setRenderComponent(go, selectedFileName);
                    populateInspector(go);
                })
                .show();
    }

    private void createPhysicsView(GameObject go) {
        addComponentHeader("Physics Component", true, true, () -> {
            go.components.removeIf(c -> c instanceof PhysicsComponent);
            sceneManager.removePhysicsComponent(go);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_physics, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);

        Spinner stateSpinner = view.findViewById(R.id.spinner_physics_state);
        View massLayout = view.findViewById(R.id.layout_physics_mass);
        String[] states = activity.getResources().getStringArray(R.array.brick_physics_states_full);
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, states);
        stateAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setSelection(physics.state.ordinal());

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                physics.state = ThreeDManager.PhysicsState.values()[position];
                massLayout.setVisibility(physics.state == ThreeDManager.PhysicsState.DYNAMIC ? View.VISIBLE : View.GONE);
                sceneManager.setPhysicsComponent(go, physics);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout collidersContainer = view.findViewById(R.id.colliders_container);
        collidersContainer.removeAllViews();
        for (ColliderShapeData collider : physics.colliders) {
            View colliderItemView = inflater.inflate(R.layout.inspector_collider_item, collidersContainer, false);
            populateColliderItemView(go, colliderItemView, collider);
            collidersContainer.addView(colliderItemView);
        }

        view.findViewById(R.id.btn_add_box_collider).setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = ColliderShapeData.ShapeType.BOX;
            newCollider.size.set(1, 1, 1);
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });
        /*view.findViewById(R.id.btn_add_sphere_collider).setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = ColliderShapeData.ShapeType.SPHERE;
            newCollider.radius = 0.5f;
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });*/
        view.findViewById(R.id.btn_add_capsule_collider).setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = ColliderShapeData.ShapeType.CAPSULE;
            newCollider.radius = 0.5f;
            newCollider.size.set(0.5f, 2.0f, 0.5f);
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });

        EditText massEditor = view.findViewById(R.id.edit_physics_mass);
        EditText frictionEditor = view.findViewById(R.id.edit_physics_friction);
        EditText restitutionEditor = view.findViewById(R.id.edit_physics_restitution);

        massLayout.setVisibility(physics.state == ThreeDManager.PhysicsState.DYNAMIC ? View.VISIBLE : View.GONE);
        massEditor.setText(String.valueOf(physics.mass));
        frictionEditor.setText(String.valueOf(physics.friction));
        restitutionEditor.setText(String.valueOf(physics.restitution));

        addSimpleTextListener(massEditor, s -> { try { physics.mass = Float.parseFloat(s); sceneManager.setPhysicsComponent(go, physics); } catch (Exception e) {} });
        addSimpleTextListener(frictionEditor, s -> { try { physics.friction = Float.parseFloat(s); sceneManager.setFriction(go.id, physics.friction); } catch (Exception e) {} });
        addSimpleTextListener(restitutionEditor, s -> { try { physics.restitution = Float.parseFloat(s); sceneManager.setRestitution(go.id, physics.restitution); } catch (Exception e) {} });

        container.addView(view);
    }

    public void setSelectedCollider(ColliderShapeData collider) {
        this.selectedCollider = collider;
    }

    private void populateColliderItemView(GameObject go, View itemView, ColliderShapeData collider) {
        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);

        TextView title = itemView.findViewById(R.id.collider_title);
        View contentLayout = itemView.findViewById(R.id.collider_content_layout);
        ImageButton duplicateButton = itemView.findViewById(R.id.btn_duplicate_collider);
        ImageButton deleteButton = itemView.findViewById(R.id.btn_delete_collider);

        EditText cx = itemView.findViewById(R.id.edit_collider_cx);
        EditText cy = itemView.findViewById(R.id.edit_collider_cy);
        EditText cz = itemView.findViewById(R.id.edit_collider_cz);

        View sizeLayout = itemView.findViewById(R.id.layout_collider_size);
        EditText sx = itemView.findViewById(R.id.edit_collider_sx);
        EditText sy = itemView.findViewById(R.id.edit_collider_sy);
        EditText sz = itemView.findViewById(R.id.edit_collider_sz);
        TextView labelSx = itemView.findViewById(R.id.label_collider_sx);
        TextView labelSz = itemView.findViewById(R.id.label_collider_sz);

        View radiusLayout = itemView.findViewById(R.id.layout_collider_radius);
        EditText radiusEditor = itemView.findViewById(R.id.edit_collider_radius);

        contentLayout.setOnClickListener(v -> {
            selectedCollider = collider;
            if (gizmo != null) {
                gizmo.setSelected(go, collider);
            }
            populateInspector(go);
        });

        if (collider == selectedCollider) {
            itemView.setBackgroundColor(0x559999FF);
        } else {
            itemView.setBackgroundColor(0xFF404040);
        }

        duplicateButton.setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = collider.type;
            newCollider.centerOffset.set(collider.centerOffset).add(0.1f, 0, 0);
            newCollider.size.set(collider.size);
            newCollider.radius = collider.radius;
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });

        deleteButton.setOnClickListener(v -> {
            physics.colliders.remove(collider);
            if (selectedCollider == collider) {
                selectedCollider = null;
                gizmo.setSelected(go, null);
            }
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });
        title.setText(collider.type.toString() + " Collider");
        cx.setText(String.format(Locale.US, "%.3f", collider.centerOffset.x));
        cy.setText(String.format(Locale.US, "%.3f", collider.centerOffset.y));
        cz.setText(String.format(Locale.US, "%.3f", collider.centerOffset.z));

        if (collider.type == ColliderShapeData.ShapeType.BOX) {
            sizeLayout.setVisibility(View.VISIBLE);
            radiusLayout.setVisibility(View.GONE);
            sx.setVisibility(View.VISIBLE); sx.setText(String.format(Locale.US, "%.3f", collider.size.x));
            sy.setVisibility(View.VISIBLE); sy.setText(String.format(Locale.US, "%.3f", collider.size.y));
            sz.setVisibility(View.VISIBLE); sz.setText(String.format(Locale.US, "%.3f", collider.size.z));
            labelSx.setVisibility(View.VISIBLE);
            labelSz.setVisibility(View.VISIBLE);
        } else if (collider.type == ColliderShapeData.ShapeType.SPHERE) {
            sizeLayout.setVisibility(View.GONE);
            radiusLayout.setVisibility(View.VISIBLE);
            radiusEditor.setText(String.format(Locale.US, "%.3", collider.radius));
        } else {
            sizeLayout.setVisibility(View.VISIBLE);
            radiusLayout.setVisibility(View.VISIBLE);
            radiusEditor.setText(String.format(Locale.US, "%.3f", collider.radius));
            sy.setText(String.format(Locale.US, "%.3f", collider.size.y));
            sx.setVisibility(View.GONE);
            sz.setVisibility(View.GONE);
            labelSx.setVisibility(View.GONE);
            labelSz.setVisibility(View.GONE);
        }

        Runnable updateAction = () -> {
            try {
                collider.centerOffset.set(
                        Float.parseFloat(cx.getText().toString()),
                        Float.parseFloat(cy.getText().toString()),
                        Float.parseFloat(cz.getText().toString())
                );
                switch (collider.type) {
                    case BOX:
                        collider.size.set(
                                Float.parseFloat(sx.getText().toString()),
                                Float.parseFloat(sy.getText().toString()),
                                Float.parseFloat(sz.getText().toString())
                        );
                        break;
                    case SPHERE:
                        collider.radius = Float.parseFloat(radiusEditor.getText().toString());
                        break;
                    case CAPSULE:
                        collider.radius = Float.parseFloat(radiusEditor.getText().toString());
                        collider.size.y = Float.parseFloat(sy.getText().toString());
                        break;
                }
                sceneManager.setPhysicsComponent(go, physics);
            } catch (NumberFormatException e) {}
        };

        TextWatcher watcher = new DelayedTextWatcher(updateAction);
        cx.addTextChangedListener(watcher);
        cy.addTextChangedListener(watcher);
        cz.addTextChangedListener(watcher);
        sx.addTextChangedListener(watcher);
        sy.addTextChangedListener(watcher);
        sz.addTextChangedListener(watcher);
        radiusEditor.addTextChangedListener(watcher);
    }

    private void createLightView(GameObject go) {
        addComponentHeader("Light Component", true, false, () -> {
            go.components.removeIf(c -> c instanceof LightComponent);
            sceneManager.removeLightComponent(go);
            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_light, container, false);
        if (view instanceof ViewGroup) {
            setWhiteTextToAllChildren((ViewGroup) view);
        }
        LightComponent light = go.getComponent(LightComponent.class);

        Spinner typeSpinner = view.findViewById(R.id.spinner_light_type);
        EditText intensityEditor = view.findViewById(R.id.edit_light_intensity);
        EditText rangeEditor = view.findViewById(R.id.edit_light_range);
        EditText angleEditor = view.findViewById(R.id.edit_light_angle);
        View rangeLayout = view.findViewById(R.id.layout_light_range);
        View spotLayout = view.findViewById(R.id.layout_light_spot);
        EditText exponentEditor = view.findViewById(R.id.edit_light_exponent);
        Button colorButton = view.findViewById(R.id.btn_light_color);

        ArrayAdapter<LightComponent.LightType> typeAdapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, LightComponent.LightType.values());
        typeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        typeSpinner.setAdapter(typeAdapter);

        exponentEditor.setText(String.valueOf(light.exponent));

        typeSpinner.setSelection(light.type.ordinal());
        intensityEditor.setText(String.valueOf(light.intensity));
        rangeEditor.setText(String.valueOf(light.range));
        angleEditor.setText(String.valueOf(light.cutoffAngle));

        Runnable updateVisibility = () -> {
            LightComponent.LightType type = (LightComponent.LightType) typeSpinner.getSelectedItem();
            rangeLayout.setVisibility(type == LightComponent.LightType.POINT || type == LightComponent.LightType.SPOT ? View.VISIBLE : View.GONE);
            spotLayout.setVisibility(type == LightComponent.LightType.SPOT ? View.VISIBLE : View.GONE);
        };
        updateVisibility.run();

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                light.type = LightComponent.LightType.values()[position];
                sceneManager.setLightComponent(go, light);
                updateVisibility.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        final int initialAndroidColor = libGdxColorToAndroidColor(light.color);
        colorButton.setBackgroundColor(initialAndroidColor);

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose color")
                    .initialColor(initialAndroidColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);

                        light.color.set(r / 255f, g / 255f, b / 255f, 1.0f);

                        sceneManager.setLightComponent(go, light);})
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .build()
                    .show();
        });

        addSimpleTextListener(exponentEditor, s -> {
            try {
                light.exponent = Float.parseFloat(s);
                sceneManager.setLightComponent(go, light);
            } catch (Exception e) {}
        });


        addSimpleTextListener(intensityEditor, s -> { try { light.intensity = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });
        addSimpleTextListener(rangeEditor, s -> { try { light.range = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });
        addSimpleTextListener(angleEditor, s -> { try { light.cutoffAngle = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });

        container.addView(view);
    }

    private void createScriptView(GameObject go, ScriptComponent scriptComp) {
        addComponentHeader("Script Component", true, false, () -> {
            go.components.remove(scriptComp);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_script, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        TextView pathText = view.findViewById(R.id.text_script_path);
        Button selectButton = view.findViewById(R.id.btn_select_script);

        if (scriptComp.scriptPath != null && !scriptComp.scriptPath.isEmpty()) {
            pathText.setText(scriptComp.scriptPath);
        } else {
            pathText.setText("None (Select a script)");
        }

        selectButton.setOnClickListener(v -> showScriptPicker(scriptComp));

        container.addView(view);
    }

    private void showScriptPicker(ScriptComponent scriptComp) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        if (!projectFilesDir.exists()) {
            projectFilesDir.mkdirs();
        }

        File[] allFiles = projectFilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".kts"));

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(activity, "No script files (.kts) found in 'scripts' folder.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] scriptNames = new String[allFiles.length];
        for (int i = 0; i < allFiles.length; i++) {
            scriptNames[i] = allFiles[i].getName();
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select a Script")
                .setItems(scriptNames, (dialog, which) -> {
                    String selectedScriptPath = scriptNames[which];
                    scriptComp.scriptPath = selectedScriptPath;
                    populateInspector(selectedObject);
                })
                .show();
    }

    private void showAddComponentDialog(GameObject go) {
        String[] components = {"Render", "Physics", "Light", "Animation", "Camera", "Material"};
        new AlertDialog.Builder(activity)
                .setTitle("Add Component")
                .setItems(components, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (!go.hasComponent(RenderComponent.class)) {
                                sceneManager.setRenderComponent(go, "myModel.glb");
                            }
                            break;
                        case 1:
                            if (!go.hasComponent(PhysicsComponent.class)) {
                                sceneManager.setPhysicsComponent(go, ThreeDManager.PhysicsState.STATIC, ThreeDManager.PhysicsShape.BOX, 1.0f);
                            }
                            break;
                        case 2:
                            if (!go.hasComponent(LightComponent.class)) {
                                sceneManager.setLightComponent(go, new LightComponent());
                            }
                            break;
                        case 3:
                            if (!go.hasComponent(AnimationComponent.class)) {
                                sceneManager.setAnimationComponent(go, new AnimationComponent());
                            }
                            break;
                        case 4:
                            if (!go.hasComponent(CameraComponent.class)) {
                                sceneManager.setCameraComponent(go, new CameraComponent());
                            }
                            break;
                        case 5:
                            if (!go.hasComponent(MaterialComponent.class)) {
                                sceneManager.setMaterialComponent(go, new MaterialComponent());
                            }
                            break;
                    }
                    populateInspector(go);
                })
                .show();
    }

    private boolean isShowingColliders = false;

    private void addComponentHeader(String title, boolean canBeDeleted, boolean hasVisibilityToggle, Runnable onDelete) {
        View headerView = inflater.inflate(R.layout.inspector_component_header, container, false);
        TextView titleView = headerView.findViewById(R.id.header_title);
        ImageButton deleteButton = headerView.findViewById(R.id.btn_delete_component);
        ImageButton visibilityButton = headerView.findViewById(R.id.btn_toggle_visibility);

        titleView.setText(title);

        if (hasVisibilityToggle) {
            visibilityButton.setVisibility(View.VISIBLE);
            visibilityButton.setOnClickListener(v -> {
                EditorFragment fragment = (EditorFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment != null && fragment.getListener() != null) {
                    isShowingColliders = !isShowingColliders;
                    fragment.getListener().setColliderVisibility(isShowingColliders);
                    visibilityButton.setAlpha(isShowingColliders ? 1.0f : 0.5f);
                }
            });
        }

        titleView.setText(title);
        if (canBeDeleted) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> onDelete.run());
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        container.addView(headerView);
    }

    private interface Vector3Updater { void update(GameObject go, Vector3 vec); }
    private interface QuaternionUpdater { void update(GameObject go, Quaternion quat); }

    private void addVector3Listener(GameObject go, View parent, int xId, int yId, int zId, Vector3Updater updater) {
        EditText x = parent.findViewById(xId);
        EditText y = parent.findViewById(yId);
        EditText z = parent.findViewById(zId);
        TextWatcher watcher = new DelayedTextWatcher(() -> {
            if (selectedObject == null || !x.hasFocus() && !y.hasFocus() && !z.hasFocus()) return;
            try {
                Vector3 v = new Vector3(
                        Float.parseFloat(x.getText().toString()),
                        Float.parseFloat(y.getText().toString()),
                        Float.parseFloat(z.getText().toString())
                );
                updater.update(go, v);
            } catch (NumberFormatException e) {}
        });
        x.addTextChangedListener(watcher);
        y.addTextChangedListener(watcher);
        z.addTextChangedListener(watcher);
    }

    private void addEulerListener(GameObject go, View parent, int pId, int yId, int rId, QuaternionUpdater updater) {
        EditText p = parent.findViewById(pId);
        EditText y = parent.findViewById(yId);
        EditText r = parent.findViewById(rId);
        TextWatcher watcher = new DelayedTextWatcher(() -> {
            if (selectedObject == null || !p.hasFocus() && !y.hasFocus() && !r.hasFocus()) return;
            try {
                Quaternion q = new Quaternion().setEulerAngles(
                        Float.parseFloat(y.getText().toString()),
                        Float.parseFloat(p.getText().toString()),
                        Float.parseFloat(r.getText().toString())
                );
                updater.update(go, q);
            } catch (NumberFormatException e) {}
        });
        p.addTextChangedListener(watcher);
        y.addTextChangedListener(watcher);
        r.addTextChangedListener(watcher);
    }

    private void updateVector3Fields(View parent, int xId, int yId, int zId, Vector3 vec) {
        ((EditText)parent.findViewById(xId)).setText(String.format(Locale.US, "%.3f", vec.x));
        ((EditText)parent.findViewById(yId)).setText(String.format(Locale.US, "%.3f", vec.y));
        ((EditText)parent.findViewById(zId)).setText(String.format(Locale.US, "%.3f", vec.z));
    }

    private void updateEulerFields(View parent, int pId, int yId, int rId, Quaternion q) {
        ((EditText)parent.findViewById(pId)).setText(String.format(Locale.US, "%.1f", q.getPitch()));
        ((EditText)parent.findViewById(yId)).setText(String.format(Locale.US, "%.1f", q.getYaw()));
        ((EditText)parent.findViewById(rId)).setText(String.format(Locale.US, "%.1f", q.getRoll()));
    }

    private void addSimpleTextListener(EditText editText, StringUpdater updater) {
        editText.addTextChangedListener(new DelayedTextWatcher(() -> {
            if (editText.hasFocus()) {
                updater.update(editText.getText().toString());
            }
        }));
    }

    private interface StringUpdater { void update(String value); }

    private static class DelayedTextWatcher implements TextWatcher {
        private final Runnable action;
        public DelayedTextWatcher(Runnable action) { this.action = action; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { action.run(); }
    }

    private interface FloatConsumer { void accept(float value); }
    private interface StringConsumer { void accept(String value); }
    private interface ColorConsumer { void accept(com.badlogic.gdx.graphics.Color value); }

    private void setupSlider(GameObject go, View sliderLayout, float initialValue, FloatConsumer onUpdate) {
        SeekBar seekBar = sliderLayout.findViewById(R.id.seekbar);
        TextView valueText = sliderLayout.findViewById(R.id.text_value);

        seekBar.setProgress((int)(initialValue * 100));
        valueText.setText(String.format(Locale.US, "%.2f", initialValue));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    valueText.setText(String.format(Locale.US, "%.2f", progress / 100f));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                float newValue = seekBar.getProgress() / 100f;
                onUpdate.accept(newValue);
                sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
            }
        });
    }

    private void setupColorPicker(GameObject go, Button colorButton, com.badlogic.gdx.graphics.Color initialColor, ColorConsumer onUpdate) {
        colorButton.setBackgroundColor(libGdxColorToAndroidColor(initialColor));

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose Color")
                    .initialColor(libGdxColorToAndroidColor(initialColor))
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);
                        int a = android.graphics.Color.alpha(selectedColor);

                        com.badlogic.gdx.graphics.Color newGdxColor = new com.badlogic.gdx.graphics.Color(
                                r / 255f, g / 255f, b / 255f, a / 255f
                        );

                        onUpdate.accept(newGdxColor);
                        sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
                    })
                    .setNegativeButton("Cancel", null)
                    .build()
                    .show();
        });
    }

    private void setupTextureSlot(GameObject go, View textureSlotLayout, String currentPath, StringConsumer onUpdate) {
        TextView pathText = textureSlotLayout.findViewById(R.id.text_texture_path);
        Button selectButton = textureSlotLayout.findViewById(R.id.btn_select_texture);
        ImageButton clearButton = textureSlotLayout.findViewById(R.id.btn_clear_texture);

        if (currentPath != null && !currentPath.isEmpty()) {
            pathText.setText(currentPath);
            clearButton.setVisibility(View.VISIBLE);
        } else {
            pathText.setText("None");
            clearButton.setVisibility(View.GONE);
        }

        selectButton.setOnClickListener(v -> showTexturePicker(fileName -> {
            onUpdate.accept(fileName);
            sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
            populateInspector(go);
        }));

        clearButton.setOnClickListener(v -> {
            onUpdate.accept(null);
            sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
            populateInspector(go);
        });
    }

    private void showTexturePicker(StringConsumer onTextureSelected) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();
        if (allFiles == null) return;

        final List<String> textureFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                textureFiles.add(file.getName());
            }
        }

        if (textureFiles.isEmpty()) {
            Toast.makeText(activity, "No image files (.png, .jpg) found in project.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select Texture")
                .setItems(textureFiles.toArray(new String[0]), (dialog, which) -> {
                    onTextureSelected.accept(textureFiles.get(which));
                })
                .show();
    }
}