package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobBannerShownScript;

import androidx.annotation.NonNull;

public class AdmobBannerShownEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobBannerShownScript script;

    public AdmobBannerShownEventBrick() {
        this(new AdmobBannerShownScript());
    }

    public AdmobBannerShownEventBrick(@NonNull AdmobBannerShownScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobBannerShownEventBrick clone = (AdmobBannerShownEventBrick) super.clone();
        clone.script = (AdmobBannerShownScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_banner_shown;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
