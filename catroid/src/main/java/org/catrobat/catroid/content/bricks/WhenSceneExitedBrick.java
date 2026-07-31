package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenSceneExitedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WhenSceneExitedBrick extends ScriptBrickBaseType implements
		BrickSpinner.OnItemSelectedListener<Scene> {

	private static final long serialVersionUID = 1L;

	private WhenSceneExitedScript script;

	private transient BrickSpinner<Scene> spinner;

	public WhenSceneExitedBrick() {
		this(new WhenSceneExitedScript());
	}

	public WhenSceneExitedBrick(@NonNull WhenSceneExitedScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	public String getSceneName() {
		return script.getSceneName();
	}

	public void setSceneName(String sceneName) {
		script.setSceneName(sceneName);
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenSceneExitedBrick clone = (WhenSceneExitedBrick) super.clone();
		clone.script = (WhenSceneExitedScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.spinner = null;
		return clone;
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_scene_exited;
	}

	@Override
	public View getView(final Context context) {
		super.getView(context);

		List<Nameable> items = new ArrayList<>();
		items.addAll(ProjectManager.getInstance().getCurrentProject().getSceneList());
		spinner = new BrickSpinner<>(R.id.brick_when_scene_exited_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(getSceneName());

		return view;
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable Scene item) {
		setSceneName(item != null ? item.getName() : null);
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
