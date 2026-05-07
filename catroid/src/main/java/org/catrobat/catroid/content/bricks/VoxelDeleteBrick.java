package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelDeleteBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelDeleteBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_delete_edit_id);
    }

    public VoxelDeleteBrick(Formula id) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_delete;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelDeleteAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME)
        ));
    }
}
