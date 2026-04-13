package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RunChip8Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RunChip8Brick() {
        addAllowedBrickField(Brick.BrickField.TEXT, R.id.brick_chip8_edit_file);
    }

    public RunChip8Brick(String filename) {
        this(new Formula(filename));
    }

    public RunChip8Brick(Formula filenameFormula) {
        this();
        setFormulaWithBrickField(BrickField.TEXT, filenameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_run_chip8;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRunChip8Action(sprite, sequence, getFormulaWithBrickField(BrickField.TEXT)));
    }
}