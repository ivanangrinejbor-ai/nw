package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class ContinueBrick extends BrickBaseType {

    private static final long serialVersionUID = 1L;

    public ContinueBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_continue;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createContinueAction());
    }
}
