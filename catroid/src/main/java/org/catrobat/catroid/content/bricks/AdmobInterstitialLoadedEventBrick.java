package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobInterstitialLoadedScript;

import androidx.annotation.NonNull;

public class AdmobInterstitialLoadedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobInterstitialLoadedScript script;

    public AdmobInterstitialLoadedEventBrick() {
        this(new AdmobInterstitialLoadedScript());
    }

    public AdmobInterstitialLoadedEventBrick(@NonNull AdmobInterstitialLoadedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobInterstitialLoadedEventBrick clone = (AdmobInterstitialLoadedEventBrick) super.clone();
        clone.script = (AdmobInterstitialLoadedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_interstitial_loaded;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
