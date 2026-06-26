package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class FindPathToXYBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public FindPathToXYBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_find_path_to_xy_edit_1);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_find_path_to_xy_edit_2);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_find_path_to_xy_edit_3);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_find_path_to_xy_edit_4);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_find_path_to_xy_edit_5);
    }

    public FindPathToXYBrick(String spriteName, Formula startX, Formula startY, Formula endX, Formula endY) {
        this(new Formula(spriteName), startX, startY, endX, endY);
    }

    public FindPathToXYBrick(Formula spriteName, Formula startX, Formula startY, Formula endX, Formula endY) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
        setFormulaWithBrickField(BrickField.VALUE_1, startX);
        setFormulaWithBrickField(BrickField.VALUE_2, startY);
        setFormulaWithBrickField(BrickField.VALUE_3, endX);
        setFormulaWithBrickField(BrickField.VALUE_4, endY);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_find_path_to_xy;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFindPathToXYAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4)));
    }
}
