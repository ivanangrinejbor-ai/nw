package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RemoveParentBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RemoveParentBrick() {
        addAllowedBrickField(BrickField.CHILD_OBJECT, R.id.brick_remove_parent_object_edit);
    }

    public RemoveParentBrick(String childObjectName) {
        this(new Formula(childObjectName));
    }

    public RemoveParentBrick(Formula childObjectFormula) {
        this();
        setFormulaWithBrickField(BrickField.CHILD_OBJECT, childObjectFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_parent;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createRemoveParentAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.CHILD_OBJECT))
        );
    }
}