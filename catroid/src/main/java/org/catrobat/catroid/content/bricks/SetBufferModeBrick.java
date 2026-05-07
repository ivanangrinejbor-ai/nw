package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetBufferModeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetBufferModeBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_buffer_2d);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_buffer_3d);
    }

    public SetBufferModeBrick(Formula name, Formula r2d, Formula r3d) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.X_POSITION, r2d);
        setFormulaWithBrickField(BrickField.Y_POSITION, r3d);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_buffer_mode;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetBufferModeAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION)
        ));
    }
}