package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DApplyImpulseBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DApplyImpulseBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_impulse_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_impulse_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_impulse_y);
    }

    public Fast2DApplyImpulseBrick(String id, Double x, Double y) {
        this(new Formula(id), new Formula(x), new Formula(y));
    }

    public Fast2DApplyImpulseBrick(Formula id, Formula x, Formula y) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_apply_impulse;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DApplyImpulseAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION)));
    }
}