package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetPositionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetPositionBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_set_pos_edit_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_set_pos_edit_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_set_pos_edit_y);
    }

    public Fast2DSetPositionBrick(String entityId, Double x, Double y) {
        this(new Formula(entityId), new Formula(x), new Formula(y));
    }

    public Fast2DSetPositionBrick(Formula entityId, Formula x, Formula y) {
        this();
        setFormulaWithBrickField(BrickField.NAME, entityId);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_position;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DSetPositionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION)));
    }
}