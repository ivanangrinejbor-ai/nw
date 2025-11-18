package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PrepareMusicAs3DSoundBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PrepareMusicAs3DSoundBrick() {
        addAllowedBrickField(BrickField.FILE_NAME, R.id.brick_prepare_music_file_edit);
        addAllowedBrickField(BrickField.SOUND_NAME, R.id.brick_prepare_music_name_edit);
    }

    public PrepareMusicAs3DSoundBrick(String fileName, String soundName) {
        this(new Formula(fileName), new Formula(soundName));
    }

    public PrepareMusicAs3DSoundBrick(Formula fileName, Formula soundName) {
        this();
        setFormulaWithBrickField(BrickField.FILE_NAME, fileName);
        setFormulaWithBrickField(BrickField.SOUND_NAME, soundName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_prepare_music;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createPrepareMusicAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FILE_NAME),
                getFormulaWithBrickField(BrickField.SOUND_NAME)
        ));
    }
}