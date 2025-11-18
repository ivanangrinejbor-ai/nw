package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetGlobalSoundVolumeBrick extends FormulaBrick {
    public SetGlobalSoundVolumeBrick() {
        addAllowedBrickField(BrickField.VOLUME, R.id.brick_set_global_sound_volume_edit);
    }

    public SetGlobalSoundVolumeBrick(double volume) {
        this(new Formula(volume));
    }

    public SetGlobalSoundVolumeBrick(Formula volume) {
        this();
        setFormulaWithBrickField(BrickField.VOLUME, volume);
    }

    @Override public int getViewResource() { return R.layout.brick_set_global_sound_volume; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetGlobalSoundVolumeAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VOLUME)
        ));
    }
}