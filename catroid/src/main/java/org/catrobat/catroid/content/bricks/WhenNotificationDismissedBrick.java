package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNotificationDismissedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenNotificationDismissedBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenNotificationDismissedScript script;

    public WhenNotificationDismissedBrick() {
        this(new WhenNotificationDismissedScript());
    }

    public WhenNotificationDismissedBrick(WhenNotificationDismissedScript script) {
        script.setScriptBrick(this);
        commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_notification_dismissed;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenNotificationDismissedBrick clone = (WhenNotificationDismissedBrick) super.clone();
        clone.script = (WhenNotificationDismissedScript) script.clone();
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
