package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class ExecuteForCloneNumberAction extends Action {
    private Scope scope;
    private Formula cloneNumberFormula;
    private Action cloneAction;
    private boolean initialized;
    private int targetCloneIndex = -1;  // cached on first act(), avoids re-interpreting each frame

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setCloneNumber(Formula cloneNumber) {
        this.cloneNumberFormula = cloneNumber;
    }

    public void setCloneAction(Action cloneAction) {
        this.cloneAction = cloneAction;
    }

    @Override
    public boolean act(float delta) {
        if (!initialized) {
            initialized = true;
            if (scope == null || cloneNumberFormula == null || cloneAction == null) {
                return true;
            }
            try {
                targetCloneIndex = cloneNumberFormula.interpretInteger(scope);
                if (scope.getSprite() == null || scope.getSprite().cloneIndex != targetCloneIndex) {
                    // This clone doesn't match the target number, skip execution permanently
                    targetCloneIndex = -1;
                    return true;
                }
            } catch (InterpretationException e) {
                Log.d(getClass().getSimpleName(), "Formula interpretation failed", e);
                return true;
            }
        }
        // Run the inner action only if this clone matched — no re-interpretation
        if (cloneAction != null && targetCloneIndex >= 0) {
            return cloneAction.act(delta);
        }
        return true;
    }

    @Override
    public void restart() {
        super.restart();
        initialized = false;
        targetCloneIndex = -1;
        if (cloneAction != null) {
            cloneAction.restart();
        }
    }

    @Override
    public void setActor(Actor actor) {
        super.setActor(actor);
        if (cloneAction != null) {
            cloneAction.setActor(actor);
        }
    }
}
