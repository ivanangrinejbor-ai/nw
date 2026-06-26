package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class StopFollowingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public StopFollowingBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_stop_following_edit_1);
    }

    public StopFollowingBrick(String spriteName) {
        this(new Formula(spriteName));
    }

    public StopFollowingBrick(Formula spriteName) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_following;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createStopFollowingAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
