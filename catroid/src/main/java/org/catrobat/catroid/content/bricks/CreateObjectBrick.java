package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateObjectBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private int useSceneSelection = 0; // 0 = current scene, 1 = use typed scene name
	private int persistentSelection = 0; // 0 = runtime only, 1 = persist to project

	public CreateObjectBrick() {
		addAllowedBrickField(BrickField.CREATE_OBJECT_NAME, R.id.brick_create_object_name_edit);
		addAllowedBrickField(BrickField.CREATE_OBJECT_SCENE, R.id.brick_create_object_scene_edit);
		setFormulaWithBrickField(BrickField.CREATE_OBJECT_SCENE, new Formula(""));
	}

	public CreateObjectBrick(String objectName) {
		this();
		setFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME, new Formula(objectName));
	}

	public CreateObjectBrick(Formula objectName) {
		this();
		setFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME, objectName);
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		CreateObjectBrick clone = (CreateObjectBrick) super.clone();
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_object;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		CheckBox sceneCheckbox = view.findViewById(R.id.brick_create_object_scene_checkbox);
		TextView sceneEdit = view.findViewById(R.id.brick_create_object_scene_edit);
		TextView sceneLabel = view.findViewById(R.id.brick_create_object_scene_label);

		sceneCheckbox.setChecked(useSceneSelection == 1);
		sceneEdit.setVisibility(useSceneSelection == 1 ? View.VISIBLE : View.GONE);
		sceneLabel.setVisibility(useSceneSelection == 1 ? View.VISIBLE : View.GONE);

		sceneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			useSceneSelection = isChecked ? 1 : 0;
			sceneEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			sceneLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
		});

		// Persist to project spinner (No = runtime only, Yes = persist)
		Spinner persistentSpinner = view.findViewById(R.id.brick_create_object_persistent_spinner);
		ArrayAdapter<String> persistentAdapter = new ArrayAdapter<>(context,
				R.layout.simple_spinner_item_white_text,
				new String[]{context.getString(R.string.no), context.getString(R.string.yes)});
		persistentAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
		persistentSpinner.setAdapter(persistentAdapter);
		persistentSpinner.setSelection(persistentSelection);
		persistentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				persistentSelection = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCreateObjectAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.CREATE_OBJECT_NAME),
				isUseScene() ? getFormulaWithBrickField(BrickField.CREATE_OBJECT_SCENE) : null,
				isPersistent()));
	}

	public boolean isPersistent() {
		return persistentSelection == 1;
	}

	public boolean isUseScene() {
		return useSceneSelection == 1;
	}
}
