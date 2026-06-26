package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CheckPathExistsBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CheckPathExistsBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_check_path_exists_edit_1);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_check_path_exists_edit_2);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_check_path_exists_edit_3);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_check_path_exists_edit_4);
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_check_path_exists_edit_5);
    }

    public CheckPathExistsBrick(Formula startX, Formula startY, Formula endX, Formula endY, String resultVar) {
        this(startX, startY, endX, endY, new Formula(resultVar));
    }

    public CheckPathExistsBrick(Formula startX, Formula startY, Formula endX, Formula endY, Formula resultVar) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, startX);
        setFormulaWithBrickField(BrickField.VALUE_2, startY);
        setFormulaWithBrickField(BrickField.VALUE_3, endX);
        setFormulaWithBrickField(BrickField.VALUE_4, endY);
        setFormulaWithBrickField(BrickField.TEXT_1, resultVar);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_check_path_exists;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCheckPathExistsAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
