package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenNotificationReplyScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenNotificationReplyBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenNotificationReplyScript script;

    public WhenNotificationReplyBrick() {
        this(new WhenNotificationReplyScript());
    }

    public WhenNotificationReplyBrick(WhenNotificationReplyScript script) {
        script.setScriptBrick(this);
        commentedOut = script.isCommentedOut();
        this.script = script;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_notification_reply;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenNotificationReplyBrick clone = (WhenNotificationReplyBrick) super.clone();
        clone.script = (WhenNotificationReplyScript) script.clone();
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
