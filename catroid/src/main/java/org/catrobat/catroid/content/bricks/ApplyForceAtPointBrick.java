package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyForceAtPointBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    public ApplyForceAtPointBrick() {
        addAllowedBrickField(BrickField.PHYSICS_FORCE_X, R.id.brick_force_at_point_fx);
        addAllowedBrickField(BrickField.PHYSICS_FORCE_Y, R.id.brick_force_at_point_fy);
        addAllowedBrickField(BrickField.PHYSICS_POINT_X, R.id.brick_force_at_point_px);
        addAllowedBrickField(BrickField.PHYSICS_POINT_Y, R.id.brick_force_at_point_py);
    }
    public ApplyForceAtPointBrick(double fx, double fy, double px, double py) {
        this(new Formula(fx), new Formula(fy), new Formula(px), new Formula(py));
    }
    public ApplyForceAtPointBrick(Formula fx, Formula fy, Formula px, Formula py) {
        this();
        setFormulaWithBrickField(BrickField.PHYSICS_FORCE_X, fx);
        setFormulaWithBrickField(BrickField.PHYSICS_FORCE_Y, fy);
        setFormulaWithBrickField(BrickField.PHYSICS_POINT_X, px);
        setFormulaWithBrickField(BrickField.PHYSICS_POINT_Y, py);
    }
    @Override public int getViewResource() { return R.layout.brick_apply_force_at_point; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction seq) {
        seq.addAction(sprite.getActionFactory().createApplyForceAtPointAction(sprite, seq,
                getFormulaWithBrickField(BrickField.PHYSICS_FORCE_X),
                getFormulaWithBrickField(BrickField.PHYSICS_FORCE_Y),
                getFormulaWithBrickField(BrickField.PHYSICS_POINT_X),
                getFormulaWithBrickField(BrickField.PHYSICS_POINT_Y)));
    }
}
