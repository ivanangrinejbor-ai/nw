package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AppendToFileBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AppendToFileBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_append_file_name_edit_text);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_append_file_text_edit_text);
    }

    public AppendToFileBrick(String fileName, String text) {
        this(new Formula(fileName), new Formula(text));
    }

    public AppendToFileBrick(Formula fileName, Formula text) {
        this();
        setFormulaWithBrickField(BrickField.NAME, fileName);
        setFormulaWithBrickField(BrickField.TEXT, text);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_append_to_file;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAppendToFileAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT)
                )
        );
    }
}
