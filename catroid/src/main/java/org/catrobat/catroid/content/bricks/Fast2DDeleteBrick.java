package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DDeleteBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DDeleteBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_delete_id);
    }

    public Fast2DDeleteBrick(String id) {
        this(new Formula(id));
    }

    public Fast2DDeleteBrick(Formula id) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_delete;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DDeleteAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME)));
    }
}