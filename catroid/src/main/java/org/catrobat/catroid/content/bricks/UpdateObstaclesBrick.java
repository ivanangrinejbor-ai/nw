package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class UpdateObstaclesBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public UpdateObstaclesBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_update_obstacles;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createUpdateObstaclesAction(sprite, sequence));
    }
}
