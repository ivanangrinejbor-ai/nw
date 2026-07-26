package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

import java.util.List;
import java.util.UUID;

/**
 * Маркер-брик "Конец иначе если" — закрывает ветку Else If в плоском списке.
 * Не сериализуется отдельно (transient в IfLogicBeginBrick).
 */
public class ElseIfEndBrick extends BrickBaseType {

	private static final long serialVersionUID = 1L;

	private final transient IfLogicBeginBrick ifBrick;
	private final transient int branchIndex;

	public ElseIfEndBrick(IfLogicBeginBrick ifBrick, int branchIndex) {
		this.ifBrick = ifBrick;
		this.branchIndex = branchIndex;
		parent = ifBrick;
	}

	public int getBranchIndex() {
		return branchIndex;
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
		// Только ЭТА ветка (маркер + её конец)
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
		return R.layout.brick_else_if_end;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		// Маркер — не добавляет action самостоятельно
	}

	@Override
	public UUID getBrickID() {
		return ifBrick.getBrickID();
	}
}
