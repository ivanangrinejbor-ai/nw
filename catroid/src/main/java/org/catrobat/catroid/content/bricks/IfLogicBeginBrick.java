/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
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
import java.util.UUID;

import androidx.annotation.VisibleForTesting;

public class IfLogicBeginBrick extends FormulaBrick implements CompositeBrick {

	private static final long serialVersionUID = 1L;

	private transient ElseBrick elseBrick = new ElseBrick(this);
	private transient EndBrick endBrick = new EndBrick(this, R.layout.brick_if_end_if);
	private transient List<ElseIfBrick> elseIfMarkers = new ArrayList<>();
	private transient List<ElseIfEndBrick> elseIfEndMarkers = new ArrayList<>();

	protected List<Brick> ifBranchBricks = new ArrayList<>();
	protected List<Brick> elseBranchBricks = new ArrayList<>();

	protected List<Formula> elseIfConditions = new ArrayList<>();
	protected List<List<Brick>> elseIfBranchBricks = new ArrayList<>();

	public IfLogicBeginBrick() {
		addAllowedBrickField(BrickField.IF_CONDITION, R.id.brick_if_begin_edit_text);
	}

	public IfLogicBeginBrick(Formula formula) {
		this();
		setFormulaWithBrickField(BrickField.IF_CONDITION, formula);
	}

	private Object readResolve() {
		if (elseBrick == null) {
			elseBrick = new ElseBrick(this);
		}
		if (endBrick == null) {
			endBrick = new EndBrick(this, R.layout.brick_if_end_if);
		}
		if (ifBranchBricks == null) {
			ifBranchBricks = new ArrayList<>();
		}
		if (elseBranchBricks == null) {
			elseBranchBricks = new ArrayList<>();
		}
		if (elseIfConditions == null) {
			elseIfConditions = new ArrayList<>();
		}
		if (elseIfBranchBricks == null) {
			elseIfBranchBricks = new ArrayList<>();
		}
		rebuildElseIfMarkers();
		return this;
	}

	private void rebuildElseIfMarkers() {
		elseIfMarkers = new ArrayList<>();
		elseIfEndMarkers = new ArrayList<>();
		for (int i = 0; i < elseIfConditions.size(); i++) {
			elseIfMarkers.add(new ElseIfBrick(this, i));
			elseIfEndMarkers.add(new ElseIfEndBrick(this, i));
		}
	}

	public void addElseIfBranch() {
		elseIfConditions.add(new Formula(0));
		elseIfBranchBricks.add(new ArrayList<>());
		elseIfMarkers.add(new ElseIfBrick(this, elseIfConditions.size() - 1));
		elseIfEndMarkers.add(new ElseIfEndBrick(this, elseIfConditions.size() - 1));
	}

	public int getElseIfCount() {
		return elseIfConditions.size();
	}

	public Formula getElseIfCondition(int index) {
		return elseIfConditions.get(index);
	}

	public List<Brick> getElseIfBranchParts(int index) {
		List<Brick> parts = new ArrayList<>();
		if (index >= 0 && index < elseIfMarkers.size()) {
			parts.add(elseIfMarkers.get(index));
		}
		if (index >= 0 && index < elseIfEndMarkers.size()) {
			parts.add(elseIfEndMarkers.get(index));
		}
		return parts;
	}

	public boolean removeElseIfBranch(int index) {
		if (index < 0 || index >= elseIfConditions.size()) {
			return false;
		}
		elseIfConditions.remove(index);
		elseIfBranchBricks.remove(index);
		rebuildElseIfMarkers();
		return true;
	}

	public boolean moveElseIfBranchUp(int index) {
		if (index <= 0 || index >= elseIfConditions.size()) {
			return false;
		}
		java.util.Collections.swap(elseIfConditions, index, index - 1);
		java.util.Collections.swap(elseIfBranchBricks, index, index - 1);
		rebuildElseIfMarkers();
		return true;
	}

	public boolean moveElseIfBranchDown(int index) {
		if (index < 0 || index >= elseIfConditions.size() - 1) {
			return false;
		}
		java.util.Collections.swap(elseIfConditions, index, index + 1);
		java.util.Collections.swap(elseIfBranchBricks, index, index + 1);
		rebuildElseIfMarkers();
		return true;
	}

	public void setElseIfCondition(int index, Formula formula) {
		if (index >= 0 && index < elseIfConditions.size()) {
			elseIfConditions.set(index, formula);
		}
	}

