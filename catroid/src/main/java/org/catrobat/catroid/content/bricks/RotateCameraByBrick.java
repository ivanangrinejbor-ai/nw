package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RotateCameraByBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RotateCameraByBrick() {
        addAllowedBrickField(BrickField.PITCH, R.id.rotate_pitch_edit);
        addAllowedBrickField(BrickField.YAW, R.id.rotate_yaw_edit);
        addAllowedBrickField(BrickField.ROLL, R.id.rotate_roll_edit);
    }

    public RotateCameraByBrick(float pitch, float yaw, float roll) {
        this(new Formula(pitch), new Formula(yaw), new Formula(roll));
    }

    public RotateCameraByBrick(Formula pitch, Formula yaw, Formula roll) {
        this();
        setFormulaWithBrickField(BrickField.PITCH, pitch);
        setFormulaWithBrickField(BrickField.YAW, yaw);
        setFormulaWithBrickField(BrickField.ROLL, roll);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_rotate_camera_by;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createRotateCameraByAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.PITCH),
                getFormulaWithBrickField(BrickField.YAW),
                getFormulaWithBrickField(BrickField.ROLL)
        ));
    }
}