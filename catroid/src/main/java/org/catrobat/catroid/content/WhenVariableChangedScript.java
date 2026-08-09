package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenVariableChangedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.WhenVariableChangedEventId;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class WhenVariableChangedScript extends Script {

    private static final long serialVersionUID = 1L;

    private UserVariable monitoredVariable;
    private transient Object lastValue;

    public UserVariable getMonitoredVariable() {
        return monitoredVariable;
    }

    public void setMonitoredVariable(UserVariable variable) {
        this.monitoredVariable = variable;
    }

    public Object getLastValue() {
        return lastValue;
    }

    public void setLastValue(Object value) {
        this.lastValue = value;
    }

    @Override
    public Script clone() throws CloneNotSupportedException {
        WhenVariableChangedScript clone = (WhenVariableChangedScript) super.clone();
        clone.monitoredVariable = this.monitoredVariable;
        clone.lastValue = this.lastValue;
        return clone;
    }

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new WhenVariableChangedBrick(this);
        }
        return scriptBrick;
    }

    @Override
    public void addRequiredResources(final Brick.ResourcesSet requiredResourcesSet) {
        for (Brick brick : brickList) {
            brick.addRequiredResources(requiredResourcesSet);
        }
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        String varName = monitoredVariable != null ? monitoredVariable.getName() : "";
        return new WhenVariableChangedEventId(varName);
    }
}
