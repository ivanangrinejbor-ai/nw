package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetZIndexBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetZIndexBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_set_z_id);
        addAllowedBrickField(BrickField.VIBRATE_DURATION, R.id.brick_fast2d_set_z_value);
    }

    public Fast2DSetZIndexBrick(String id, Double z) {
        this(new Formula(id), new Formula(z));
    }

    public Fast2DSetZIndexBrick(Formula id, Formula z) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.VIBRATE_DURATION, z);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_z;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DSetZIndexAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.VIBRATE_DURATION)));
    }
}