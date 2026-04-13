package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DSetColorBrick extends FormulaBrick {
    public Fast2DSetColorBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_color_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_color_r);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_color_g);
        addAllowedBrickField(BrickField.ROTATION, R.id.brick_fast2d_color_b);
        addAllowedBrickField(BrickField.SIZE, R.id.brick_fast2d_color_a);
    }

    public Fast2DSetColorBrick(String id, Double r, Double g, Double b, Double a) {
        this(new Formula(id), new Formula(r), new Formula(g), new Formula(b), new Formula(a));
    }

    public Fast2DSetColorBrick(Formula id, Formula r, Formula g, Formula b, Formula a) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, r);
        setFormulaWithBrickField(BrickField.Y_POSITION, g);
        setFormulaWithBrickField(BrickField.ROTATION, b);
        setFormulaWithBrickField(BrickField.SIZE, a);
    }

    @Override
    public int getViewResource() { return R.layout.brick_fast2d_set_color; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DSetColorAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.ROTATION),
                getFormulaWithBrickField(BrickField.SIZE)));
    }
}