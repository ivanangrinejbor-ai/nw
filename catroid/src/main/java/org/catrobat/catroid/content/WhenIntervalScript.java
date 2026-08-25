package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ConcurrentFormulaHashMap;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenIntervalBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.IntervalEventId;
import org.catrobat.catroid.formulaeditor.Formula;

public class WhenIntervalScript extends Script {

	private static final long serialVersionUID = 1L;

	private ConcurrentFormulaHashMap formulaMap = new ConcurrentFormulaHashMap();

	public WhenIntervalScript() {
		formulaMap.putIfAbsent(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS, new Formula(1));
	}

	public WhenIntervalScript(Formula seconds) {
		this();
		formulaMap.replace(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS, seconds);
	}

	public ConcurrentFormulaHashMap getFormulaMap() {
		return formulaMap;
	}

	@Override
	public Script clone() throws CloneNotSupportedException {
		WhenIntervalScript clone = (WhenIntervalScript) super.clone();
		clone.formulaMap = formulaMap.clone();
		return clone;
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenIntervalBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public void addRequiredResources(final Brick.ResourcesSet requiredResourcesSet) {
		for (Formula formula : formulaMap.values()) {
			formula.addRequiredResources(requiredResourcesSet);
		}
		for (Brick brick : brickList) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new IntervalEventId(formulaMap.get(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS));
	}
}
