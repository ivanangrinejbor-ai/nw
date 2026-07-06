package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class HeadWebRequestBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public HeadWebRequestBrick() {
        addAllowedBrickField(BrickField.URL, R.id.brick_head_web_request_edit_url);
        addAllowedBrickField(BrickField.HEADER, R.id.brick_head_web_request_edit_header);
        addAllowedBrickField(BrickField.TIMEOUT, R.id.brick_head_web_request_edit_timeout);
    }

    public HeadWebRequestBrick(String val1, String val2) {
        this(new Formula(val1), new Formula(val2), null);
    }

    public HeadWebRequestBrick(Formula rurl, Formula header, UserVariable userVariable) {
        this();
        setFormulaWithBrickField(BrickField.URL, rurl);
        setFormulaWithBrickField(BrickField.HEADER, header);
        this.userVariable = userVariable;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_head_web_request;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.head_web_request_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createHeadWebRequestAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.URL),
                getFormulaWithBrickField(BrickField.HEADER), userVariable));
    }
}
