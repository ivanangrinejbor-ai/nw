package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class FindPathToObjectBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public FindPathToObjectBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_find_path_to_object_edit_1);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_find_path_to_object_edit_2);
    }

    public FindPathToObjectBrick(String fromSprite, String toSprite) {
        this(new Formula(fromSprite), new Formula(toSprite));
    }

    public FindPathToObjectBrick(Formula fromSprite, Formula toSprite) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, fromSprite);
        setFormulaWithBrickField(BrickField.TEXT_2, toSprite);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_find_path_to_object;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFindPathToObjectAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2)));
    }
}
