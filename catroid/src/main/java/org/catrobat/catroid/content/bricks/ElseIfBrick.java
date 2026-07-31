package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.List;
import java.util.UUID;

public class ElseIfBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private final transient IfLogicBeginBrick ifBrick;
	private final transient int branchIndex;

	public ElseIfBrick(IfLogicBeginBrick ifBrick, int branchIndex) {
		this.ifBrick = ifBrick;
		this.branchIndex = branchIndex;
		parent = ifBrick;
		addAllowedBrickField(BrickField.IF_CONDITION, R.id.brick_else_if_edit_text);
		Formula condition = ifBrick.getElseIfCondition(branchIndex);
		if (condition != null) {
			setFormulaWithBrickField(BrickField.IF_CONDITION, condition);
		}
	}

	public int getBranchIndex() {
		return branchIndex;
	}

	public Formula getCondition() {
		return ifBrick.getElseIfCondition(branchIndex);
	}

	@Override
	public void setFormulaWithBrickField(FormulaField formulaField, Formula formula) {
		super.setFormulaWithBrickField(formulaField, formula);
		if (ifBrick != null && formulaField == BrickField.IF_CONDITION) {
			ifBrick.setElseIfCondition(branchIndex, formula);
		}
	}

	@Override
	public boolean isCommentedOut() {
		return ifBrick.isCommentedOut();
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return true;
	}

	@Override
	public List<Brick> getAllParts() {
		return ifBrick.getElseIfBranchParts(branchIndex);
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		ifBrick.addToFlatList(bricks);
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return ifBrick.getElseIfBranch(branchIndex);
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return -1;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_else_if;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		setClickListeners();
		return view;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}

	@Override
	public UUID getBrickID() {
		return ifBrick.getBrickID();
	}
}
