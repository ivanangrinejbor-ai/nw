package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreatePointJointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreatePointJointBrick() {
        addAllowedBrickField(BrickField.JOINT_NAME, R.id.brick_joint_name_edit);
        addAllowedBrickField(BrickField.OBJECT_A, R.id.brick_joint_object_a_edit);
        addAllowedBrickField(BrickField.OBJECT_B, R.id.brick_joint_object_b_edit);
        addAllowedBrickField(BrickField.PIVOT_A_X, R.id.brick_joint_pivot_a_x);
        addAllowedBrickField(BrickField.PIVOT_A_Y, R.id.brick_joint_pivot_a_y);
        addAllowedBrickField(BrickField.PIVOT_A_Z, R.id.brick_joint_pivot_a_z);
        addAllowedBrickField(BrickField.PIVOT_B_X, R.id.brick_joint_pivot_b_x);
        addAllowedBrickField(BrickField.PIVOT_B_Y, R.id.brick_joint_pivot_b_y);
        addAllowedBrickField(BrickField.PIVOT_B_Z, R.id.brick_joint_pivot_b_z);
    }

    public CreatePointJointBrick(String jointName, String objectA, String objectB) {
        this();
        setFormulaWithBrickField(BrickField.JOINT_NAME, new Formula(jointName));
        setFormulaWithBrickField(BrickField.OBJECT_A, new Formula(objectA));
        setFormulaWithBrickField(BrickField.OBJECT_B, new Formula(objectB));
        setFormulaWithBrickField(BrickField.PIVOT_A_X, new Formula(0));
        setFormulaWithBrickField(BrickField.PIVOT_A_Y, new Formula(0));
        setFormulaWithBrickField(BrickField.PIVOT_A_Z, new Formula(0));
        setFormulaWithBrickField(BrickField.PIVOT_B_X, new Formula(0));
        setFormulaWithBrickField(BrickField.PIVOT_B_Y, new Formula(0));
        setFormulaWithBrickField(BrickField.PIVOT_B_Z, new Formula(0));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_point_joint;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCreatePointJointAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.JOINT_NAME),
                getFormulaWithBrickField(BrickField.OBJECT_A),
                getFormulaWithBrickField(BrickField.OBJECT_B),
                getFormulaWithBrickField(BrickField.PIVOT_A_X),
                getFormulaWithBrickField(BrickField.PIVOT_A_Y),
                getFormulaWithBrickField(BrickField.PIVOT_A_Z),
                getFormulaWithBrickField(BrickField.PIVOT_B_X),
                getFormulaWithBrickField(BrickField.PIVOT_B_Y),
                getFormulaWithBrickField(BrickField.PIVOT_B_Z)
        ));
    }
}