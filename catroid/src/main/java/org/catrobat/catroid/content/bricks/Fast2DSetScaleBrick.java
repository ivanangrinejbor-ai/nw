package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetScaleBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetScaleBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_set_scale_id);
        addAllowedBrickField(BrickField.X_SCALE, R.id.brick_fast2d_set_scale_x);
        addAllowedBrickField(BrickField.Y_SCALE, R.id.brick_fast2d_set_scale_y);
    }

    public Fast2DSetScaleBrick(String id, Double sx, Double sy) {
        this(new Formula(id), new Formula(sx), new Formula(sy));
    }

    public Fast2DSetScaleBrick(Formula id, Formula sx, Formula sy) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_SCALE, sx);
        setFormulaWithBrickField(BrickField.Y_SCALE, sy);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_scale;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DSetScaleAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.X_SCALE),
                        getFormulaWithBrickField(BrickField.Y_SCALE)));
    }
}