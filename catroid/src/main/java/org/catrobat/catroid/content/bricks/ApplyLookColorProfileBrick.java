package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class ApplyLookColorProfileBrick extends BrickBaseType
		implements BrickSpinner.OnItemSelectedListener<LookData> {
	private static final long serialVersionUID = 1L;
	private LookData targetLook;
	private transient BrickSpinner<LookData> spinner;

	public LookData getTargetLook() {
		return targetLook;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		ApplyLookColorProfileBrick clone = (ApplyLookColorProfileBrick) super.clone();
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_apply_look_color_profile;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		List<Nameable> items = new ArrayList<>();
		items.addAll(getSprite().getLookList());
		spinner = new BrickSpinner<>(R.id.brick_apply_look_color_profile_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(targetLook);
		return view;
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable LookData item) {
		targetLook = item;
	}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createApplyLookColorProfileAction(sprite, targetLook));
	}

	private Sprite getSprite() {
		return ProjectManager.getInstance().getCurrentSprite();
	}
}
