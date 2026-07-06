package org.catrobat.catroid.content.scripts;

import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.AdmobBannerLoadedEventBrick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.eventids.EventId;

public class AdmobBannerLoadedScript extends Script {
    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new AdmobBannerLoadedEventBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new EventId(EventId.ADMOB_BANNER_LOADED);
    }
}
