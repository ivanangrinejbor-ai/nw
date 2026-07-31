package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.stage.StageActivity;

public class SetAmbientLightAction extends TemporalAction {
    public Scope scope;
    public Formula red;
    public Formula green;
    public Formula blue;

    @Override
    protected void update(float percent) {
        if (scope == null) return;
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {

            float r = red.interpretFloat(scope);
            float g = green.interpretFloat(scope);
            float b = blue.interpretFloat(scope);

            threeDManager.setAmbientLight(r, g, b);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}