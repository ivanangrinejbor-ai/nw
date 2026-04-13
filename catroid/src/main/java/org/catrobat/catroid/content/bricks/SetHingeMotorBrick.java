package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetHingeMotorBrick extends FormulaBrick {
    public SetHingeMotorBrick() {
        addAllowedBrickField(BrickField.CONSTRAINT_ID, R.id.brick_motor_id);
        addAllowedBrickField(BrickField.MOTOR_TARGET, R.id.brick_motor_target);
        addAllowedBrickField(BrickField.MOTOR_MAX_FORCE, R.id.brick_motor_max);
    }

    public SetHingeMotorBrick(String id, double angle, double force) {
        this();
        setFormulaWithBrickField(BrickField.CONSTRAINT_ID, new Formula(id));
        setFormulaWithBrickField(BrickField.MOTOR_TARGET, new Formula(angle));
        setFormulaWithBrickField(BrickField.MOTOR_MAX_FORCE, new Formula(force));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_threed_hinge_motor;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetHingeMotorAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.CONSTRAINT_ID),
                getFormulaWithBrickField(BrickField.MOTOR_TARGET),
                getFormulaWithBrickField(BrickField.MOTOR_MAX_FORCE)
        ));
    }
}