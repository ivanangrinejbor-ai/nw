package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenShakeBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class WhenShakeScript extends Script {
	private static final long serialVersionUID = 1L;

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenShakeBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new EventId(EventId.SHAKE);
	}
}