	public List<Brick> getElseIfBranch(int index) {
		return elseIfBranchBricks.get(index);
	}

	@Override
	public boolean hasSecondaryList() {
		return true;
	}

	@Override
	public List<Brick> getNestedBricks() {
		return ifBranchBricks;
	}

	@Override
	public List<Brick> getSecondaryNestedBricks() {
		return elseBranchBricks;
	}

	public boolean addBrickToIfBranch(Brick brick) {
		return ifBranchBricks.add(brick);
	}

	public boolean addBrickToElseBranch(Brick brick) {
		return elseBranchBricks.add(brick);
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		for (Brick brick : ifBranchBricks) {
			brick.setCommentedOut(commentedOut);
		}
		for (List<Brick> branch : elseIfBranchBricks) {
			for (Brick brick : branch) {
				brick.setCommentedOut(commentedOut);
			}
		}
		for (Brick brick : elseBranchBricks) {
			brick.setCommentedOut(commentedOut);
		}
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		IfLogicBeginBrick clone = (IfLogicBeginBrick) super.clone();
		clone.elseBrick = new ElseBrick(clone);
		clone.endBrick = new EndBrick(clone, R.layout.brick_if_end_if);
		clone.ifBranchBricks = new ArrayList<>();
		clone.elseBranchBricks = new ArrayList<>();
		clone.elseIfConditions = new ArrayList<>();
		clone.elseIfBranchBricks = new ArrayList<>();

		for (Brick brick : ifBranchBricks) {
			clone.addBrickToIfBranch(brick.clone());
		}
		for (int i = 0; i < elseIfConditions.size(); i++) {
			clone.elseIfConditions.add(elseIfConditions.get(i).clone());
			List<Brick> clonedBranch = new ArrayList<>();
			for (Brick brick : elseIfBranchBricks.get(i)) {
				clonedBranch.add(brick.clone());
			}
			clone.elseIfBranchBricks.add(clonedBranch);
		}
		for (Brick brick : elseBranchBricks) {
			clone.addBrickToElseBranch(brick.clone());
		}
		clone.rebuildElseIfMarkers();
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
		for (int i = 0; i < elseIfMarkers.size(); i++) {
			bricks.add(elseIfMarkers.get(i));
			if (i < elseIfEndMarkers.size()) {
				bricks.add(elseIfEndMarkers.get(i));
			}
		}
		bricks.add(elseBrick);
		bricks.add(endBrick);
		return bricks;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		super.addToFlatList(bricks);
		for (Brick brick : ifBranchBricks) {
			brick.addToFlatList(bricks);
		}
		for (int i = 0; i < elseIfMarkers.size(); i++) {
			bricks.add(elseIfMarkers.get(i));
			for (Brick brick : elseIfBranchBricks.get(i)) {
				brick.addToFlatList(bricks);
			}
			if (i < elseIfEndMarkers.size()) {
				bricks.add(elseIfEndMarkers.get(i));
			}
		}
		bricks.add(elseBrick);
		for (Brick brick : elseBranchBricks) {
			brick.addToFlatList(bricks);
		}
		bricks.add(endBrick);
	}

