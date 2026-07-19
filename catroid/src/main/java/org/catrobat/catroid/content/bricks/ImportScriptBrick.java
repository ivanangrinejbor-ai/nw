package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;

import java.lang.ref.WeakReference;

import androidx.appcompat.app.AppCompatActivity;

public class ImportScriptBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private int overwriteSelection = 0;
	private int useSceneSelection = 0;

	private static volatile WeakReference<ImportScriptBrick> pendingPickerBrickRef;

	public ImportScriptBrick() {
		addAllowedBrickField(BrickField.IMPORT_SCRIPT_OBJECT, R.id.brick_import_script_object_edit);
		addAllowedBrickField(BrickField.IMPORT_SCRIPT_FILE, R.id.brick_import_script_file_edit);
		addAllowedBrickField(BrickField.IMPORT_SCRIPT_SCENE, R.id.brick_import_script_scene_edit);
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_SCENE, new Formula(""));
	}

	public ImportScriptBrick(String objectName, String filePath, boolean overwrite) {
		this();
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT, new Formula(objectName));
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, new Formula(filePath));
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	public ImportScriptBrick(Formula objectName, Formula filePath, boolean overwrite) {
		this();
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT, objectName);
		setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, filePath);
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	public boolean isOverwrite() {
		return overwriteSelection == 1;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwriteSelection = overwrite ? 1 : 0;
	}

	public boolean isUseScene() {
		return useSceneSelection == 1;
	}

	public void setUseScene(boolean useScene) {
		this.useSceneSelection = useScene ? 1 : 0;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_import_script;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);

		CheckBox sceneCheckbox = view.findViewById(R.id.brick_import_script_scene_checkbox);
		TextView sceneEdit = view.findViewById(R.id.brick_import_script_scene_edit);
		TextView sceneLabel = view.findViewById(R.id.brick_import_script_scene_label);

		sceneCheckbox.setChecked(useSceneSelection == 1);
		sceneEdit.setVisibility(useSceneSelection == 1 ? View.VISIBLE : View.GONE);
		sceneLabel.setVisibility(useSceneSelection == 1 ? View.VISIBLE : View.GONE);

		sceneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			useSceneSelection = isChecked ? 1 : 0;
			sceneEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			sceneLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
		});

		Spinner overwriteSpinner = view.findViewById(R.id.brick_import_script_overwrite_spinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
				R.layout.simple_spinner_item_white_text,
				new String[]{context.getString(R.string.no), context.getString(R.string.yes)});
		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
		overwriteSpinner.setAdapter(adapter);
		overwriteSpinner.setSelection(overwriteSelection);
		overwriteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				overwriteSelection = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		android.widget.TextView fileEdit = view.findViewById(R.id.brick_import_script_file_edit);
		fileEdit.setOnClickListener(v -> openFilePicker(context));

		return view;
	}

	private void openFilePicker(Context context) {
		AppCompatActivity activity = UiUtils.getActivityFromView(view);
		if (activity == null) {
			return;
		}
		pendingPickerBrickRef = new WeakReference<>(this);
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "text/xml"});
		activity.startActivityForResult(intent, org.catrobat.catroid.ui.SpriteActivity.REQUEST_NEO_SCRIPT_FILE);
	}

	public static void onFilePicked(android.net.Uri uri) {
		WeakReference<ImportScriptBrick> ref = pendingPickerBrickRef;
		if (ref == null || uri == null) {
			return;
		}
		ImportScriptBrick brick = ref.get();
		pendingPickerBrickRef = null;
		if (brick == null) {
			return;
		}
		String path = uri.toString();
		brick.setFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE, new Formula(path));
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createImportScriptAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.IMPORT_SCRIPT_OBJECT),
				getFormulaWithBrickField(BrickField.IMPORT_SCRIPT_FILE),
				isOverwrite(),
				isUseScene() ? getFormulaWithBrickField(BrickField.IMPORT_SCRIPT_SCENE) : null));
	}
}
