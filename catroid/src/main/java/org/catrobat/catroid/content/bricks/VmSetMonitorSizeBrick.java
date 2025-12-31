package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VmSetMonitorSizeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VmSetMonitorSizeBrick() {
        addAllowedBrickField(BrickField.WIDTH, R.id.brick_vm_monitor_edit_width);
        addAllowedBrickField(BrickField.HEIGHT, R.id.brick_vm_monitor_edit_height);
    }

    public VmSetMonitorSizeBrick(int width, int height) {
        this(new Formula(width), new Formula(height));
    }

    public VmSetMonitorSizeBrick(Formula width, Formula height) {
        this();
        setFormulaWithBrickField(BrickField.WIDTH, width);
        setFormulaWithBrickField(BrickField.HEIGHT, height);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_vm_monitor_size;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createVmSetMonitorSizeAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.WIDTH),
                        getFormulaWithBrickField(BrickField.HEIGHT)));
    }
}