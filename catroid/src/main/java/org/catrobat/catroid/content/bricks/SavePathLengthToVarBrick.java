package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SavePathLengthToVarBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SavePathLengthToVarBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_save_path_length_to_var_edit_1);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_save_path_length_to_var_edit_2);
    }

    public SavePathLengthToVarBrick(String spriteName, String variableName) {
        this(new Formula(spriteName), new Formula(variableName));
    }

    public SavePathLengthToVarBrick(Formula spriteName, Formula variableName) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
        setFormulaWithBrickField(BrickField.TEXT_2, variableName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_save_path_length_to_var;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSavePathLengthToVarAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2)));
    }
}
