package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateFolderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateFolderBrick() {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_create_folder_edit_text);
    }

    public CreateFolderBrick(String value) {
        this(new Formula(value));
    }

    public CreateFolderBrick(Formula formula) {
        this();
        setFormulaWithBrickField(BrickField.TEXT, formula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_folder;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateFolderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
