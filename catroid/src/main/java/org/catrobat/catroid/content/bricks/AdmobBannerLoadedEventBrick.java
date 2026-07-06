package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobBannerLoadedScript;

import androidx.annotation.NonNull;

public class AdmobBannerLoadedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobBannerLoadedScript script;

    public AdmobBannerLoadedEventBrick() {
        this(new AdmobBannerLoadedScript());
    }

    public AdmobBannerLoadedEventBrick(@NonNull AdmobBannerLoadedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobBannerLoadedEventBrick clone = (AdmobBannerLoadedEventBrick) super.clone();
        clone.script = (AdmobBannerLoadedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_banner_loaded;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
