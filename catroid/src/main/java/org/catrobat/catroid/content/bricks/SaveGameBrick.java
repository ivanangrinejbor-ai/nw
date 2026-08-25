package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SaveGameBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public SaveGameBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_save_game_slot_edit);
	}

	public SaveGameBrick(double slot) {
		this(new Formula(slot));
	}

	public SaveGameBrick(Formula slot) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, slot);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_save_game;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSaveGameAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}
