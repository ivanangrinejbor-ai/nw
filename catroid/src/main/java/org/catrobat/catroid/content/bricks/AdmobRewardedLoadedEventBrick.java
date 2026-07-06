package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobRewardedLoadedScript;

import androidx.annotation.NonNull;

public class AdmobRewardedLoadedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobRewardedLoadedScript script;

    public AdmobRewardedLoadedEventBrick() {
        this(new AdmobRewardedLoadedScript());
    }

    public AdmobRewardedLoadedEventBrick(@NonNull AdmobRewardedLoadedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobRewardedLoadedEventBrick clone = (AdmobRewardedLoadedEventBrick) super.clone();
        clone.script = (AdmobRewardedLoadedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_rewarded_loaded;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
