package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class HttpClearBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public HttpClearBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_http_clear_id_field);
    }

    public HttpClearBrick(String requestId) {
        this(new Formula(requestId));
    }

    public HttpClearBrick(Formula requestId) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, requestId);
    }

    @Override public int getViewResource() { return R.layout.brick_http_clear; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createHttpClearAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1)));
    }
}
