package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Create3dJoystickBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public Create3dJoystickBrick() {
		addAllowedBrickField(BrickField.X_POSITION, R.id.brick_create_3d_joystick_x);
		addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_create_3d_joystick_y);
		addAllowedBrickField(BrickField.STRING_1, R.id.brick_create_3d_joystick_bg);
		addAllowedBrickField(BrickField.STRING_2, R.id.brick_create_3d_joystick_thumb);
		addAllowedBrickField(BrickField.SPEED, R.id.brick_create_3d_joystick_speed);
	}

	public Create3dJoystickBrick(double posX, double posY, String bgFile, String thumbFile, double speed) {
		this(new Formula(posX), new Formula(posY), new Formula(bgFile), new Formula(thumbFile), new Formula(speed));
	}

	public Create3dJoystickBrick(Formula posX, Formula posY, Formula bgFile, Formula thumbFile, Formula speed) {
		this();
		setFormulaWithBrickField(BrickField.X_POSITION, posX);
		setFormulaWithBrickField(BrickField.Y_POSITION, posY);
		setFormulaWithBrickField(BrickField.STRING_1, bgFile);
		setFormulaWithBrickField(BrickField.STRING_2, thumbFile);
		setFormulaWithBrickField(BrickField.SPEED, speed);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_3d_joystick;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCreate3dJoystickAction(
				sprite,
				sequence,
				getFormulaWithBrickField(BrickField.X_POSITION),
				getFormulaWithBrickField(BrickField.Y_POSITION),
				getFormulaWithBrickField(BrickField.STRING_1),
				getFormulaWithBrickField(BrickField.STRING_2),
				getFormulaWithBrickField(BrickField.SPEED)
		));
	}
}
