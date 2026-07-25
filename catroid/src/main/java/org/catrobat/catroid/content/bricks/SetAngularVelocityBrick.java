package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetAngularVelocityBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    public SetAngularVelocityBrick() { addAllowedBrickField(BrickField.PHYSICS_ANGULAR_VELOCITY, R.id.brick_set_angular_velocity_edit); }
    public SetAngularVelocityBrick(double v) { this(new Formula(v)); }
    public SetAngularVelocityBrick(Formula v) { this(); setFormulaWithBrickField(BrickField.PHYSICS_ANGULAR_VELOCITY, v); }
    @Override public int getViewResource() { return R.layout.brick_set_angular_velocity; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction seq) {
        seq.addAction(sprite.getActionFactory().createSetAngularVelocityAction(sprite, seq, getFormulaWithBrickField(BrickField.PHYSICS_ANGULAR_VELOCITY)));
    }
}
