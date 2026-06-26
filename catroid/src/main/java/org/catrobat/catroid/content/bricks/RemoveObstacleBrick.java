package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RemoveObstacleBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RemoveObstacleBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_remove_obstacle_edit_1);
    }

    public RemoveObstacleBrick(String spriteName) {
        this(new Formula(spriteName));
    }

    public RemoveObstacleBrick(Formula spriteName) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_obstacle;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRemoveObstacleAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
