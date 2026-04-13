package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetRotationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetRotationBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_set_rotation_id);
        addAllowedBrickField(BrickField.ROTATION, R.id.brick_fast2d_set_rotation_angle);
    }

    public Fast2DSetRotationBrick(String id, Double angle) {
        this(new Formula(id), new Formula(angle));
    }

    public Fast2DSetRotationBrick(Formula id, Formula angle) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.ROTATION, angle);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_rotation;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DSetRotationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.ROTATION)));
    }
}