package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.stage.StageActivity;

public class AttachToCameraWithOffsetAction extends TemporalAction {
    private Scope scope;
    private Formula objectName;
    private Formula offsetX;
    private Formula offsetY;
    private Formula offsetZ;

    private boolean started;

    @Override
    public void restart() {
        super.restart();
        started = false;
    }

    @Override
    protected void update(float percent) {
        if (started) return;
        if (scope == null) return;
        var listener = StageActivity.getActiveStageListener();
        if (listener == null || listener.sceneManager == null) return;
        started = true;

        try {
            String name = objectName != null ? objectName.interpretString(scope) : null;
            if (name == null || name.isEmpty()) return;

            float x = offsetX != null ? offsetX.interpretFloat(scope) : 0f;
            float y = offsetY != null ? offsetY.interpretFloat(scope) : 0f;
            float z = offsetZ != null ? offsetZ.interpretFloat(scope) : 0f;

            listener.sceneManager.attachObjectToCamera(name, x, y, z);
        } catch (InterpretationException e) {
            return;
        }
    }

    public void setScope(Scope scope) { this.scope = scope; }
    public void setObjectName(Formula objectName) { this.objectName = objectName; }
    public void setOffsetX(Formula offsetX) { this.offsetX = offsetX; }
    public void setOffsetY(Formula offsetY) { this.offsetY = offsetY; }
    public void setOffsetZ(Formula offsetZ) { this.offsetZ = offsetZ; }
}
