package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenNotificationActionClickedBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class WhenNotificationActionClickedScript extends Script {
    private static final long serialVersionUID = 1L;

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenNotificationActionClickedBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.NOTIFICATION_ACTION_CLICKED);
    }
}
