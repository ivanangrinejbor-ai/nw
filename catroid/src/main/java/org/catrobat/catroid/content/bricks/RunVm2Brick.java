package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RunVm2Brick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RunVm2Brick() {
        addAllowedBrickField(BrickField.VM_ARGUMENTS, R.id.brick_run_vm_arguments_edit_text);
    }

    public RunVm2Brick(String arguments) {
        this(new Formula(arguments));
    }

    public RunVm2Brick(Formula argumentsFormula) {
        this();
        setFormulaWithBrickField(BrickField.VM_ARGUMENTS, argumentsFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_run_vm2;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createRunVm2Action(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VM_ARGUMENTS)
                ));
    }
}