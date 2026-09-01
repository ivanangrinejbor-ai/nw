package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class CaptureLookColorProfileBrick extends BrickBaseType {
	private static final long serialVersionUID = 1L;

	@Override
	public int getViewResource() {
		return R.layout.brick_capture_look_color_profile;
	}

	@Override
	public View getView(Context context) {
		return super.getView(context);
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCaptureLookColorProfileAction(sprite));
	}
}
