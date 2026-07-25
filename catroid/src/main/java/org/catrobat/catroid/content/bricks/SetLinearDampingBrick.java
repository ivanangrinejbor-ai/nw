package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetLinearDampingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    public SetLinearDampingBrick() { addAllowedBrickField(BrickField.PHYSICS_DAMPING, R.id.brick_set_linear_damping_edit); }
    public SetLinearDampingBrick(double v) { this(new Formula(v)); }
    public SetLinearDampingBrick(Formula v) { this(); setFormulaWithBrickField(BrickField.PHYSICS_DAMPING, v); }
    @Override public int getViewResource() { return R.layout.brick_set_linear_damping; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction seq) {
        seq.addAction(sprite.getActionFactory().createSetLinearDampingAction(sprite, seq, getFormulaWithBrickField(BrickField.PHYSICS_DAMPING)));
    }
}
