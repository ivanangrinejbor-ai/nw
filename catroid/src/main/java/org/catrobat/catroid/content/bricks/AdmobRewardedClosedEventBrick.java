package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobRewardedClosedScript;

import androidx.annotation.NonNull;

public class AdmobRewardedClosedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobRewardedClosedScript script;

    public AdmobRewardedClosedEventBrick() {
        this(new AdmobRewardedClosedScript());
    }

    public AdmobRewardedClosedEventBrick(@NonNull AdmobRewardedClosedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobRewardedClosedEventBrick clone = (AdmobRewardedClosedEventBrick) super.clone();
        clone.script = (AdmobRewardedClosedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_rewarded_closed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
