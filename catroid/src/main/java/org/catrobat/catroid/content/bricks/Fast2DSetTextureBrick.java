package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetTextureBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public Fast2DSetTextureBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_set_texture_id);
        addAllowedBrickField(BrickField.STRING, R.id.brick_fast2d_set_texture_file);
    }

    public Fast2DSetTextureBrick(String id, String filePath) {
        this(new Formula(id), new Formula(filePath));
    }

    public Fast2DSetTextureBrick(Formula id, Formula filePath) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.STRING, filePath);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_fast2d_set_texture;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFast2DSetTextureAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.STRING)));
    }
}