package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.VoxelBuildAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelBuildBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelBuildBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_world_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_voxel_atlas_text);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_atlas_width);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_atlas_height);
    }

    public VoxelBuildBrick(Formula id, Formula atlas, Formula w, Formula h) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.TEXT, atlas);
        setFormulaWithBrickField(BrickField.X, w);
        setFormulaWithBrickField(BrickField.Y, h);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_build;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createVoxelBuildAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT),
                        getFormulaWithBrickField(BrickField.X),
                        getFormulaWithBrickField(BrickField.Y)));
    }
}
