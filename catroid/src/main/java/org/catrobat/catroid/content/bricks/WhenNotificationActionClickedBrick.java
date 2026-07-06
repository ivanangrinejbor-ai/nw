package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNotificationActionClickedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenNotificationActionClickedBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenNotificationActionClickedScript script;

    public WhenNotificationActionClickedBrick() {
        this(new WhenNotificationActionClickedScript());
    }

    public WhenNotificationActionClickedBrick(WhenNotificationActionClickedScript script) {
        script.setScriptBrick(this);
        commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_notification_action_clicked;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenNotificationActionClickedBrick clone = (WhenNotificationActionClickedBrick) super.clone();
        clone.script = (WhenNotificationActionClickedScript) script.clone();
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
