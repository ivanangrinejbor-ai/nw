package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RemoveNotificationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RemoveNotificationBrick() {
        addAllowedBrickField(BrickField.NOTIFICATION_ID, R.id.brick_remove_notification_edit_id);
    }

    public RemoveNotificationBrick(Formula id) {
        this();
        setFormulaWithBrickField(BrickField.NOTIFICATION_ID, id);
    }

    public RemoveNotificationBrick(int id) {
        this(new Formula(id));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_notification;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRemoveNotificationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ID)));
    }
}
