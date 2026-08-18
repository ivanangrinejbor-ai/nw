package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetTcpServerTimeoutBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	public SetTcpServerTimeoutBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_tcp_timeout_edit);
	}

	public SetTcpServerTimeoutBrick(int timeout) {
		this(new Formula(String.valueOf(timeout)));
	}

	public SetTcpServerTimeoutBrick(String timeout) {
		this(new Formula(timeout));
	}

	public SetTcpServerTimeoutBrick(Formula timeout) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, timeout);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_set_tcp_timeout;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSetTcpServerTimeoutAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}