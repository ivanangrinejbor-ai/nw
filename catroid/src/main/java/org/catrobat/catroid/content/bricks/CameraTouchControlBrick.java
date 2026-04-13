/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CameraTouchControlBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CameraTouchControlBrick() {
        addAllowedBrickField(BrickField.ENABLED, R.id.cam_enabled);
        addAllowedBrickField(BrickField.SENSITIVITY, R.id.cam_sens);
        addAllowedBrickField(BrickField.X, R.id.cam_area_x);
        addAllowedBrickField(BrickField.Y, R.id.cam_area_y);
        addAllowedBrickField(BrickField.WIDTH, R.id.cam_area_w);
        addAllowedBrickField(BrickField.HEIGHT, R.id.cam_area_h);
    }

    public CameraTouchControlBrick(int enabled, float sensitivity, float x, float y, float w, float h) {
        this(new Formula(enabled), new Formula(sensitivity), new Formula(x), new Formula(y), new Formula(w), new Formula(h));
    }

    public CameraTouchControlBrick(Formula enabled, Formula sensitivity, Formula x, Formula y, Formula w, Formula h) {
        this();
        setFormulaWithBrickField(BrickField.ENABLED, enabled);
        setFormulaWithBrickField(BrickField.SENSITIVITY, sensitivity);
        setFormulaWithBrickField(BrickField.X, x);
        setFormulaWithBrickField(BrickField.Y, y);
        setFormulaWithBrickField(BrickField.WIDTH, w);
        setFormulaWithBrickField(BrickField.HEIGHT, h);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_camera_touch_control;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCameraTouchControlAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.ENABLED),
                getFormulaWithBrickField(BrickField.SENSITIVITY),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.WIDTH),
                getFormulaWithBrickField(BrickField.HEIGHT)
        ));
    }
}