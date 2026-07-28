package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class StartDialogueBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public StartDialogueBrick() {
        addAllowedBrickField(BrickField.DIALOGUE_FILE, R.id.brick_dialogue_file_edit);
    }

    public StartDialogueBrick(String filePath) {
        this(new Formula(filePath));
    }

    public StartDialogueBrick(Formula filePath) {
        this();
        setFormulaWithBrickField(BrickField.DIALOGUE_FILE, filePath);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_start_dialogue;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStartDialogueAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.DIALOGUE_FILE)));
    }
}
