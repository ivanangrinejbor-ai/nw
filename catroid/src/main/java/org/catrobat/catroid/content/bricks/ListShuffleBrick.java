package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class ListShuffleBrick extends UserListBrick {

    private static final long serialVersionUID = 1L;

    public ListShuffleBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_shuffle_list;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_shuffle_list_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createShuffleListAction(userList));
    }
}
