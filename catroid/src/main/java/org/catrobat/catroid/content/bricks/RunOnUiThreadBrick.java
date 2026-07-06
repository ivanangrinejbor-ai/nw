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

import java.util.ArrayList;
import java.util.List;

public class RunOnUiThreadBrick extends BrickBaseType implements CompositeBrick {

	private static final long serialVersionUID = 1L;

	private transient EndBrick endBrick = new EndBrick(this);
	protected List<Brick> uiThreadBricks = new ArrayList<>();

	public RunOnUiThreadBrick() {
	}

	@Override
	public boolean hasSecondaryList() {
		return false;
	}

	@Override
	public List<Brick> getNestedBricks() {
		return uiThreadBricks;
	}

	@Override
	public List<Brick> getSecondaryNestedBricks() {
		return null;
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return true;
	}

	@Override
	public List<Brick> getAllParts() {
		List<Brick> parts = new ArrayList<>();
		parts.add(this);
		parts.add(endBrick);
		return parts;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		bricks.add(this);
		for (Brick brick : uiThreadBricks) {
			brick.addToFlatList(bricks);
		}
		bricks.add(endBrick);
	}

	@Override
	public void setParent(Brick parentBrick) {
		super.setParent(parentBrick);
		for (Brick brick : uiThreadBricks) {
			brick.setParent(this);
		}
	}

	@Override
	public boolean removeChild(Brick brick) {
		return uiThreadBricks.remove(brick);
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return uiThreadBricks;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		for (Brick brick : uiThreadBricks) {
			brick.setCommentedOut(commentedOut);
		}
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		RunOnUiThreadBrick clone = (RunOnUiThreadBrick) super.clone();
		clone.uiThreadBricks = new ArrayList<>();
		for (Brick brick : uiThreadBricks) {
			Brick clonedBrick = brick.clone();
			clonedBrick.setParent(clone);
			clone.uiThreadBricks.add(clonedBrick);
		}
		clone.endBrick = new EndBrick(clone);
		return clone;
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
		super.addRequiredResources(requiredResourcesSet);
		for (Brick brick : uiThreadBricks) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_run_on_ui_thread;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		ActionFactory factory = sprite.getActionFactory();

		ScriptSequenceAction uiSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
		for (Brick brick : uiThreadBricks) {
			brick.addActionToSequence(sprite, uiSequence);
		}

		Action action = factory.createRunOnUiThreadAction(sprite, sequence, uiSequence);
		sequence.addAction(action);
	}
}
