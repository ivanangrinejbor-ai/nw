package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class PatchWebRequestBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public PatchWebRequestBrick() {
        addAllowedBrickField(BrickField.URL, R.id.brick_patch_web_request_edit_url);
        addAllowedBrickField(BrickField.HEADER, R.id.brick_patch_web_request_edit_header);
        addAllowedBrickField(BrickField.BODY, R.id.brick_patch_web_request_edit_body);
        addAllowedBrickField(BrickField.TIMEOUT, R.id.brick_patch_web_request_edit_timeout);
    }

    public PatchWebRequestBrick(String val1, String val2, String val3) {
        this(new Formula(val1), new Formula(val2), new Formula(val3), null);
    }

    public PatchWebRequestBrick(Formula rurl, Formula header, Formula body, UserVariable userVariable) {
        this();
        setFormulaWithBrickField(BrickField.URL, rurl);
        setFormulaWithBrickField(BrickField.HEADER, header);
        setFormulaWithBrickField(BrickField.BODY, body);
        this.userVariable = userVariable;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_patch_web_request;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.patch_web_request_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createPatchWebRequestAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.URL),
                getFormulaWithBrickField(BrickField.HEADER),
                getFormulaWithBrickField(BrickField.BODY), userVariable));
    }
}
