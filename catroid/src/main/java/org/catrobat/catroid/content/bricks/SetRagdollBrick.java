package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

/**
 * Brick: Set Ragdoll / Зарегдолить
 *
 * Parameter (Formula):
 *   1 (or any non-zero) → enable ragdoll
 *   0                   → disable ragdoll
 *   2                   → ragdoll follow: body is dragged after the target position
 *                          set by scripts (rope-like), see PhysicsLook ragdoll follow.
 *
 * Category: Motion / Physics
 */
public class SetRagdollBrick extends FormulaBrick {

    private static final long serialVersionUID = 1L;

    public SetRagdollBrick() {
        addAllowedBrickField(BrickField.PHYSICS_TOGGLE, R.id.brick_set_ragdoll_edit);
    }

    public SetRagdollBrick(int enable) {
        this(new Formula(enable));
    }

    public SetRagdollBrick(Formula formula) {
        this();
        setFormulaWithBrickField(BrickField.PHYSICS_TOGGLE, formula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_ragdoll;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetRagdollAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.PHYSICS_TOGGLE)));
    }
}
