package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

/**
 * Sets the master game volume (0-100%).
 * When active, ALL other volume blocks (SetVolumeTo, ChangeVolumeBy,
 * SetSoundVolume, SetGlobalSoundVolume) are IGNORED.
 * Appears in both Sound and Control categories.
 */
public class SetGameVolumeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetGameVolumeBrick() {
        addAllowedBrickField(BrickField.VOLUME, R.id.brick_set_game_volume_edit);
    }

    public SetGameVolumeBrick(double volume) {
        this(new Formula(volume));
    }

    public SetGameVolumeBrick(Formula volume) {
        this();
        setFormulaWithBrickField(BrickField.VOLUME, volume);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_game_volume;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetGameVolumeAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VOLUME)));
    }
}
