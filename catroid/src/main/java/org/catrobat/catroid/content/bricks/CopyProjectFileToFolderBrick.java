package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CopyProjectFileToFolderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CopyProjectFileToFolderBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_copy_project_file_to_folder_edit_file);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_copy_project_file_to_folder_edit_folder);
    }

    public CopyProjectFileToFolderBrick(String sourceFileName, String folderName) {
        this(new Formula(sourceFileName), new Formula(folderName));
    }

    public CopyProjectFileToFolderBrick(Formula sourceFileName, Formula folderName) {
        this();
        setFormulaWithBrickField(BrickField.NAME, sourceFileName);
        setFormulaWithBrickField(BrickField.TEXT, folderName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_copy_project_file_to_folder;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCopyProjectFileToFolderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
