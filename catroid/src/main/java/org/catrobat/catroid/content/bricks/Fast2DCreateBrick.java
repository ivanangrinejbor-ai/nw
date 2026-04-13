package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DCreateBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DCreateBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_create_edit_id);
    }

    public Fast2DCreateBrick(String entityId) {
        this(new Formula(entityId));
    }

    public Fast2DCreateBrick(Formula entityIdFormula) {
        this();
        setFormulaWithBrickField(BrickField.NAME, entityIdFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_create;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DCreateAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME)));
    }
}