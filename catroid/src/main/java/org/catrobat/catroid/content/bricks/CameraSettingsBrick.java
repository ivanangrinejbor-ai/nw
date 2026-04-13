package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CameraSettingsBrick extends FormulaBrick {
    public CameraSettingsBrick() {
        addAllowedBrickField(BrickField.FOV, R.id.camera_fov_edit);
        addAllowedBrickField(BrickField.INTENSITY, R.id.camera_shake_int_edit);
        addAllowedBrickField(BrickField.DURATION, R.id.camera_shake_dur_edit);
    }

    public CameraSettingsBrick(float fov, float shakeInt, float shakeDur) {
        this(new Formula(fov), new Formula(shakeInt), new Formula(shakeDur));
    }

    public CameraSettingsBrick(Formula fov, Formula shakeInt, Formula shakeDur) {
        this();
        setFormulaWithBrickField(BrickField.FOV, fov);
        setFormulaWithBrickField(BrickField.INTENSITY, shakeInt);
        setFormulaWithBrickField(BrickField.DURATION, shakeDur);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_camera_settings;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCameraSettingsAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FOV),
                getFormulaWithBrickField(BrickField.INTENSITY),
                getFormulaWithBrickField(BrickField.DURATION)));
    }
}