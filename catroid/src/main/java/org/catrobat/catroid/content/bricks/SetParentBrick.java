package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetParentBrick extends FormulaBrick {
    public SetParentBrick() {
        addAllowedBrickField(BrickField.CHILD_OBJECT, R.id.brick_set_parent_child_edit);
        addAllowedBrickField(BrickField.PARENT_OBJECT, R.id.brick_set_parent_parent_edit);
    }

    public SetParentBrick(String child, String parent) {
        this(new Formula(child), new Formula(parent));
    }

    public SetParentBrick(Formula child, Formula parent) {
        this();
        setFormulaWithBrickField(BrickField.CHILD_OBJECT, child);
        setFormulaWithBrickField(BrickField.PARENT_OBJECT, parent);
    }

    @Override
    public int getViewResource() { return R.layout.brick_set_parent; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetParentAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.CHILD_OBJECT),
                getFormulaWithBrickField(BrickField.PARENT_OBJECT))
        );
    }
}