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
                int number = cloneNumberFormula.interpretInteger(scope);
                if (scope.getSprite().cloneIndex != number) {
                    // This clone doesn't match the target number, skip execution
                    return true;
                }
            } catch (InterpretationException e) {
                Log.d(getClass().getSimpleName(), "Formula interpretation failed", e);
                return true;
            }
        }
        // Only execute cloneAction if this clone matches the target number
        if (cloneAction != null && scope != null) {
            try {
                if (scope.getSprite().cloneIndex == cloneNumberFormula.interpretInteger(scope)) {
                    return cloneAction.act(delta);
                }
            } catch (InterpretationException e) {
                Log.d(getClass().getSimpleName(), "Formula interpretation failed", e);
            }
        }
        return true;
    }

    @Override
    public void restart() {
        super.restart();
        initialized = false;
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
