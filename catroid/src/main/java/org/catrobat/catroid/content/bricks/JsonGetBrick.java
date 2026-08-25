package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class JsonGetBrick extends UserVariableBrickWithFormula {
	private static final long serialVersionUID = 1L;

	public JsonGetBrick() {
		addAllowedBrickField(BrickField.NAME, R.id.brick_json_get_name_edit);
		addAllowedBrickField(BrickField.KEY, R.id.brick_json_get_key_edit);
	}

	public JsonGetBrick(String name, String key) {
		this(new Formula(name), new Formula(key), null);
	}

	public JsonGetBrick(Formula name, Formula key, UserVariable userVariable) {
		this();
		setFormulaWithBrickField(BrickField.NAME, name);
		setFormulaWithBrickField(BrickField.KEY, key);
		this.userVariable = userVariable;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_json_get;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.brick_json_get_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		if (userVariable == null || userVariable.getName() == null) {
			return;
		}
		sequence.addAction(sprite.getActionFactory().createJsonGetAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.NAME),
				getFormulaWithBrickField(BrickField.KEY),
				userVariable));
	}
}
