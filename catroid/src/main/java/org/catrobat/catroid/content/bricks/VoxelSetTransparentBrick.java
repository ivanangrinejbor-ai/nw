package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelSetTransparentBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelSetTransparentBrick() {
        super();
        addAllowedBrickField(BrickField.VALUE, R.id.brick_voxel_transp_edit_id);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_voxel_transp_edit_value);
    }

    public VoxelSetTransparentBrick(Formula blockId, Formula isTransparent) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, blockId);
        setFormulaWithBrickField(BrickField.VALUE_1, isTransparent);
    }

    public VoxelSetTransparentBrick(int blockId, int isTransparent) {
        this(new Formula(blockId), new Formula(isTransparent));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_set_transparent;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelSetTransparentAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.VALUE),
                getFormulaWithBrickField(BrickField.VALUE_1)
        ));
    }
}
