package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetCollisionFilterBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetCollisionFilterBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_col_filter_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_col_filter_sensor);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_col_filter_group);
    }

    public Fast2DSetCollisionFilterBrick(String id, Double isSensor, Double group) {
        this(new Formula(id), new Formula(isSensor), new Formula(group));
    }

    public Fast2DSetCollisionFilterBrick(Formula id, Formula isSensor, Formula group) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, isSensor);
        setFormulaWithBrickField(BrickField.Y_POSITION, group);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_collision_filter;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DSetCollisionFilterAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION)));
    }
}