package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class BakeByPrefixBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public BakeByPrefixBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_bake_prefix_edit);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_bake_result_edit);
    }

    public BakeByPrefixBrick(String prefix, String resultName) {
        this(new Formula(prefix), new Formula(resultName));
    }

    public BakeByPrefixBrick(Formula prefix, Formula resultName) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, prefix);
        setFormulaWithBrickField(BrickField.VALUE_2, resultName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_bake_by_prefix;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createBakeByPrefixAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2)
        ));
    }
}