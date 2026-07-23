package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCustomParamValueBrick extends FormulaBrick {
    private static final long serialVersionUID = 2L;

    public SetCustomParamValueBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_set_param_name);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_param_val);
    }

    public SetCustomParamValueBrick(String paramName, String newValue) {
        this(new Formula(paramName), new Formula(newValue));
    }

    public SetCustomParamValueBrick(Formula paramName, Formula newValue) {
        this();
        setFormulaWithBrickField(BrickField.NAME, paramName);
        setFormulaWithBrickField(BrickField.VALUE_1, newValue);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_custom_param_value;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetCustomParamValueAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.VALUE_1)));
    }
}
