package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class DialogueRunningBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public DialogueRunningBrick() {
        addAllowedBrickField(BrickField.STRING, R.id.brick_dialogue_running_edit);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_dialogue_running;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createDialogueRunningAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.STRING)));
    }
}
