package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobBannerHiddenScript;

import androidx.annotation.NonNull;

public class AdmobBannerHiddenEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobBannerHiddenScript script;

    public AdmobBannerHiddenEventBrick() {
        this(new AdmobBannerHiddenScript());
    }

    public AdmobBannerHiddenEventBrick(@NonNull AdmobBannerHiddenScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobBannerHiddenEventBrick clone = (AdmobBannerHiddenEventBrick) super.clone();
        clone.script = (AdmobBannerHiddenScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_banner_hidden;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
