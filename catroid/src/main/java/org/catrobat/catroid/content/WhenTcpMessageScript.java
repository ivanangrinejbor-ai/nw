package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenTcpMessageBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.TcpMessageEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class WhenTcpMessageScript extends Script {
	private static final long serialVersionUID = 1L;

	private UserVariable messageVariable;

	public WhenTcpMessageScript() {
	}

	public WhenTcpMessageScript(UserVariable variable) {
		this.messageVariable = variable;
	}

	public UserVariable getMessageVariable() {
		return messageVariable;
	}

	public void setMessageVariable(UserVariable variable) {
		this.messageVariable = variable;
	}

	@Override
	public Script clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenTcpMessageBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public void addRequiredResources(final Brick.ResourcesSet requiredResourcesSet) {
		for (Brick brick : brickList) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new TcpMessageEventId();
	}
}
