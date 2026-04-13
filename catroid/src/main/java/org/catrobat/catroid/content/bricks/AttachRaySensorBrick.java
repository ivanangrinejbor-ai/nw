package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AttachRaySensorBrick extends FormulaBrick {
    public AttachRaySensorBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_attach_ray_name);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_attach_ray_obj);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_attach_ray_off_x);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_attach_ray_off_y);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_attach_ray_off_z);
        addAllowedBrickField(BrickField.VALUE_6, R.id.brick_attach_ray_dir_x);
        addAllowedBrickField(BrickField.VALUE_7, R.id.brick_attach_ray_dir_y);
        addAllowedBrickField(BrickField.VALUE_8, R.id.brick_attach_ray_dir_z);
        addAllowedBrickField(BrickField.VALUE_9, R.id.brick_attach_ray_dist);
    }

    public AttachRaySensorBrick(String ray, String obj, float ofx, float ofy, float ofz, float dirx, float diry, float dirz, float dist) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(ray));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(obj));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(ofx));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(ofy));
        setFormulaWithBrickField(BrickField.VALUE_5, new Formula(ofz));
        setFormulaWithBrickField(BrickField.VALUE_6, new Formula(dirx));
        setFormulaWithBrickField(BrickField.VALUE_7, new Formula(diry));
        setFormulaWithBrickField(BrickField.VALUE_8, new Formula(dirz));
        setFormulaWithBrickField(BrickField.VALUE_9, new Formula(dist));

    }

    @Override
    public int getViewResource() {
        return R.layout.brick_attach_ray_sensor;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createAttachRaySensorAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1), getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3), getFormulaWithBrickField(BrickField.VALUE_4),
                getFormulaWithBrickField(BrickField.VALUE_5), getFormulaWithBrickField(BrickField.VALUE_6),
                getFormulaWithBrickField(BrickField.VALUE_7), getFormulaWithBrickField(BrickField.VALUE_8),
                getFormulaWithBrickField(BrickField.VALUE_9)));
    }
}