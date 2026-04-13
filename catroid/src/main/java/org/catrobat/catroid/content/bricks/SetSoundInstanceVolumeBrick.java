package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetSoundInstanceVolumeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetSoundInstanceVolumeBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.sound_inst_name);
        addAllowedBrickField(BrickField.VOLUME, R.id.sound_inst_vol);
    }

    public SetSoundInstanceVolumeBrick(String name, float vol) {
        this(new Formula(name), new Formula(vol));
    }

    public SetSoundInstanceVolumeBrick(Formula name, Formula vol) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.VOLUME, vol);
    }

    @Override
    public int getViewResource() { return R.layout.brick_sound_volume; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetSoundInstanceVolumeAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.VOLUME)
        ));
    }
}