package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DApplyForceBrick extends FormulaBrick {
    public Fast2DApplyForceBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_force_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_force_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_force_y);
    }
    public Fast2DApplyForceBrick(String id, Double x, Double y) { this(new Formula(id), new Formula(x), new Formula(y)); }
    public Fast2DApplyForceBrick(Formula id, Formula x, Formula y) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
    }
    @Override public int getViewResource() { return R.layout.brick_fast2d_apply_force; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DApplyForceAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.X_POSITION), getFormulaWithBrickField(BrickField.Y_POSITION)));
    }
}