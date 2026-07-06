package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PutFileIntoFolderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PutFileIntoFolderBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_put_file_into_folder_edit_file);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_put_file_into_folder_edit_folder);
    }

    public PutFileIntoFolderBrick(String sourceFileName, String folderName) {
        this(new Formula(sourceFileName), new Formula(folderName));
    }

    public PutFileIntoFolderBrick(Formula sourceFileName, Formula folderName) {
        this();
        setFormulaWithBrickField(BrickField.NAME, sourceFileName);
        setFormulaWithBrickField(BrickField.TEXT, folderName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_put_file_into_folder;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
