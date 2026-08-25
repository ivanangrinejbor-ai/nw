package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetGameTimeScaleBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public SetGameTimeScaleBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_time_scale_edit);
	}

	public SetGameTimeScaleBrick(float scale) {
		this(new Formula(scale));
	}

	public SetGameTimeScaleBrick(Formula scale) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, scale);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_game_time_scale;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetGameTimeScaleAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}
