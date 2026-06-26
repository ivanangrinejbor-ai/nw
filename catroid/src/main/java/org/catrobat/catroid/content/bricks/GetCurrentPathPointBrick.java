package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class GetCurrentPathPointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public GetCurrentPathPointBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_get_current_path_point_edit_1);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_get_current_path_point_edit_2);
        addAllowedBrickField(BrickField.TEXT_3, R.id.brick_get_current_path_point_edit_3);
    }

    public GetCurrentPathPointBrick(String spriteName, String varX, String varY) {
        this(new Formula(spriteName), new Formula(varX), new Formula(varY));
    }

    public GetCurrentPathPointBrick(Formula spriteName, Formula varX, Formula varY) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
        setFormulaWithBrickField(BrickField.TEXT_2, varX);
        setFormulaWithBrickField(BrickField.TEXT_3, varY);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_get_current_path_point;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createGetCurrentPathPointAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2),
                        getFormulaWithBrickField(BrickField.TEXT_3)));
    }
}
