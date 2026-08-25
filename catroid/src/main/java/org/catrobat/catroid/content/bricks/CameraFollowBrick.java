package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CameraFollowBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public CameraFollowBrick() {
		addAllowedBrickField(BrickField.STRING, R.id.brick_camera_follow_sprite_edit);
		addAllowedBrickField(BrickField.VALUE, R.id.brick_camera_follow_smooth_edit);
		addAllowedBrickField(BrickField.X_POSITION_CHANGE, R.id.brick_camera_follow_offset_x_edit);
		addAllowedBrickField(BrickField.Y_POSITION_CHANGE, R.id.brick_camera_follow_offset_y_edit);
	}

	public CameraFollowBrick(String spriteName, float smooth) {
		this(new Formula(spriteName), new Formula(smooth), new Formula(0), new Formula(0));
	}

	public CameraFollowBrick(Formula spriteName, Formula smooth, Formula offsetX, Formula offsetY) {
		this();
		setFormulaWithBrickField(BrickField.STRING, spriteName);
		setFormulaWithBrickField(BrickField.VALUE, smooth);
		setFormulaWithBrickField(BrickField.X_POSITION_CHANGE, offsetX);
		setFormulaWithBrickField(BrickField.Y_POSITION_CHANGE, offsetY);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_camera_follow;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCameraFollowAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.STRING),
				getFormulaWithBrickField(BrickField.VALUE),
				getFormulaWithBrickField(BrickField.X_POSITION_CHANGE),
				getFormulaWithBrickField(BrickField.Y_POSITION_CHANGE)));
	}
}
