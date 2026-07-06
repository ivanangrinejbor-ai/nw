package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenNotificationShownBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class WhenNotificationShownScript extends Script {
    private static final long serialVersionUID = 1L;

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenNotificationShownBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.NOTIFICATION_SHOWN);
    }
}
