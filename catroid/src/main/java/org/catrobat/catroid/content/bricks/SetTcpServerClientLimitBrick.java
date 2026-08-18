package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetTcpServerClientLimitBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public SetTcpServerClientLimitBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_tcp_limit_edit);
	}

	public SetTcpServerClientLimitBrick(int limit) {
		this(new Formula(String.valueOf(limit)));
	}

	public SetTcpServerClientLimitBrick(String limit) {
		this(new Formula(limit));
	}

	public SetTcpServerClientLimitBrick(Formula limit) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, limit);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_tcp_client_limit;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetTcpServerClientLimitAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}