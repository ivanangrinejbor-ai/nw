package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class FollowPathWithSpeedBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public FollowPathWithSpeedBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_follow_path_with_speed_edit_1);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_follow_path_with_speed_edit_2);
    }

    public FollowPathWithSpeedBrick(String spriteName, Formula speed) {
        this(new Formula(spriteName), speed);
    }

    public FollowPathWithSpeedBrick(Formula spriteName, Formula speed) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
        setFormulaWithBrickField(BrickField.VALUE_1, speed);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_follow_path_with_speed;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFollowPathWithSpeedAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.VALUE_1)));
    }
}
