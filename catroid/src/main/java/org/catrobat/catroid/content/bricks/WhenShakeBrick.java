package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.WhenShakeScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.Sprite;

public class WhenShakeBrick extends ScriptBrickBaseType {
	private static final long serialVersionUID = 1L;

	private WhenShakeScript script;

	public WhenShakeBrick() {
		this(new WhenShakeScript());
	}

	public WhenShakeBrick(WhenShakeScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenShakeBrick clone = (WhenShakeBrick) super.clone();
		clone.script = (WhenShakeScript) script.clone();
		clone.script.setScriptBrick(clone);
		return clone;
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_shake;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
