package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.StateMachineManager;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class IfInStateAction extends Action {
    private Scope scope;
    private Formula machineFormula;
    private Formula stateFormula;
    private Action stateAction;

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setMachine(Formula machine) {
        this.machineFormula = machine;
    }

    public void setState(Formula state) {
        this.stateFormula = state;
    }

    public void setStateAction(Action stateAction) {
        this.stateAction = stateAction;
    }

    private boolean matches() {
        if (scope == null) {
            return false;
        }
        try {
            String machine = machineFormula != null ? machineFormula.interpretString(scope) : "";
            String state = stateFormula != null ? stateFormula.interpretString(scope) : "";
            return StateMachineManager.isInState(scope.getSprite(), machine, state);
        } catch (InterpretationException e) {
            Log.d(getClass().getSimpleName(), "Formula interpretation failed", e);
            return false;
        }
    }

    @Override
    public boolean act(float delta) {
        if (stateAction != null && matches()) {
            return stateAction.act(delta);
        }
        return true;
    }

    @Override
    public void restart() {
        super.restart();
        if (stateAction != null) {
            stateAction.restart();
        }
    }

    @Override
    public void setActor(Actor actor) {
        super.setActor(actor);
        if (stateAction != null) {
            stateAction.setActor(actor);
        }
    }
}
