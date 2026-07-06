package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobAppOpenClosedScript;

import androidx.annotation.NonNull;

public class AdmobAppOpenClosedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobAppOpenClosedScript script;

    public AdmobAppOpenClosedEventBrick() {
        this(new AdmobAppOpenClosedScript());
    }

    public AdmobAppOpenClosedEventBrick(@NonNull AdmobAppOpenClosedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobAppOpenClosedEventBrick clone = (AdmobAppOpenClosedEventBrick) super.clone();
        clone.script = (AdmobAppOpenClosedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_app_open_closed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
