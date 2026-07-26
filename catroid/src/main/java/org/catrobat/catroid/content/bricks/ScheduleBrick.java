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

import android.content.Context;
import android.view.View;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class ScheduleBrick extends FormulaBrick implements CompositeBrick {

	private static final long serialVersionUID = 1L;
	private transient EndBrick endBrick = new EndBrick(this);
	private List<Brick> scheduledBricks = new ArrayList<>();

	// XStream не вызывает конструкторы — transient endBrick будет null после десериализации.
	private Object readResolve() {
		if (endBrick == null) {
			endBrick = new EndBrick(this);
		}
		if (scheduledBricks == null) {
			scheduledBricks = new ArrayList<>();
		}
		return this;
	}

	public ScheduleBrick() {
		addAllowedBrickField(BrickField.TIME_TO_WAIT_IN_SECONDS, R.id.brick_schedule_edit_text_delay);
	}

	public ScheduleBrick(Formula delay) {
		this();
		setFormulaWithBrickField(BrickField.TIME_TO_WAIT_IN_SECONDS, delay);
	}

	@Override
	public boolean hasSecondaryList() {
		return false;
	}

	@Override
	public List<Brick> getNestedBricks() {
		return scheduledBricks;
	}

	@Override
	public List<Brick> getSecondaryNestedBricks() {
		return null;
	}

	public boolean addBrick(Brick brick) {
		return scheduledBricks.add(brick);
	}

	public List<Brick> getScheduledBricks() {
		return scheduledBricks;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		for (Brick brick : scheduledBricks) {
			brick.setCommentedOut(commentedOut);
		}
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		ScheduleBrick clone = (ScheduleBrick) super.clone();
		clone.endBrick = new EndBrick(clone);
		clone.scheduledBricks = new ArrayList<>();
		for (Brick brick : scheduledBricks) {
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
		for (Brick brick : scheduledBricks) {
			brick.addToFlatList(bricks);
		}
		bricks.add(endBrick);
	}

	@Override
	public void setParent(Brick parent) {
		super.setParent(parent);
		for (Brick brick : scheduledBricks) {
			brick.setParent(this);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return scheduledBricks;
	}

	@Override
	public boolean removeChild(Brick brick) {
		if (scheduledBricks.remove(brick)) {
			return true;
		}
		for (Brick childBrick : scheduledBricks) {
			if (childBrick.removeChild(brick)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_schedule;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		setSecondsLabel(view, BrickField.TIME_TO_WAIT_IN_SECONDS);
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		ScriptSequenceAction scheduleSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
		for (Brick brick : scheduledBricks) {
			if (!brick.isCommentedOut()) {
				brick.addActionToSequence(sprite, scheduleSequence);
			}
		}

		Action action = sprite.getActionFactory().createScheduleAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.TIME_TO_WAIT_IN_SECONDS), scheduleSequence);

		sequence.addAction(action);
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
		super.addRequiredResources(requiredResourcesSet);
		for (Brick brick : scheduledBricks) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}
}
