package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class ReturnToPreviousProjectBrick extends BrickBaseType {
    @Override public int getViewResource() { return R.layout.brick_return_to_previous_project; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createReturnToPreviousProjectAction());
    }
}