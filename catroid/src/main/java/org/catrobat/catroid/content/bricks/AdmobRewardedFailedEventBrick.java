package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobRewardedFailedScript;

import androidx.annotation.NonNull;

public class AdmobRewardedFailedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobRewardedFailedScript script;

    public AdmobRewardedFailedEventBrick() {
        this(new AdmobRewardedFailedScript());
    }

    public AdmobRewardedFailedEventBrick(@NonNull AdmobRewardedFailedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobRewardedFailedEventBrick clone = (AdmobRewardedFailedEventBrick) super.clone();
        clone.script = (AdmobRewardedFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_rewarded_failed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
