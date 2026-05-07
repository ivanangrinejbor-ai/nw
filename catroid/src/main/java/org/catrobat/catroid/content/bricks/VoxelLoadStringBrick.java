package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelLoadStringBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelLoadStringBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_load_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_voxel_load_data);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_load_dx);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_load_dy);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_load_dz);
    }

    public VoxelLoadStringBrick(Formula id, Formula data, Formula dx, Formula dy, Formula dz) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.TEXT, data);
        setFormulaWithBrickField(BrickField.X, dx);
        setFormulaWithBrickField(BrickField.Y, dy);
        setFormulaWithBrickField(BrickField.Z, dz);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_load_string;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelLoadStringAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.TEXT),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z)
        ));
    }
}
