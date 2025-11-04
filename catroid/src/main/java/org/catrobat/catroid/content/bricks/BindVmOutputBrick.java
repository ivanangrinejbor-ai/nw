package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class BindVmOutputBrick extends UserVariableBrickWithFormula {
    private static final long serialVersionUID = 1L;

    public BindVmOutputBrick() {
        this(null);
    }

    public BindVmOutputBrick(UserVariable variable) {
        this.userVariable = variable;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_bind_vm_output;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_bind_vm_output_variable_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createBindVmOutputAction(
                        userVariable
                ));
    }
}