package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetAIBrick extends FormulaBrick {
    public SetAIBrick() {
        addAllowedBrickField(BrickField.OBJECT_ID, R.id.brick_ai_obj_id);
        addAllowedBrickField(BrickField.MODE, R.id.brick_ai_mode);
        addAllowedBrickField(BrickField.TARGET, R.id.brick_ai_target);
        addAllowedBrickField(BrickField.SPEED, R.id.brick_ai_speed);
        addAllowedBrickField(BrickField.DISTANCE, R.id.brick_ai_stop);
        addAllowedBrickField(BrickField.RANGE, R.id.brick_ai_range);
        addAllowedBrickField(BrickField.STEP_HEIGHT, R.id.brick_ai_step);
        addAllowedBrickField(BrickField.AVOID_OBSTACLES, R.id.brick_ai_avoid);
    }

    public SetAIBrick(String objId, int mode, String target, float speed, float stop, float range, float step, int avoid) {
        this(new Formula(objId), new Formula(mode), new Formula(target),
                new Formula(speed), new Formula(stop), new Formula(range),
                new Formula(step), new Formula(avoid));
    }

    public SetAIBrick(Formula objId, Formula mode, Formula target, Formula speed, Formula stop, Formula range, Formula step, Formula avoid) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_ID, objId);
        setFormulaWithBrickField(BrickField.MODE, mode);
        setFormulaWithBrickField(BrickField.TARGET, target);
        setFormulaWithBrickField(BrickField.SPEED, speed);
        setFormulaWithBrickField(BrickField.DISTANCE, stop);
        setFormulaWithBrickField(BrickField.RANGE, range);
        setFormulaWithBrickField(BrickField.STEP_HEIGHT, step);
        setFormulaWithBrickField(BrickField.AVOID_OBSTACLES, avoid);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_ai;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetAIAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.OBJECT_ID),
                getFormulaWithBrickField(BrickField.MODE),
                getFormulaWithBrickField(BrickField.TARGET),
                getFormulaWithBrickField(BrickField.SPEED),
                getFormulaWithBrickField(BrickField.DISTANCE),
                getFormulaWithBrickField(BrickField.RANGE),
                getFormulaWithBrickField(BrickField.STEP_HEIGHT),
                getFormulaWithBrickField(BrickField.AVOID_OBSTACLES)));
    }
}