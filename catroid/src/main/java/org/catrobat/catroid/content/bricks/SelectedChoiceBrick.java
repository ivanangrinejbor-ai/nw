package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class SelectedChoiceBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SelectedChoiceBrick() {
        addAllowedBrickField(BrickField.STRING, R.id.brick_selected_choice_edit);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_selected_choice;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSelectedChoiceAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.STRING)));
    }
}
