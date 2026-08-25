package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetSeedBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public SetSeedBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_set_seed_edit);
	}

	public SetSeedBrick(double seed) {
		this(new Formula(seed));
	}

	public SetSeedBrick(Formula seed) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, seed);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_seed;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetSeedAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}
