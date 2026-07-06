package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobInterstitialFailedScript;

import androidx.annotation.NonNull;

public class AdmobInterstitialFailedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobInterstitialFailedScript script;

    public AdmobInterstitialFailedEventBrick() {
        this(new AdmobInterstitialFailedScript());
    }

    public AdmobInterstitialFailedEventBrick(@NonNull AdmobInterstitialFailedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobInterstitialFailedEventBrick clone = (AdmobInterstitialFailedEventBrick) super.clone();
        clone.script = (AdmobInterstitialFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_interstitial_failed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
