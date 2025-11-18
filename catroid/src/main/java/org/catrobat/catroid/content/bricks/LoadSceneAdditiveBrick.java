package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LoadSceneAdditiveBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LoadSceneAdditiveBrick() {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_load_scene_additive_edit_text);
    }

    public LoadSceneAdditiveBrick(String fileName) {
        this(new Formula(fileName));
    }

    public LoadSceneAdditiveBrick(Formula fileNameFormula) {
        this();
        setFormulaWithBrickField(BrickField.TEXT, fileNameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_load_scene_additive;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLoadSceneAdditiveAction(sprite, sequence, getFormulaWithBrickField(BrickField.TEXT)));
    }
}