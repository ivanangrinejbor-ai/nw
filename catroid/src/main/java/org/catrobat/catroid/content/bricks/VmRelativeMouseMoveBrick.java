package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VmRelativeMouseMoveBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VmRelativeMouseMoveBrick() {
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_vm_mouse_edit_dx);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_vm_mouse_edit_dy);
        addAllowedBrickField(BrickField.MASK, R.id.brick_vm_mouse_mask);
    }

    public VmRelativeMouseMoveBrick(int dx, int dy, int mask) {
        this(new Formula(dx), new Formula(dy), new Formula(mask));
    }

    public VmRelativeMouseMoveBrick(Formula dx, Formula dy, Formula mask) {
        this();
        setFormulaWithBrickField(BrickField.X_POSITION, dx);
        setFormulaWithBrickField(BrickField.Y_POSITION, dy);
        setFormulaWithBrickField(BrickField.MASK, mask);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_vm_mouse_relative;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createVmRelativeMouseMoveAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION),
                        getFormulaWithBrickField(BrickField.MASK)));
    }
}