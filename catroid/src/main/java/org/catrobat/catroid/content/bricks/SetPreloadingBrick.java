package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class SetPreloadingBrick extends BrickBaseType {
    private int preloadEnabled;

    public SetPreloadingBrick() { this.preloadEnabled = 1; }
    public SetPreloadingBrick(int enabled) { this.preloadEnabled = enabled; }

    @Override public int getViewResource() { return R.layout.brick_set_preloading; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetPreloadingAction(preloadEnabled));
    }
}
