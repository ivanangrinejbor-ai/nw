package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class DeleteFolderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public DeleteFolderBrick() {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_delete_folder_edit_text);
    }

    public DeleteFolderBrick(String value) {
        this(new Formula(value));
    }

    public DeleteFolderBrick(Formula formula) {
        this();
        setFormulaWithBrickField(BrickField.TEXT, formula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_delete_folder;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createDeleteFolderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
