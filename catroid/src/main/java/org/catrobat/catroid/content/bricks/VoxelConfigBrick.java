package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class VoxelConfigBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    // Пустой конструктор для XStream (сериализация)
    public VoxelConfigBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_config_edit_id);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_config_edit_shape);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_config_edit_tx);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_config_edit_ty);
    }

    public VoxelConfigBrick(Formula id, Formula shape, Formula tx, Formula ty) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X, shape);
        setFormulaWithBrickField(BrickField.Y, tx);
        setFormulaWithBrickField(BrickField.Z, ty);
    }

    public VoxelConfigBrick(String id, int shape, int tx, int ty) {
        this(new Formula(id), new Formula(shape), new Formula(tx), new Formula(ty));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_config;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelConfigAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z)
        ));
    }
}
