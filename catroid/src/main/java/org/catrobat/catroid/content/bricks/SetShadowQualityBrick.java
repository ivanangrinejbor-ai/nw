package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetShadowQualityBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetShadowQualityBrick() {
        addAllowedBrickField(BrickField.SHADOW_SIZE, R.id.brick_shadow_size_edit);
        addAllowedBrickField(BrickField.SHADOW_RESOLUTION, R.id.brick_shadow_resolution_edit);
    }

    public SetShadowQualityBrick(double size, double resolution) {
        this(new Formula(size), new Formula(resolution));
    }

    public SetShadowQualityBrick(Formula size, Formula resolution) {
        this();
        setFormulaWithBrickField(BrickField.SHADOW_SIZE, size);
        setFormulaWithBrickField(BrickField.SHADOW_RESOLUTION, resolution);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_shadow_quality;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetShadowQualityAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.SHADOW_SIZE),
                        getFormulaWithBrickField(BrickField.SHADOW_RESOLUTION)));
    }
}