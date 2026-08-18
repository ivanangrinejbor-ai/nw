package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class CheckPortBrick extends UserVariableBrickWithFormula {

	private static final long serialVersionUID = 1L;

	public CheckPortBrick() {
		addAllowedBrickField(BrickField.PORT, R.id.brick_check_port_edit);
	}

	public CheckPortBrick(String port) {
		this(new Formula(port));
	}

	public CheckPortBrick(Formula port) {
		this();
		setFormulaWithBrickField(BrickField.PORT, port);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_check_port;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.check_port_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCheckPortAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.PORT), userVariable));
	}
}