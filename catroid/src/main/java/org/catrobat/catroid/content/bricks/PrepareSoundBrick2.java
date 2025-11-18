package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PrepareSoundBrick2 extends FormulaBrick {
    public PrepareSoundBrick2() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_prepare_sound_file_edit);
        addAllowedBrickField(BrickField.SOUND_NAME, R.id.brick_prepare_sound_name_edit);
    }

    public PrepareSoundBrick2(String fileName, String soundName) {
        this(new Formula(fileName), new Formula(soundName));
    }

    public PrepareSoundBrick2(Formula fileName, Formula soundName) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileName);
        setFormulaWithBrickField(BrickField.SOUND_NAME, soundName);
    }

    @Override public int getViewResource() { return R.layout.brick_prepare_sound2; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createPrepareSound2Action(sprite, sequence,
                getFormulaWithBrickField(BrickField.FILE_NAME),
                getFormulaWithBrickField(BrickField.SOUND_NAME)
        ));
    }
}