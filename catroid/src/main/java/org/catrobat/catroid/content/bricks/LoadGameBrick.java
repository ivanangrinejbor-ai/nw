package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadGameBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public LoadGameBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_load_game_slot_edit);
	}

	public LoadGameBrick(double slot) {
		this(new Formula(slot));
	}

	public LoadGameBrick(Formula slot) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, slot);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_load_game;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createLoadGameAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}
