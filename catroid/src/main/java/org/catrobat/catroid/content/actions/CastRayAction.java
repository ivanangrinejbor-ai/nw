package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class CastRayAction extends TemporalAction {
    public Scope scope;
    public Formula rayName;
    public Formula fromX, fromY, fromZ;
    public Formula dirX, dirY, dirZ;

    private static final Vector3 tmpFrom = new Vector3();
    private static final Vector3 tmpDir = new Vector3();

    @Override
    protected void update(float percent) {
        ThreeDManager manager = StageActivity.getActiveStageListener().getThreeDManager();
        if (manager == null) return;

        try {
            String name = rayName.interpretString(scope);
            if (name.isEmpty()) return;

            tmpFrom.set(
                    fromX.interpretFloat(scope),
                    fromY.interpretFloat(scope),
                    fromZ.interpretFloat(scope)
            );

            tmpDir.set(
                    dirX.interpretFloat(scope),
                    dirY.interpretFloat(scope),
                    dirZ.interpretFloat(scope)
            ).nor();

            manager.castRay(name, tmpFrom, tmpDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}