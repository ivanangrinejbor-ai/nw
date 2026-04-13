package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetAngularVelocityBrick extends FormulaBrick {
    public Fast2DSetAngularVelocityBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_ang_vel_id);
        addAllowedBrickField(BrickField.ROTATION, R.id.brick_fast2d_ang_vel_v);
    }
    public Fast2DSetAngularVelocityBrick(String id, Double v) { this(new Formula(id), new Formula(v)); }
    public Fast2DSetAngularVelocityBrick(Formula id, Formula v) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.ROTATION, v);
    }
    @Override public int getViewResource() { return R.layout.brick_fast2d_set_angular_velocity; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DSetAngularVelocityAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.ROTATION)));
    }
}