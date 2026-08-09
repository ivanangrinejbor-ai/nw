package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class BreakBrick extends BrickBaseType {

    private static final long serialVersionUID = 1L;

    public BreakBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_break;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createBreakAction());
    }
}
