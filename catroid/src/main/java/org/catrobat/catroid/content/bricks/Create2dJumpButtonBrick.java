package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Create2dJumpButtonBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public Create2dJumpButtonBrick() {
		addAllowedBrickField(BrickField.X_POSITION, R.id.brick_create_2d_jump_x);
		addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_create_2d_jump_y);
		addAllowedBrickField(BrickField.STRING_1, R.id.brick_create_2d_jump_active);
		addAllowedBrickField(BrickField.STRING_2, R.id.brick_create_2d_jump_inactive);
		addAllowedBrickField(BrickField.JUMP_POWER, R.id.brick_create_2d_jump_power);
	}

	public Create2dJumpButtonBrick(double posX, double posY, String activeFile, String inactiveFile, double power) {
		this(new Formula(posX), new Formula(posY), new Formula(activeFile), new Formula(inactiveFile), new Formula(power));
	}

	public Create2dJumpButtonBrick(Formula posX, Formula posY, Formula activeFile, Formula inactiveFile, Formula power) {
		this();
		setFormulaWithBrickField(BrickField.X_POSITION, posX);
		setFormulaWithBrickField(BrickField.Y_POSITION, posY);
		setFormulaWithBrickField(BrickField.STRING_1, activeFile);
		setFormulaWithBrickField(BrickField.STRING_2, inactiveFile);
		setFormulaWithBrickField(BrickField.JUMP_POWER, power);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_create_2d_jump_button;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCreate2dJumpButtonAction(
				sprite,
				sequence,
				getFormulaWithBrickField(BrickField.X_POSITION),
				getFormulaWithBrickField(BrickField.Y_POSITION),
				getFormulaWithBrickField(BrickField.STRING_1),
				getFormulaWithBrickField(BrickField.STRING_2),
				getFormulaWithBrickField(BrickField.JUMP_POWER)
		));
	}
}
