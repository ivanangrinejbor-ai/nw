package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenIntervalScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.List;

public class WhenIntervalBrick extends FormulaBrick implements ScriptBrick {

	private static final long serialVersionUID = 1L;

	private WhenIntervalScript script;

	public WhenIntervalBrick() {
		this(new WhenIntervalScript());
	}

	public WhenIntervalBrick(WhenIntervalScript script) {
		addAllowedBrickField(BrickField.TIME_TO_WAIT_IN_SECONDS, R.id.brick_when_interval_edit_text);
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;

		formulaMap = script.getFormulaMap();
	}

	public WhenIntervalBrick(Formula seconds) {
		this(new WhenIntervalScript(seconds));
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenIntervalBrick clone = (WhenIntervalBrick) super.clone();
		clone.script = (WhenIntervalScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.formulaMap = clone.script.getFormulaMap();
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_interval;
	}

	public Formula getSecondsFormula() {
		return getFormulaWithBrickField(BrickField.TIME_TO_WAIT_IN_SECONDS);
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public int getPositionInScript() {
		return -1;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		super.addToFlatList(bricks);
		for (Brick brick : getScript().getBrickList()) {
			brick.addToFlatList(bricks);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return getScript().getBrickList();
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return -1;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		getScript().setCommentedOut(commentedOut);
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
