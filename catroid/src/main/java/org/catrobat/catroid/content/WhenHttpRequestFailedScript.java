package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ConcurrentFormulaHashMap;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenHttpRequestFailedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.HttpRequestEventId;
import org.catrobat.catroid.formulaeditor.Formula;

public class WhenHttpRequestFailedScript extends Script {
    private static final long serialVersionUID = 1L;
    private ConcurrentFormulaHashMap formulaMap = new ConcurrentFormulaHashMap();

    public WhenHttpRequestFailedScript() {
        formulaMap.putIfAbsent(Brick.BrickField.VALUE_1, new Formula("request"));
    }

    public WhenHttpRequestFailedScript(Formula requestId) {
        this();
        formulaMap.replace(Brick.BrickField.VALUE_1, requestId);
    }

    public ConcurrentFormulaHashMap getFormulaMap() { return formulaMap; }

    public boolean matches(Project project, Sprite sprite, String requestId) {
        Formula formula = formulaMap.get(Brick.BrickField.VALUE_1);
        try {
            return formula != null && requestId.equals(formula.interpretString(new Scope(project, sprite, null)));
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override public Script clone() throws CloneNotSupportedException {
        WhenHttpRequestFailedScript clone = (WhenHttpRequestFailedScript) super.clone();
        clone.formulaMap = formulaMap.clone();
        return clone;
    }

    @Override public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) scriptBrick = new WhenHttpRequestFailedBrick(this);
        return scriptBrick;
    }

    @Override public void addRequiredResources(Brick.ResourcesSet resources) {
        for (Formula formula : formulaMap.values()) formula.addRequiredResources(resources);
        for (Brick brick : brickList) brick.addRequiredResources(resources);
    }

    @Override public EventId createEventId(Sprite sprite) { return new HttpRequestEventId(); }
}
