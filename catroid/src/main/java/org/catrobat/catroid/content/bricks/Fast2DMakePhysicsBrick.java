package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class Fast2DMakePhysicsBrick extends FormulaBrick {
    public Fast2DMakePhysicsBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_fast2d_phys_id);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_fast2d_phys_dyn);
        addAllowedBrickField(BrickField.STRING, R.id.brick_fast2d_phys_shape);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_fast2d_phys_den);
        addAllowedBrickField(BrickField.ROTATION, R.id.brick_fast2d_phys_fric);
        addAllowedBrickField(BrickField.SIZE, R.id.brick_fast2d_phys_bounce);
    }
    public Fast2DMakePhysicsBrick(String id, Double dyn, String shape, Double den, Double fric, Double bnc) {
        this(new Formula(id), new Formula(dyn), new Formula(shape), new Formula(den), new Formula(fric), new Formula(bnc));
    }
    public Fast2DMakePhysicsBrick(Formula id, Formula dyn, Formula shape, Formula den, Formula fric, Formula bnc) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X_POSITION, dyn);
        setFormulaWithBrickField(BrickField.STRING, shape);
        setFormulaWithBrickField(BrickField.Y_POSITION, den);
        setFormulaWithBrickField(BrickField.ROTATION, fric);
        setFormulaWithBrickField(BrickField.SIZE, bnc);
    }
    @Override public int getViewResource() { return R.layout.brick_fast2d_make_physics; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createFast2DMakePhysicsAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.STRING), getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.ROTATION), getFormulaWithBrickField(BrickField.SIZE)));
    }
}