package org.catrobat.catroid.content.scripts;

import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.AdmobAppOpenClosedEventBrick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class AdmobAppOpenClosedScript extends Script {
    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new AdmobAppOpenClosedEventBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.ADMOB_APP_OPEN_CLOSED);
    }
}
