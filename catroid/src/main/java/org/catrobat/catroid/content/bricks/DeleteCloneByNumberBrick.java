package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class DeleteCloneByNumberBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public DeleteCloneByNumberBrick() {
        addAllowedBrickField(BrickField.NUMBER, R.id.brick_delete_clone_by_number_edit);
    }

    public DeleteCloneByNumberBrick(int cloneNumber) {
        this(new Formula(cloneNumber));
    }

    public DeleteCloneByNumberBrick(Formula cloneNumber) {
        this();
        setFormulaWithBrickField(BrickField.NUMBER, cloneNumber);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_delete_clone_by_number;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createDeleteCloneByNumberAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NUMBER)));
    }
}
