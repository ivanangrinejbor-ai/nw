package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class StopMovingBrick extends BrickBaseType {

    private static final long serialVersionUID = 1L;

    public StopMovingBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_moving;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStopMovingAction(sprite, sequence));
    }
}
