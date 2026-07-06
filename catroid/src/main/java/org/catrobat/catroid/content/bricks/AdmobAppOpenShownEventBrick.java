package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobAppOpenShownScript;

import androidx.annotation.NonNull;

public class AdmobAppOpenShownEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobAppOpenShownScript script;

    public AdmobAppOpenShownEventBrick() {
        this(new AdmobAppOpenShownScript());
    }

    public AdmobAppOpenShownEventBrick(@NonNull AdmobAppOpenShownScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobAppOpenShownEventBrick clone = (AdmobAppOpenShownEventBrick) super.clone();
        clone.script = (AdmobAppOpenShownScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_app_open_shown;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
