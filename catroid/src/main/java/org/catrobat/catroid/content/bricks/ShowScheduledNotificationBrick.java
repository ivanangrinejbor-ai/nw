package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ShowScheduledNotificationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ShowScheduledNotificationBrick() {
        addAllowedBrickField(BrickField.NOTIFICATION_ID, R.id.brick_show_scheduled_notification_edit_id);
        addAllowedBrickField(BrickField.DURATION_IN_SECONDS, R.id.brick_show_scheduled_notification_edit_duration);
    }

    public ShowScheduledNotificationBrick(Formula id, Formula delay) {
        this();
        setFormulaWithBrickField(BrickField.NOTIFICATION_ID, id);
        setFormulaWithBrickField(BrickField.DURATION_IN_SECONDS, delay);
    }

    public ShowScheduledNotificationBrick(int id, int delay) {
        this(new Formula(id), new Formula(delay));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_show_scheduled_notification;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createShowScheduledNotificationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ID),
                        getFormulaWithBrickField(BrickField.DURATION_IN_SECONDS)));
    }
}
