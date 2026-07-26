/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class SwitchBeginBrick extends FormulaBrick implements CompositeBrick {

	private static final long serialVersionUID = 1L;

	private transient EndBrick endBrick = new EndBrick(this, R.layout.brick_switch_end);

	private List<Brick> caseBricks = new ArrayList<>();

	// XStream не вызывает конструкторы — transient endBrick будет null после десериализации.
	private Object readResolve() {
		if (endBrick == null) {
			endBrick = new EndBrick(this, R.layout.brick_switch_end);
		}
		if (caseBricks == null) {
			caseBricks = new ArrayList<>();
		}
		return this;
	}

	public SwitchBeginBrick() {
		addAllowedBrickField(BrickField.TEXT, R.id.brick_switch_begin_edit_text);
	}

	public SwitchBeginBrick(Formula expression) {
		this();
		setFormulaWithBrickField(BrickField.TEXT, expression);
	}

	@Override
	public boolean hasSecondaryList() {
		return false;
	}

	@Override
	public List<Brick> getNestedBricks() {
		return caseBricks;
	}

	@Override
	public List<Brick> getSecondaryNestedBricks() {
		return null;
	}

	public boolean addBrick(Brick brick) {
		return caseBricks.add(brick);
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		for (Brick brick : caseBricks) {
			brick.setCommentedOut(commentedOut);
		}
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		SwitchBeginBrick clone = (SwitchBeginBrick) super.clone();
		clone.endBrick = new EndBrick(clone, R.layout.brick_switch_end);
		clone.caseBricks = new ArrayList<>();
		for (Brick brick : caseBricks) {
			clone.addBrick(brick.clone());
		}
		return clone;
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return true;
	}

	@Override
	public List<Brick> getAllParts() {
		List<Brick> bricks = new ArrayList<>();
		bricks.add(this);
		bricks.add(endBrick);
		return bricks;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		super.addToFlatList(bricks);
		for (Brick brick : caseBricks) {
			brick.addToFlatList(bricks);
		}
		bricks.add(endBrick);
	}

	@Override
	public void setParent(Brick parent) {
		super.setParent(parent);
		for (Brick brick : caseBricks) {
			brick.setParent(this);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return caseBricks;
	}

	@Override
	public boolean removeChild(Brick brick) {
		if (caseBricks.remove(brick)) {
			return true;
		}
		for (Brick childBrick : caseBricks) {
			if (childBrick.removeChild(brick)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_switch_begin;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		List<Formula> caseValues = new ArrayList<>();
		List<ScriptSequenceAction> caseSequences = new ArrayList<>();

		ScriptSequenceAction currentCaseSequence = null;

		for (Brick brick : caseBricks) {
			if (brick instanceof SwitchCaseBrick) {
				if (currentCaseSequence != null) {
					caseSequences.add(currentCaseSequence);
				}
				caseValues.add(((SwitchCaseBrick) brick).getFormulaWithBrickField(BrickField.TEXT));
				currentCaseSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
			} else if (currentCaseSequence != null && !brick.isCommentedOut()) {
				brick.addActionToSequence(sprite, currentCaseSequence);
			}
		}
		if (currentCaseSequence != null) {
			caseSequences.add(currentCaseSequence);
		}

		Formula expression = getFormulaWithBrickField(BrickField.TEXT);
		Action action = sprite.getActionFactory().createSwitchCaseAction(sprite, sequence,
				expression, caseValues, caseSequences);
		sequence.addAction(action);
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
		super.addRequiredResources(requiredResourcesSet);
		for (Brick brick : caseBricks) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}
}
