package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class JsonParseBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public JsonParseBrick() {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_json_name);
        addAllowedBrickField(BrickField.VALUE, R.id.brick_json_text);
    }

    public JsonParseBrick(String name, String text) {
        this(new Formula(name), new Formula(text));
    }

    public JsonParseBrick(Formula name, Formula text) {
        this();
        setFormulaWithBrickField(BrickField.TEXT, name);
        setFormulaWithBrickField(BrickField.VALUE, text);
    }

    @Override public int getViewResource() { return R.layout.brick_json_parse; }

    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createJsonParseAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.TEXT), getFormulaWithBrickField(BrickField.VALUE)));
    }
}
