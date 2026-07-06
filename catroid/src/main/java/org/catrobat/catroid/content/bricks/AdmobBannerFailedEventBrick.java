package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobBannerFailedScript;

import androidx.annotation.NonNull;

public class AdmobBannerFailedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobBannerFailedScript script;

    public AdmobBannerFailedEventBrick() {
        this(new AdmobBannerFailedScript());
    }

    public AdmobBannerFailedEventBrick(@NonNull AdmobBannerFailedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobBannerFailedEventBrick clone = (AdmobBannerFailedEventBrick) super.clone();
        clone.script = (AdmobBannerFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_banner_failed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
