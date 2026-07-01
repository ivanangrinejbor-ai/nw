package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ObjectLookAtBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public ObjectLookAtBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_object_look_at_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_object_look_at_x);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_object_look_at_y);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_object_look_at_z);
    }

    public ObjectLookAtBrick(String id, double x, double y, double z) {
        this(new Formula(id), new Formula(x), new Formula(y), new Formula(z));
    }

    public ObjectLookAtBrick(Formula id, Formula x, Formula y, Formula z) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, id);
        setFormulaWithBrickField(BrickField.VALUE_2, x);
        setFormulaWithBrickField(BrickField.VALUE_3, y);
        setFormulaWithBrickField(BrickField.VALUE_4, z);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_object_look_at;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createObjectLookAtAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4)
                ));
    }
}