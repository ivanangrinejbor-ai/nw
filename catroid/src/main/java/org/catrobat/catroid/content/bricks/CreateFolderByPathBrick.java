package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateFolderByPathBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateFolderByPathBrick() {
        addAllowedBrickField(BrickField.PATH, R.id.brick_create_folder_by_path_edit_path);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_create_folder_by_path_edit_text);
    }

    public CreateFolderByPathBrick(String path, String folderName) {
        this(new Formula(path), new Formula(folderName));
    }

    public CreateFolderByPathBrick(Formula path, Formula folderName) {
        this();
        setFormulaWithBrickField(BrickField.PATH, path);
        setFormulaWithBrickField(BrickField.TEXT, folderName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_folder_by_path;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateFolderByPathAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.PATH),
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
