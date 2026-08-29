package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenTcpDisconnectedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.TcpDisconnectEventId;

public class WhenTcpDisconnectedScript extends Script {
	private static final long serialVersionUID = 1L;

	public WhenTcpDisconnectedScript() {
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenTcpDisconnectedBrick(this);
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
		return new TcpDisconnectEventId();
	}
}
