package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobAppOpenFailedScript;

import androidx.annotation.NonNull;

public class AdmobAppOpenFailedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobAppOpenFailedScript script;

    public AdmobAppOpenFailedEventBrick() {
        this(new AdmobAppOpenFailedScript());
    }

    public AdmobAppOpenFailedEventBrick(@NonNull AdmobAppOpenFailedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobAppOpenFailedEventBrick clone = (AdmobAppOpenFailedEventBrick) super.clone();
        clone.script = (AdmobAppOpenFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_app_open_failed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
