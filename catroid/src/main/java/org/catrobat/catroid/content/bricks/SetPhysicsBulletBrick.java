package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetPhysicsBulletBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    public SetPhysicsBulletBrick() { addAllowedBrickField(BrickField.PHYSICS_TOGGLE, R.id.brick_set_bullet_edit); }
    public SetPhysicsBulletBrick(double v) { this(new Formula(v)); }
    public SetPhysicsBulletBrick(Formula v) { this(); setFormulaWithBrickField(BrickField.PHYSICS_TOGGLE, v); }
    @Override public int getViewResource() { return R.layout.brick_set_bullet; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction seq) {
        seq.addAction(sprite.getActionFactory().createSetBulletAction(sprite, seq, getFormulaWithBrickField(BrickField.PHYSICS_TOGGLE)));
    }
}
