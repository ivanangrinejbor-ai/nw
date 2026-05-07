package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetBufferCamera3DBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetBufferCamera3DBrick() {
        // Регистрируем все 8 полей
        addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_cam_3d_name);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_buffer_cam_3d_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_buffer_cam_3d_y);
        addAllowedBrickField(BrickField.Z_POSITION, R.id.brick_buffer_cam_3d_z);
        addAllowedBrickField(BrickField.DEGREES, R.id.brick_buffer_cam_3d_yaw);
        addAllowedBrickField(BrickField.DEGREES_BY, R.id.brick_buffer_cam_3d_pitch);
        addAllowedBrickField(BrickField.ROTATION, R.id.brick_buffer_cam_3d_roll);
        addAllowedBrickField(BrickField.SIZE, R.id.brick_buffer_cam_3d_fov);
    }

    public SetBufferCamera3DBrick(Formula name, Formula x, Formula y, Formula z,
                                  Formula yaw, Formula pitch, Formula roll, Formula fov) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
        setFormulaWithBrickField(BrickField.Z_POSITION, z);
        setFormulaWithBrickField(BrickField.DEGREES, yaw);
        setFormulaWithBrickField(BrickField.DEGREES_BY, pitch);
        setFormulaWithBrickField(BrickField.ROTATION, roll);
        setFormulaWithBrickField(BrickField.SIZE, fov);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_buffer_cam_3d;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetBufferCamera3DAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.Z_POSITION),
                getFormulaWithBrickField(BrickField.DEGREES),
                getFormulaWithBrickField(BrickField.DEGREES_BY),
                getFormulaWithBrickField(BrickField.ROTATION),
                getFormulaWithBrickField(BrickField.SIZE)
        ));
    }
}