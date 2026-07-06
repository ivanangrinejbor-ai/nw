package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobAppOpenLoadedScript;

import androidx.annotation.NonNull;

public class AdmobAppOpenLoadedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobAppOpenLoadedScript script;

    public AdmobAppOpenLoadedEventBrick() {
        this(new AdmobAppOpenLoadedScript());
    }

    public AdmobAppOpenLoadedEventBrick(@NonNull AdmobAppOpenLoadedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobAppOpenLoadedEventBrick clone = (AdmobAppOpenLoadedEventBrick) super.clone();
        clone.script = (AdmobAppOpenLoadedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_app_open_loaded;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
