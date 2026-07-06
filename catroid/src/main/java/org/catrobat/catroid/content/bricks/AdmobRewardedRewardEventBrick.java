package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobRewardedRewardScript;

import androidx.annotation.NonNull;

public class AdmobRewardedRewardEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobRewardedRewardScript script;

    public AdmobRewardedRewardEventBrick() {
        this(new AdmobRewardedRewardScript());
    }

    public AdmobRewardedRewardEventBrick(@NonNull AdmobRewardedRewardScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobRewardedRewardEventBrick clone = (AdmobRewardedRewardEventBrick) super.clone();
        clone.script = (AdmobRewardedRewardScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_rewarded_reward;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
