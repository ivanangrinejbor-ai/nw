package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class JsonSetBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public JsonSetBrick() {
		addAllowedBrickField(BrickField.NAME, R.id.brick_json_set_name_edit);
		addAllowedBrickField(BrickField.KEY, R.id.brick_json_set_key_edit);
		addAllowedBrickField(BrickField.VALUE, R.id.brick_json_set_value_edit);
	}

	public JsonSetBrick(String name, String key, String value) {
		this(new Formula(name), new Formula(key), new Formula(value));
	}

	public JsonSetBrick(Formula name, Formula key, Formula value) {
		this();
		setFormulaWithBrickField(BrickField.NAME, name);
		setFormulaWithBrickField(BrickField.KEY, key);
		setFormulaWithBrickField(BrickField.VALUE, value);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_json_set;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createJsonSetAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.NAME),
				getFormulaWithBrickField(BrickField.KEY),
				getFormulaWithBrickField(BrickField.VALUE)));
	}
}
