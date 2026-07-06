package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class TouchDirectionBrick extends BrickBaseType {
    private static final long serialVersionUID = 1L;

    @Override
    public int getViewResource() {
        return R.layout.brick_touch_direction;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createTouchDirectionAction(sprite, sequence));
    }
}
