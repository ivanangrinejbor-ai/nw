package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CopyProjectFileToPathBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CopyProjectFileToPathBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_copy_project_file_to_path_edit_file);
        addAllowedBrickField(BrickField.VALUE, R.id.brick_copy_project_file_to_path_edit_path);
    }

    public CopyProjectFileToPathBrick(String sourceFileName, String path) {
        this(new Formula(sourceFileName), new Formula(path));
    }

    public CopyProjectFileToPathBrick(Formula sourceFileName, Formula path) {
        this();
        setFormulaWithBrickField(BrickField.NAME, sourceFileName);
        setFormulaWithBrickField(BrickField.VALUE, path);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_copy_project_file_to_path;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCopyProjectFileToPathAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.VALUE)));
    }
}
