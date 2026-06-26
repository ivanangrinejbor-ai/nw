package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class FollowPathBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public FollowPathBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_follow_path_edit_1);
    }

    public FollowPathBrick(String spriteName) {
        this(new Formula(spriteName));
    }

    public FollowPathBrick(Formula spriteName) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, spriteName);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_follow_path;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createFollowPathAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1)));
    }
}
