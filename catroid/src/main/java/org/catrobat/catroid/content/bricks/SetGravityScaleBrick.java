package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetGravityScaleBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    public SetGravityScaleBrick() { addAllowedBrickField(BrickField.PHYSICS_GRAVITY_SCALE, R.id.brick_set_gravity_scale_edit); }
    public SetGravityScaleBrick(double v) { this(new Formula(v)); }
    public SetGravityScaleBrick(Formula v) { this(); setFormulaWithBrickField(BrickField.PHYSICS_GRAVITY_SCALE, v); }
    @Override public int getViewResource() { return R.layout.brick_set_gravity_scale; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction seq) {
        seq.addAction(sprite.getActionFactory().createSetGravityScaleAction(sprite, seq, getFormulaWithBrickField(BrickField.PHYSICS_GRAVITY_SCALE)));
    }
}
