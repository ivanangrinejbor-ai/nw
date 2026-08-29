package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenTcpDisconnectedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

import java.util.List;

public class WhenTcpDisconnectedBrick extends BrickBaseType implements ScriptBrick {

	private static final long serialVersionUID = 1L;

	private WhenTcpDisconnectedScript script;

	public WhenTcpDisconnectedBrick() {
		this(new WhenTcpDisconnectedScript());
	}

	public WhenTcpDisconnectedBrick(WhenTcpDisconnectedScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenTcpDisconnectedBrick clone = (WhenTcpDisconnectedBrick) super.clone();
		clone.script = (WhenTcpDisconnectedScript) script.clone();
		clone.script.setScriptBrick(clone);
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_tcp_disconnected;
	}

	public WhenTcpDisconnectedScript getWhenTcpDisconnectedScript() {
		return script;
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
