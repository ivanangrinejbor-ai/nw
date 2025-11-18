package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class StopSoundBrick2 extends FormulaBrick {
    public StopSoundBrick2() {
        addAllowedBrickField(BrickField.INSTANCE_NAME, R.id.brick_stop_sound_3d_instance_edit);
    }

    public StopSoundBrick2(String instanceName) {
        this(new Formula(instanceName));
    }

    public StopSoundBrick2(Formula instanceName) {
        this();
        setFormulaWithBrickField(BrickField.INSTANCE_NAME, instanceName);
    }

    @Override public int getViewResource() { return R.layout.brick_stop_sound_3d; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStopSoundAction2(sprite, sequence,
                getFormulaWithBrickField(BrickField.INSTANCE_NAME)
        ));
    }
}