package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetStateBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetStateBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_set_state_machine_edit);
        addAllowedBrickField(BrickField.STRING, R.id.brick_set_state_value_edit);
    }

    public SetStateBrick(String machine, String state) {
        this(new Formula(machine), new Formula(state));
    }

    public SetStateBrick(Formula machine, Formula state) {
        this();
        setFormulaWithBrickField(BrickField.NAME, machine);
        setFormulaWithBrickField(BrickField.STRING, state);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_state;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetStateAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.STRING)));
    }
}
