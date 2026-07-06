package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobRewardedShownScript;

import androidx.annotation.NonNull;

public class AdmobRewardedShownEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobRewardedShownScript script;

    public AdmobRewardedShownEventBrick() {
        this(new AdmobRewardedShownScript());
    }

    public AdmobRewardedShownEventBrick(@NonNull AdmobRewardedShownScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobRewardedShownEventBrick clone = (AdmobRewardedShownEventBrick) super.clone();
        clone.script = (AdmobRewardedShownScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_rewarded_shown;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
