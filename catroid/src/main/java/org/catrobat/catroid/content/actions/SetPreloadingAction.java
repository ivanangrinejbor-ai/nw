package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.GlobalManager;

public class SetPreloadingAction extends TemporalAction {
    private int enabled;

    public SetPreloadingAction(int enabled) { this.enabled = enabled; }

    @Override protected void update(float percent) {
        GlobalManager.Companion.setPreloadProject(enabled != 0);
    }
}
