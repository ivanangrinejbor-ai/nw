package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNotificationShownScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenNotificationShownBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenNotificationShownScript script;

    public WhenNotificationShownBrick() {
        this(new WhenNotificationShownScript());
    }

    public WhenNotificationShownBrick(WhenNotificationShownScript script) {
        script.setScriptBrick(this);
        commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_notification_shown;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenNotificationShownBrick clone = (WhenNotificationShownBrick) super.clone();
        clone.script = (WhenNotificationShownScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
