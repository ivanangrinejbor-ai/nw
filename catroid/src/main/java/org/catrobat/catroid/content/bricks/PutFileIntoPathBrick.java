package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PutFileIntoPathBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PutFileIntoPathBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_put_file_into_path_edit_file);
        addAllowedBrickField(BrickField.FILE_URL, R.id.brick_put_file_into_path_edit_path);
    }

    public PutFileIntoPathBrick(String sourceFileName, String destPath) {
        this(new Formula(sourceFileName), new Formula(destPath));
    }

    public PutFileIntoPathBrick(Formula sourceFileName, Formula destPath) {
        this();
        setFormulaWithBrickField(BrickField.NAME, sourceFileName);
        setFormulaWithBrickField(BrickField.FILE_URL, destPath);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_put_file_into_path;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.FILE_URL)));
    }
}
