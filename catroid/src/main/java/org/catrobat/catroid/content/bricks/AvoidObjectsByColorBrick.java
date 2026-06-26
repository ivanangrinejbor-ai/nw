package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AvoidObjectsByColorBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AvoidObjectsByColorBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_avoid_objects_by_color_edit_1);
    }

    public AvoidObjectsByColorBrick(String hexColor) {
        this(new Formula(hexColor));
    }

    public AvoidObjectsByColorBrick(Formula hexColor) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, hexColor);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_avoid_objects_by_color;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAvoidObjectsByColorAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
