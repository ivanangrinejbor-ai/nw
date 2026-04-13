package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetAnimationSpeedBrick extends FormulaBrick {
    public SetAnimationSpeedBrick() {
        addAllowedBrickField(BrickField.OBJECT_ID, R.id.anim_obj_id);
        addAllowedBrickField(BrickField.SPEED, R.id.anim_speed);
    }
    public SetAnimationSpeedBrick(String id, float speed) {
        this(new Formula(id), new Formula(speed));
    }
    public SetAnimationSpeedBrick(Formula id, Formula speed) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_ID, id);
        setFormulaWithBrickField(BrickField.SPEED, speed);
    }
    @Override public int getViewResource() { return R.layout.brick_set_animation_speed; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetAnimationSpeedAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.OBJECT_ID), getFormulaWithBrickField(BrickField.SPEED)));
    }
}