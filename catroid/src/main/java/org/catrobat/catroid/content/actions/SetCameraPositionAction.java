package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.stage.StageActivity;

public class SetCameraPositionAction extends TemporalAction {
    public Scope scope;
    public Formula xValue;
    public Formula yValue;
    public Formula zValue;

    @Override
    protected void update(float percent) {
        if (scope == null) return;
        if (xValue == null || yValue == null || zValue == null) return;
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) {
            return;
        }

        try {
            float x = xValue.interpretFloat(scope);
            float y = yValue.interpretFloat(scope);
            float z = zValue.interpretFloat(scope);

            threeDManager.setCameraPosition(x, y, z);
            // Note: Camera position update is handled internally by threeDManager.setCameraPosition; explicit update() call may be deferred to next render frame
        } catch (InterpretationException e) {
            e.printStackTrace();
        }
    }
}