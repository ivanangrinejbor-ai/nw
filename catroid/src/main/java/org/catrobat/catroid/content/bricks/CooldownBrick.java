/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
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
import org.catrobat.catroid.content.actions.CooldownAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class CooldownBrick extends FormulaBrick implements CompositeBrick {

	private static final long serialVersionUID = 1L;

	private transient EndBrick endBrick = new EndBrick(this);

	protected List<Brick> innerBricks = new ArrayList<>();

	public CooldownBrick() {
		addAllowedBrickField(BrickField.TIME, R.id.brick_cooldown_edit_text);
	}

	public CooldownBrick(double seconds) {
		this(new Formula(seconds));
	}

	public CooldownBrick(Formula seconds) {
		this();
		setFormulaWithBrickField(BrickField.TIME, seconds);
	}

	private Object readResolve() {
		if (endBrick == null) {
			endBrick = new EndBrick(this);
		}
		if (innerBricks == null) {
			innerBricks = new ArrayList<>();
		}
		return this;
	}

	public void addBrickToInnerBranch(Brick brick) {
		innerBricks.add(brick);
	}

	public List<Brick> getInnerBricks() {
		return innerBricks;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_cooldown;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		ActionFactory factory = sprite.getActionFactory();

		ScriptSequenceAction innerSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
		for (Brick brick : innerBricks) {
			if (!brick.isCommentedOut()) {
				brick.addActionToSequence(sprite, innerSequence);
			}
		}

		Action action = factory.createCooldownAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.TIME));
		if (action instanceof CooldownAction) {
			((CooldownAction) action).setInnerAction(innerSequence);
		}
		sequence.addAction(action);
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return true;
	}

	@Override
	public boolean hasSecondaryList() {
		return false;
	}

	@Override
	public List<Brick> getNestedBricks() {
		return innerBricks;
	}

	@Override
	public List<Brick> getSecondaryNestedBricks() {
		return null;
	}

	@Override
	public List<Brick> getAllParts() {
		List<Brick> parts = new ArrayList<>();
		parts.add(this);
		parts.add(endBrick);
		return parts;
	}

	public Brick getEndBrick() {
		return endBrick;
	}

	public void setEndBrick(EndBrick endBrick) {
		this.endBrick = endBrick;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		CooldownBrick clone = (CooldownBrick) super.clone();
		clone.endBrick = new EndBrick(clone);
		clone.innerBricks = new ArrayList<>();
		for (Brick brick : innerBricks) {
			clone.innerBricks.add(brick.clone());
		}
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CooldownBrick)) return false;
		CooldownBrick other = (CooldownBrick) obj;
		if (!super.equals(other)) return false;
		return innerBricks != null && innerBricks.equals(other.innerBricks);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
