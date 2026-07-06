package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.scripts.AdmobInitializedScript;

import androidx.annotation.NonNull;

public class AdmobInitializedEventBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private AdmobInitializedScript script;

    public AdmobInitializedEventBrick() {
        this(new AdmobInitializedScript());
    }

    public AdmobInitializedEventBrick(@NonNull AdmobInitializedScript script) {
        script.setScriptBrick(this);
        this.script = script;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        AdmobInitializedEventBrick clone = (AdmobInitializedEventBrick) super.clone();
        clone.script = (AdmobInitializedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_event_admob_initialized;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
