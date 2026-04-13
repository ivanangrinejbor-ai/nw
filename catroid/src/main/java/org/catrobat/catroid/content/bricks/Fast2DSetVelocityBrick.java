package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetVelocityBrick extends FormulaBrick {
    public Fast2DSetVelocityBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_vel_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_vel_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_vel_y);
    }
    public Fast2DSetVelocityBrick(String id, Double vx, Double vy) {
        this(new Formula(id), new Formula(vx), new Formula(vy));
    }
    public Fast2DSetVelocityBrick(Formula id, Formula vx, Formula vy) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, vx);
        setFormulaWithBrickField(BrickField.Y_POSITION, vy);
    }
    @Override public int getViewResource() { return R.layout.brick_fast2d_set_velocity; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DSetVelocityAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.X_POSITION), getFormulaWithBrickField(BrickField.Y_POSITION)));
    }
}