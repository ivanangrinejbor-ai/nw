package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RemoveJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RemoveJointBrick() {
        addAllowedBrickField(BrickField.JOINT_NAME, R.id.brick_remove_joint_name_edit);
    }

    public RemoveJointBrick(String jointName) {
        this(new Formula(jointName));
    }

    public RemoveJointBrick(Formula jointNameFormula) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_NAME, jointNameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_remove_joint;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createRemoveJointAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.JOINT_NAME))
        );
    }
}