package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetTextureTilingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetTextureTilingBrick() {
        addAllowedBrickField(BrickField.OBJECT_ID, R.id.tiling_obj_id);
        addAllowedBrickField(BrickField.X, R.id.tiling_u);
        addAllowedBrickField(BrickField.Y, R.id.tiling_v);
    }

    public SetTextureTilingBrick(String objId, float u, float v) {
        this(new Formula(objId), new Formula(u), new Formula(v));
    }

    public SetTextureTilingBrick(Formula objId, Formula u, Formula v) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_ID, objId);
        setFormulaWithBrickField(BrickField.X, u);
        setFormulaWithBrickField(BrickField.Y, v);
    }

    @Override
    public int getViewResource() { return R.layout.brick_texture_tiling; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetTextureTilingAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.OBJECT_ID),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y)
        ));
    }
}