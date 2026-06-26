package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AddObstacleBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AddObstacleBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_add_obstacle_edit_1);
    }

    public AddObstacleBrick(String spriteName) {
        this(new Formula(spriteName));
    }

    public AddObstacleBrick(Formula spriteName) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_add_obstacle;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAddObstacleAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
