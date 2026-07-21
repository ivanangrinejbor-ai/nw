package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ShakeScreenBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public ShakeScreenBrick() {
		addAllowedBrickField(BrickField.INTENSITY, R.id.brick_shake_screen_int_edit);
		addAllowedBrickField(BrickField.DURATION, R.id.brick_shake_screen_dur_edit);
	}

	public ShakeScreenBrick(float intensity, float duration) {
		this(new Formula(intensity), new Formula(duration));
	}

	public ShakeScreenBrick(Formula intensity, Formula duration) {
		this();
		setFormulaWithBrickField(BrickField.INTENSITY, intensity);
		setFormulaWithBrickField(BrickField.DURATION, duration);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_shake_screen;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createShakeScreenAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.INTENSITY),
				getFormulaWithBrickField(BrickField.DURATION)));
	}
}
