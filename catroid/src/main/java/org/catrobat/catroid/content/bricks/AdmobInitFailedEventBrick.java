package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobInitFailedScript;

import androidx.annotation.NonNull;

public class AdmobInitFailedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobInitFailedScript script;

    public AdmobInitFailedEventBrick() {
        this(new AdmobInitFailedScript());
    }

    public AdmobInitFailedEventBrick(@NonNull AdmobInitFailedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobInitFailedEventBrick clone = (AdmobInitFailedEventBrick) super.clone();
        clone.script = (AdmobInitFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_init_failed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
