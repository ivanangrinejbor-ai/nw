package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class GetCustomParamBrick extends UserVariableBrickWithFormula {
    private static final long serialVersionUID = 2L;

    public GetCustomParamBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_get_custom_param_name);
    }

    public GetCustomParamBrick(String paramName, UserVariable targetVar) {
        this(new Formula(paramName), targetVar);
    }

    public GetCustomParamBrick(Formula paramName, UserVariable targetVar) {
        this();
        setFormulaWithBrickField(BrickField.NAME, paramName);
        this.userVariable = targetVar;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_get_custom_param;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_get_custom_param_var_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createGetCustomParamAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                userVariable));
    }
}