	@Override
	public void setParent(Brick parent) {
		super.setParent(parent);
		for (Brick brick : ifBranchBricks) {
			brick.setParent(this);
		}
		for (Brick brick : elseBranchBricks) {
			brick.setParent(elseBrick);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return ifBranchBricks;
	}

	@Override
	public boolean removeChild(Brick brick) {
		if (brick instanceof ElseIfBrick) {
			ElseIfBrick marker = (ElseIfBrick) brick;
			if (elseIfMarkers.contains(marker)) {
				return removeElseIfBranch(marker.getBranchIndex());
			}
			return true;
		}
		if (brick instanceof ElseIfEndBrick) {
			ElseIfEndBrick marker = (ElseIfEndBrick) brick;
			if (elseIfEndMarkers.contains(marker)) {
				return removeElseIfBranch(marker.getBranchIndex());
			}
			return true;
		}
		if (ifBranchBricks.remove(brick)) {
			return true;
		}
		for (List<Brick> branch : elseIfBranchBricks) {
			if (branch.remove(brick)) {
				return true;
			}
		}
		if (elseBranchBricks.remove(brick)) {
			return true;
		}
		for (Brick childBrick : ifBranchBricks) {
			if (childBrick.removeChild(brick)) {
				return true;
			}
		}
		for (List<Brick> branch : elseIfBranchBricks) {
			for (Brick childBrick : branch) {
				if (childBrick.removeChild(brick)) {
					return true;
				}
			}
		}
		for (Brick childBrick : elseBranchBricks) {
			if (childBrick.removeChild(brick)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_if_begin_if;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		View addBtn = view.findViewById(R.id.brick_if_add_else_if);
		if (addBtn != null) {
			addBtn.setOnClickListener(v -> {
				addElseIfBranch();
				var activity = org.catrobat.catroid.ui.UiUtils.getActivityFromView(view);
				if (activity instanceof org.catrobat.catroid.ui.SpriteActivity) {
					org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment frag =
						(org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment)
							((org.catrobat.catroid.ui.SpriteActivity) activity)
								.getSupportFragmentManager()
								.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment.TAG);
					if (frag != null) {
						frag.getAdapter().updateItems(
							org.catrobat.catroid.ProjectManager.getInstance().getCurrentSprite());
						frag.notifyDataSetChanged();
					}
				}
			});
		}
		return view;
	}

	@Override
	public View getPrototypeView(Context context) {
		View view = super.getPrototypeView(context);
		view.findViewById(R.id.if_else_prototype_punctuation).setVisibility(View.VISIBLE);
		view.findViewById(R.id.if_prototype_else).setVisibility(View.VISIBLE);
		view.findViewById(R.id.if_else_prototype_punctuation2).setVisibility(View.VISIBLE);
		View addBtn = view.findViewById(R.id.brick_if_add_else_if);
		if (addBtn != null) {
			addBtn.setVisibility(View.GONE);
		}
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		ScriptSequenceAction ifSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());

		for (Brick brick : ifBranchBricks) {
			if (!brick.isCommentedOut()) {
				brick.addActionToSequence(sprite, ifSequence);
			}
		}

		ScriptSequenceAction finalElseSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
		for (Brick brick : elseBranchBricks) {
			if (!brick.isCommentedOut()) {
				brick.addActionToSequence(sprite, finalElseSequence);
			}
		}

		ScriptSequenceAction currentElse = finalElseSequence;
		for (int i = elseIfConditions.size() - 1; i >= 0; i--) {
			ScriptSequenceAction branchSeq = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
			for (Brick brick : elseIfBranchBricks.get(i)) {
				if (!brick.isCommentedOut()) {
					brick.addActionToSequence(sprite, branchSeq);
				}
			}
			ScriptSequenceAction wrapperElse = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
			Action elseIfAction = sprite.getActionFactory()
					.createIfLogicAction(sprite, sequence, elseIfConditions.get(i), branchSeq, currentElse);
			wrapperElse.addAction(elseIfAction);
			currentElse = wrapperElse;
		}

		Action action = sprite.getActionFactory()
				.createIfLogicAction(sprite, sequence, getFormulaWithBrickField(BrickField.IF_CONDITION),
						ifSequence, currentElse);

		sequence.addAction(action);
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
		super.addRequiredResources(requiredResourcesSet);
		for (Brick brick : ifBranchBricks) {
			brick.addRequiredResources(requiredResourcesSet);
		}
		for (Formula condition : elseIfConditions) {
			condition.addRequiredResources(requiredResourcesSet);
		}
		for (List<Brick> branch : elseIfBranchBricks) {
			for (Brick brick : branch) {
				brick.addRequiredResources(requiredResourcesSet);
			}
		}
		for (Brick brick : elseBranchBricks) {
			brick.addRequiredResources(requiredResourcesSet);
		}
	}

	@VisibleForTesting
	public static class ElseBrick extends BrickBaseType {

		ElseBrick(IfLogicBeginBrick ifBrick) {
			parent = ifBrick;
		}

		@Override
		public boolean isCommentedOut() {
			return parent.isCommentedOut();
		}

		@Override
		public boolean consistsOfMultipleParts() {
			return true;
		}

		@Override
		public List<Brick> getAllParts() {
			return parent.getAllParts();
		}

		@Override
		public void addToFlatList(List<Brick> bricks) {
			parent.addToFlatList(bricks);
		}

		@Override
		public List<Brick> getDragAndDropTargetList() {
			return ((IfLogicBeginBrick) parent).elseBranchBricks;
		}

		@Override
		public int getPositionInDragAndDropTargetList() {
			return -1;
		}

		@Override
		public int getViewResource() {
			return R.layout.brick_if_else;
		}

		@Override
		public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		}

		@Override
		public UUID getBrickID() {
			return parent.getBrickID();
		}
	}
}
