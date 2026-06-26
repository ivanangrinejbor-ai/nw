package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class CreateObstaclesFromBackgroundBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateObstaclesFromBackgroundBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_obstacles_from_background;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateObstaclesFromBackgroundAction(sprite, sequence));
    }
}
