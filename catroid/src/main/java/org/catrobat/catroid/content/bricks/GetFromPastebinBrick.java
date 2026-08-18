package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class GetFromPastebinBrick extends UserVariableBrickWithFormula {

	private static final long serialVersionUID = 1L;

	public GetFromPastebinBrick() {
		addAllowedBrickField(BrickField.URL, R.id.brick_get_pastebin_edit_url);
	}

	public GetFromPastebinBrick(String url) {
		this(new Formula(url));
	}

	public GetFromPastebinBrick(Formula url) {
		this();
		setFormulaWithBrickField(BrickField.URL, url);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_get_from_pastebin;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.get_pastebin_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createGetFromPastebinAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.URL), userVariable));
	}
}