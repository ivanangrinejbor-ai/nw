package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CheckEndOfPathBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CheckEndOfPathBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_check_end_of_path_edit_1);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_check_end_of_path_edit_2);
    }

    public CheckEndOfPathBrick(String spriteName, String resultVar) {
        this(new Formula(spriteName), new Formula(resultVar));
    }

    public CheckEndOfPathBrick(Formula spriteName, Formula resultVar) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
        setFormulaWithBrickField(BrickField.TEXT_2, resultVar);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_check_end_of_path;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCheckEndOfPathAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2)));
    }
}
