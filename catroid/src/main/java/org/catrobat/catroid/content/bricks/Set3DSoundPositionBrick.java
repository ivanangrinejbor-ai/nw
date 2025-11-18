package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Set3DSoundPositionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Set3DSoundPositionBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_set_3d_sound_name);
        addAllowedBrickField(BrickField.VALUE_X, R.id.brick_set_3d_sound_x);
        addAllowedBrickField(BrickField.VALUE_Y, R.id.brick_set_3d_sound_y);
        addAllowedBrickField(BrickField.VALUE_Z, R.id.brick_set_3d_sound_z);
    }

    public Set3DSoundPositionBrick(String name, Integer x, Integer y, Integer z) {
        this(new Formula(name), new Formula(x), new Formula(y), new Formula(z));
    }

    public Set3DSoundPositionBrick(Formula name, Formula x, Formula y, Formula z) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.VALUE_X, x);
        setFormulaWithBrickField(BrickField.VALUE_Y, y);
        setFormulaWithBrickField(BrickField.VALUE_Z, z);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_3d_sound_position;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSet3DSoundPositionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.VALUE_X),
                        getFormulaWithBrickField(BrickField.VALUE_Y),
                        getFormulaWithBrickField(BrickField.VALUE_Z)
                ));
    }
}