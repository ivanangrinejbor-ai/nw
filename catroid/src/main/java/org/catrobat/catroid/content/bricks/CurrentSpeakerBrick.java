package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class CurrentSpeakerBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CurrentSpeakerBrick() {
        addAllowedBrickField(BrickField.STRING, R.id.brick_current_speaker_edit);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_current_speaker;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCurrentSpeakerAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.STRING)));
    }
}
