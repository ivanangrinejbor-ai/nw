package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetCameraBrick extends FormulaBrick {
    public Fast2DSetCameraBrick() {
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_cam_edit_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_cam_edit_y);
        addAllowedBrickField(BrickField.SIZE, R.id.brick_fast2d_cam_edit_zoom);
    }

    public Fast2DSetCameraBrick(Double x, Double y, Double zoom) {
        this(new Formula(x), new Formula(y), new Formula(zoom));
    }

    public Fast2DSetCameraBrick(Formula x, Formula y, Formula zoom) {
        this();
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
        setFormulaWithBrickField(BrickField.SIZE, zoom);
    }

    @Override
    public int getViewResource() { return R.layout.brick_fast2d_set_camera; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DSetCameraAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.SIZE)));
    }
}