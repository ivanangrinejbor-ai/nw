package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetSoundInstancePitchBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetSoundInstancePitchBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.sound_inst_name_p);
        addAllowedBrickField(BrickField.PITCH, R.id.sound_inst_pitch);
    }

    public SetSoundInstancePitchBrick(String name, float pitch) {
        this(new Formula(name), new Formula(pitch));
    }

    public SetSoundInstancePitchBrick(Formula name, Formula pitch) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.PITCH, pitch);
    }

    @Override
    public int getViewResource() { return R.layout.brick_sound_pitch; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetSoundInstancePitchAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.PITCH)
        ));
    }
}