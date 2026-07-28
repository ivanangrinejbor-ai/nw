package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class JumpToNodeBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public JumpToNodeBrick() {
        addAllowedBrickField(BrickField.DIALOGUE_NODE_ID, R.id.brick_dialogue_node_edit);
    }

    public JumpToNodeBrick(String nodeId) {
        this(new Formula(nodeId));
    }

    public JumpToNodeBrick(Formula nodeId) {
        this();
        setFormulaWithBrickField(BrickField.DIALOGUE_NODE_ID, nodeId);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_jump_to_node;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createJumpToNodeAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.DIALOGUE_NODE_ID)));
    }
}
