package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelCreateWorldBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelCreateWorldBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_world_id);

        addAllowedBrickField(BrickField.X, R.id.brick_voxel_size_x);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_size_y);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_size_z);

        addAllowedBrickField(BrickField.X_1, R.id.brick_voxel_world_x);
        addAllowedBrickField(BrickField.Y_1, R.id.brick_voxel_world_y);
        addAllowedBrickField(BrickField.Z_1, R.id.brick_voxel_world_z);
    }

    public VoxelCreateWorldBrick(Formula id, Formula sx, Formula sy, Formula sz, Formula wx, Formula wy, Formula wz) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X, sx);
        setFormulaWithBrickField(BrickField.Y, sy);
        setFormulaWithBrickField(BrickField.Z, sz);
        setFormulaWithBrickField(BrickField.X_1, wx);
        setFormulaWithBrickField(BrickField.Y_1, wy);
        setFormulaWithBrickField(BrickField.Z_1, wz);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_create_world;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelCreateWorldAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z),
                getFormulaWithBrickField(BrickField.X_1),
                getFormulaWithBrickField(BrickField.Y_1),
                getFormulaWithBrickField(BrickField.Z_1)
        ));
    }
}
